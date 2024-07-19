/*
 * AutoReconnect-Reforged
 *
 * Copyright 2020-2023 Bstn1802
 * Copyright 2024 NotRyken
 *
 * The following code is a derivative work of the code from the AutoReconnect
 * project, which is licensed LGPLv3. This code therefore is also licensed under
 * the terms of the GNU Lesser Public License, version 3.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.terminalmc.autoreconnectrf;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.reconnect.ReconnectStrategy;
import dev.terminalmc.autoreconnectrf.reconnect.SingleplayerReconnectStrategy;
import dev.terminalmc.autoreconnectrf.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AutoReconnect {
    public static final String MOD_ID = "autoreconnectrf";
    public static final String MOD_NAME = "AutoReconnect-Reforged";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final List<String> DISCONNECT_KEYS = List.of(
            "disconnect.closed",
            "disconnect.disconnected",
            "disconnect.endOfStream",
            "disconnect.exceeded_packet_rate",
            "disconnect.genericReason", // arg
            "disconnect.ignoring_status_request",
            "disconnect.loginFailed",
            "disconnect.loginFailedInfo", // arg
            "disconnect.loginFailedInfo.insufficientPrivileges",
            "disconnect.loginFailedInfo.invalidSession",
            "disconnect.loginFailedInfo.serversUnavailable",
            "disconnect.loginFailedInfo.userBanned",
            "disconnect.lost",
            "disconnect.overflow",
            "disconnect.packetError",
            "disconnect.spam",
            "disconnect.timeout",
            "disconnect.transfer",
            "disconnect.unknownHost",

            "multiplayer.disconnect.authservers_down",
            "multiplayer.disconnect.banned",
            "multiplayer.disconnect.banned_ip.reason", // arg
            "multiplayer.disconnect.banned.reason", // arg
            "multiplayer.disconnect.chat_validation_failed",
            "multiplayer.disconnect.duplicate_login",
            "multiplayer.disconnect.expired_public_key",
            "multiplayer.disconnect.flying",
            "multiplayer.disconnect.generic",
            "multiplayer.disconnect.idling",
            "multiplayer.disconnect.illegal_characters",
            "multiplayer.disconnect.incompatible", // arg
            "multiplayer.disconnect.invalid_entity_attacked",
            "multiplayer.disconnect.invalid_packet",
            "multiplayer.disconnect.invalid_player_data",
            "multiplayer.disconnect.invalid_player_movement",
            "multiplayer.disconnect.invalid_public_key_signature",
            "multiplayer.disconnect.invalid_public_key_signature",
            "multiplayer.disconnect.invalid_vehicle_movement",
            "multiplayer.disconnect.ip_banned",
            "multiplayer.disconnect.kicked",
            "multiplayer.disconnect.missing_tags",
            "multiplayer.disconnect.name_taken",
            "multiplayer.disconnect.not_whitelisted",
            "multiplayer.disconnect.out_of_order_chat",
            "multiplayer.disconnect.outdated_client", // arg
            "multiplayer.disconnect.outdated_server", // arg
            "multiplayer.disconnect.server_full",
            "multiplayer.disconnect.server_shutdown",
            "multiplayer.disconnect.slow_login",
            "multiplayer.disconnect.too_many_pending_chats",
            "multiplayer.disconnect.transfers_disabled",
            "multiplayer.disconnect.unexpected_query_response",
            "multiplayer.disconnect.unsigned_chat",
            "multiplayer.disconnect.unverified_username",

            "multiplayer.requiredTexturePrompt.disconnect"
    );

    public static final List<Pattern> conditionPatterns = new ArrayList<>();

    public static @Nullable String lastDcReasonStr = null;
    public static @Nullable String lastDcReasonKey = null;

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

    // Legacy

    private static final ScheduledThreadPoolExecutor EXECUTOR_SERVICE = new ScheduledThreadPoolExecutor(1);
    private static final AtomicReference<ScheduledFuture<?>> countdown = new AtomicReference<>(null);
    private static ReconnectStrategy reconnectStrategy = null;

    static {
        EXECUTOR_SERVICE.setRemoveOnCancelPolicy(true);
    }

    public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit timeUnit) {
        return EXECUTOR_SERVICE.schedule(command, delay, timeUnit);
    }

    public static void setReconnectHandler(ReconnectStrategy pReconnectStrategy) {
        if (reconnectStrategy != null) {
            // should imply that both handlers target the same world/server
            // we return to preserve the attempts counter
            assert reconnectStrategy.getClass().equals(pReconnectStrategy.getClass()) &&
                    reconnectStrategy.getName().equals(pReconnectStrategy.getName());
            return;
        }
        reconnectStrategy = pReconnectStrategy;
    }

    public static void reconnect() {
        if (reconnectStrategy == null) return; // shouldn't happen normally, but can be forced
        cancelCountdown();
        reconnectStrategy.reconnect();
    }

    public static void startCountdown(final IntConsumer callback) {
        // if (countdown.get() != null) return; // should not happen
        if (reconnectStrategy == null) {
            // TODO fix issue appropriately, logging error for now
            LogUtils.getLogger().error("Cannot reconnect because reconnectStrategy is null");
            callback.accept(-1); // signal reconnecting is not possible
            return;
        }

        int delay = Config.get().getDelayForAttempt(reconnectStrategy.nextAttempt());
        if (delay >= 0) {
            countdown(delay, callback);
        } else {
            // no more attempts configured
            callback.accept(-1);
        }
    }

    public static void cancelAutoReconnect() {
        if (reconnectStrategy == null) return; // should not happen
        reconnectStrategy.resetAttempts();
        cancelCountdown();
    }

    public static void onScreenChanged(Screen current, Screen next) {
        if (sameType(current, next)) return;
        // TODO condition could use some improvement, shouldn't cause any issues tho
        if (!isMainScreen(current) && isMainScreen(next) || isReAuthenticating(current, next)) {
            cancelAutoReconnect();
            reconnectStrategy = null;
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
            // The first starting slash has to be removed,
            // otherwise it will be interpreted as a double slash.
            String command = message.substring(1);
            player.connection.sendUnsignedCommand(command);
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

    private static boolean isReAuthenticating(Screen from, Screen to) {
        return from instanceof DisconnectedScreen && to != null &&
                to.getClass().getName().startsWith("me.axieum.mcmod.authme");
    }

    public static Optional<Button> findBackButton(Screen screen) {
        for (GuiEventListener element : screen.children()) {
            if (!(element instanceof Button button)) continue;

            String translatableKey;
            if (button.getMessage() instanceof TranslatableContents translatable) {
                translatableKey = translatable.getKey();
            } else if (button.getMessage().getContents() instanceof TranslatableContents translatable) {
                translatableKey = translatable.getKey();
            } else continue;

            // check for gui.back, gui.toMenu, gui.toRealms, gui.toTitle, gui.toWorld (only ones starting with "gui.to")
            if (translatableKey.equals("gui.back") || translatableKey.startsWith("gui.to")) {
                return Optional.of(button);
            }
        }
        return Optional.empty();
    }
}
