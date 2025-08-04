/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
 * Copyright (C) 2025 TerminalMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.terminalmc.autoreconnectrf;

import com.mojang.realmsclient.RealmsMainScreen;
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.reconnect.ReconnectStrategy;
import dev.terminalmc.autoreconnectrf.reconnect.WorldReconnectStrategy;
import dev.terminalmc.autoreconnectrf.util.MessageUtil;
import dev.terminalmc.autoreconnectrf.util.ModLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AutoReconnect {

    public static final String MOD_ID = "autoreconnectrf";
    public static final String MOD_NAME = "AutoReconnect";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(MOD_NAME).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);

    public static final List<Pattern> conditionPatterns = new ArrayList<>();
    public static @Nullable String lastDcReasonStr = null;
    public static @Nullable String lastDcReasonKey = null;

    private static final ScheduledThreadPoolExecutor EXECUTOR_SERVICE =
            new ScheduledThreadPoolExecutor(1);

    static {
        EXECUTOR_SERVICE.setRemoveOnCancelPolicy(true);
    }

    private static final AtomicReference<ScheduledFuture<?>> countdown =
            new AtomicReference<>(null);

    private static @Nullable ReconnectStrategy reconnectStrategy = null;

    public static boolean debug() {
        return true;
    }

    /**
     * Client initialization.
     */
    public static void init() {
        Config.getAndSave();
    }

    /**
     * Client after-tick event listener.
     */
    public static void afterClientTick(Minecraft mc) {
    }

    /**
     * Config save listener.
     */
    public static void onConfigSaved(Config config) {
        conditionPatterns.clear();
        for (String s : config.options.conditionPatterns) {
            try {
                conditionPatterns.add(Pattern.compile(s));
            } catch (PatternSyntaxException ignored) {
            }
        }
    }

    /**
     * Screen change listener.
     */
    public static void onScreenChanged(@Nullable Screen current, @Nullable Screen next) {
        if (isSameType(current, next))
            return;
        if (!isMainScreen(current) && isMainScreen(next) || isReAuthenticating(current, next)) {
            cancelAutoReconnect();
        }
    }

    /**
     * Game join listener.
     */
    public static void onGameJoined() {
        if (reconnectStrategy == null)
            return; // Should not happen
        if (!reconnectStrategy.isAttempting())
            return; // Manual (re)connect
        if (debug())
            AutoReconnect.LOG.info("onGameJoined for ID {}", reconnectStrategy.getId());

        reconnectStrategy.resetAttempts();

        // Send automatic messages if configured for the current context
        Config.get()
                .getAutoMessagesForId(reconnectStrategy.getId())
                .forEach(autoMessage -> MessageUtil.sendAll(
                        autoMessage.getMessages(),
                        (int) (autoMessage.delay * 1000)
                ));
    }

    // Reconnect management

    /**
     * Sets the strategy to be used for the next reconnection attempt.
     */
    public static void setReconnectStrategy(@NotNull ReconnectStrategy pReconnectStrategy) {
        // Avoid overwriting strategy on reconnect failure
        if (reconnectStrategy == null) {
            if (debug())
                AutoReconnect.LOG.info(
                        "Setting reconnect strategy for ID {}",
                        pReconnectStrategy.getId()
                );
            reconnectStrategy = pReconnectStrategy;
        } else {
            if (debug())
                AutoReconnect.LOG.info(
                        "Not overriding existing reconnect strategy ",
                        reconnectStrategy.getId()
                );
        }
    }

    /**
     * @return {@code true} if the mod has a reconnection strategy.
     */
    public static boolean canReconnect() {
        return reconnectStrategy != null;
    }

    /**
     * Attempts to reconnect using the stored strategy.
     */
    public static void reconnect() {
        cancelCountdown();
        if (reconnectStrategy != null) {
            if (debug())
                AutoReconnect.LOG.info(
                        "Reconnecting with strategy for ID {}",
                        reconnectStrategy.getId()
                );
            reconnectStrategy.reconnect();
        } else {
            if (debug())
                AutoReconnect.LOG.info("Cannot reconnect: strategy is null");
        }
    }

    /**
     * Resets the reconnect countdown and attempts to reconnect using the saved strategy.
     */
    public static void manualReconnect() {
        AutoReconnect.cancelActiveReconnect();
        AutoReconnect.reconnect();
    }

    /**
     * Stops any active reconnection and removes the saved strategy to prevent future reconnection.
     * <p>
     * Any other mods wanting to prevent automatic reconnection should invoke this method at any
     * time after the player has joined a world/server/realm.
     */
    public static void cancelAutoReconnect() {
        cancelActiveReconnect();
        reconnectStrategy = null;
    }

    /**
     * Stops attempting reconnection but retains the strategy for manual reconnection.
     */
    public static void cancelActiveReconnect() {
        if (reconnectStrategy != null)
            reconnectStrategy.resetAttempts();
        cancelCountdown();
    }

    // Countdown management

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit timeUnit) {
        return EXECUTOR_SERVICE.schedule(command, delay, timeUnit);
    }

    /**
     * Initiates the countdown for the next reconnect attempt, if any.
     */
    public static void startCountdown(final IntConsumer callback) {
        int delay = Config.get().getDelayForAttempt(reconnectStrategy.nextAttempt());
        if (delay >= 0) {
            countdown(delay, callback);
        } else {
            // No more attempts configured
            callback.accept(-1);
        }
    }

    /**
     * Stops and clears the active countdown, if any.
     */
    private static void cancelCountdown() {
        synchronized (countdown) { // Just to be sure
            if (countdown.get() == null)
                return;
            countdown.getAndSet(null).cancel(true);
        }
    }

    /**
     * Simulated reconnect countdown timer using delayed recursion.
     */
    private static void countdown(int seconds, final IntConsumer callback) {
        if (reconnectStrategy == null)
            return; // Should not happen
        if (seconds == 0) {
            // Execute on main thread
            Minecraft.getInstance().execute(AutoReconnect::reconnect);
        } else {
            callback.accept(seconds);
            synchronized (countdown) { // Just to be sure
                countdown.set(schedule(
                        () -> countdown(seconds - 1, callback),
                        1,
                        TimeUnit.SECONDS
                ));
            }
        }
    }

    // Utility methods

    /**
     * @return {@code true} if the current reconnect strategy is for singleplayer.
     */
    public static boolean isPlayingSingleplayer() {
        return reconnectStrategy instanceof WorldReconnectStrategy;
    }

    /**
     * @return {@code true} if {@code a} is the same class as {@code b}.
     */
    private static boolean isSameType(Object a, Object b) {
        if (a == null && b == null)
            return true;
        if (a != null && b != null)
            return a.getClass().equals(b.getClass());
        return false;
    }

    /**
     * @return {@code true} if the screen is a title or play-select screen.
     */
    private static boolean isMainScreen(Screen screen) {
        return screen instanceof TitleScreen
                || screen instanceof SelectWorldScreen
                || screen instanceof JoinMultiplayerScreen
                || screen instanceof RealmsMainScreen;
    }

    /**
     * @return {@code true} if switching from a disconnect screen to an AuthMe screen.
     */
    private static boolean isReAuthenticating(Screen current, Screen next) {
        return current instanceof DisconnectedScreen
                && next != null
                && next.getClass().getName().startsWith("me.axieum.mcmod.authme");
    }
}
