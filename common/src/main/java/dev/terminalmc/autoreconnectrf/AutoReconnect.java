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
import dev.terminalmc.autoreconnectrf.reconnect.SingleplayerReconnectStrategy;
import dev.terminalmc.autoreconnectrf.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AutoReconnect {
    public static final String MOD_ID = "autoreconnectrf";
    public static final String MOD_NAME = "AutoReconnect";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);

    // Condition vars
    public static final List<Pattern> conditionPatterns = new ArrayList<>();
    public static @Nullable String lastDcReasonStr = null;
    public static @Nullable String lastDcReasonKey = null;

    // Reconnect vars
    private static final ScheduledThreadPoolExecutor EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1);
    static { EXECUTOR_SERVICE.setRemoveOnCancelPolicy(true); }
    private static final AtomicReference<ScheduledFuture<?>> countdown = new AtomicReference<>(null);
    private static @Nullable ReconnectStrategy reconnectStrategy = null;

    // Mod lifecycle methods

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
    }

    public static void onConfigSaved(Config config) {
        conditionPatterns.clear();
        for (String s : config.options.conditionPatterns) {
            try {
                conditionPatterns.add(Pattern.compile(s));
            } catch (PatternSyntaxException ignored) {}
        }
    }

    // Reconnect methods

    /**
     * Stops any active reconnection, and removes the saved strategy to prevent
     * future reconnection.
     *
     * <p>Any mods wanting to prevent automatic reconnection should invoke this
     * method at any time after the player has joined a world/server/realm.</p>
     */
    public static void cancelAutoReconnect() {
        cancelActiveReconnect();
        reconnectStrategy = null;
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit timeUnit) {
        return EXECUTOR_SERVICE.schedule(command, delay, timeUnit);
    }

    /**
     * Sets the strategy to be used for the next reconnection attempt.
     */
    public static void setReconnectStrategy(@NotNull ReconnectStrategy pReconnectStrategy) {
        // Avoid overwriting strategy on reconnect failure
        if (reconnectStrategy == null) {
            reconnectStrategy = pReconnectStrategy;
        }
    }

    /**
     * @return {@code true} if the mod has a reconnection strategy,
     * {@code false} otherwise.
     */
    public static boolean canReconnect() {
        return reconnectStrategy != null;
    }

    /**
     * Attempts to reconnect using the stored strategy.
     */
    public static void reconnect() {
        cancelCountdown();
        checkStrategy().reconnect();
    }

    /**
     * Initiates the countdown for the next reconnect attempt, if any.
     */
    public static void startCountdown(final IntConsumer callback) {
        int delay = Config.get().getDelayForAttempt(checkStrategy().nextAttempt());
        if (delay >= 0) {
            countdown(delay, callback);
        } else {
            // No more attempts configured
            callback.accept(-1);
        }
    }

    /**
     * Stops attempting reconnection but retains strategy for manual reconnect.
     */
    public static void cancelActiveReconnect() {
        if (reconnectStrategy != null) reconnectStrategy.resetAttempts();
        cancelCountdown();
    }

    /**
     * Resets the reconnect countdown and attempts to reconnect using the saved
     * strategy.
     */
    public static void manualReconnect() {
        AutoReconnect.cancelActiveReconnect();
        AutoReconnect.reconnect();
    }

    public static void onScreenChanged(Screen current, Screen next) {
        if (sameType(current, next)) return;
        // TODO condition could use some improvement, shouldn't cause any issues
        if (!isMainScreen(current) && isMainScreen(next) || isReAuthenticating(current, next)) {
            cancelAutoReconnect();
        }
    }

    public static void onGameJoined() {
        if (reconnectStrategy == null) return; // should not happen
        if (!reconnectStrategy.isAttempting()) return; // manual (re)connect

        reconnectStrategy.resetAttempts();

        // Send automatic messages if configured for the current context
        Config.get().getAutoMessagesForName(reconnectStrategy.getName()).ifPresent(
                autoMessages -> sendAutomatedMessages(
                        Minecraft.getInstance().player,
                        autoMessages.getMessages(),
                        autoMessages.getDelay()
                )
        );
    }

    public static boolean isPlayingSingleplayer() {
        return reconnectStrategy instanceof SingleplayerReconnectStrategy;
    }

    private static void cancelCountdown() {
        synchronized (countdown) { // just to be sure
            if (countdown.get() == null) return;
            countdown.getAndSet(null).cancel(true); // should stop the timer
        }
    }

    // simulated timer using delayed recursion
    private static void countdown(int seconds, final IntConsumer callback) {
        if (reconnectStrategy == null) return; // should not happen
        if (seconds == 0) {
            Minecraft.getInstance().execute(AutoReconnect::reconnect);
            return;
        }
        callback.accept(seconds);
        // wait at end of method for no initial delay
        synchronized (countdown) { // just to be sure
            countdown.set(schedule(() -> countdown(seconds - 1, callback), 1, TimeUnit.SECONDS));
        }
    }

    /**
     * Handle a list of messages to send by the player to the current connection.
     *
     * @param player   Player to send the message as.
     * @param messages String Iterator of messages to send.
     * @param delay    Delay in milliseconds before the first and between each following message.
     */
    private static void sendAutomatedMessages(LocalPlayer player, Iterator<String> messages, int delay) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            if (!messages.hasNext()) {
                executorService.shutdown();
                return;
            }

            sendMessage(player, messages.next());
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles sending of a single message or command by the player.
     *
     * @param player  Player to send the message as.
     * @param message String with the message or command to send.
     */
    private static void sendMessage(LocalPlayer player, String message) {
        if (message.startsWith("/")) {
            player.connection.sendUnsignedCommand(message.substring(1));
        } else {
            player.connection.sendChat(message);
        }
    }

    private static boolean sameType(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a != null && b != null) return a.getClass().equals(b.getClass());
        return false;
    }

    private static boolean isMainScreen(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof SelectWorldScreen ||
                screen instanceof JoinMultiplayerScreen || screen instanceof RealmsMainScreen;
    }

    private static boolean isReAuthenticating(Screen current, Screen next) {
        return current instanceof DisconnectedScreen
                && next != null
                && next.getClass().getName().startsWith("me.axieum.mcmod.authme");
    }

    private static @NotNull ReconnectStrategy checkStrategy() {
        if (reconnectStrategy == null) {
            throw new IllegalStateException("Reconnect strategy failed null check");
        } else {
            return reconnectStrategy;
        }
    }
}
