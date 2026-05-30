/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
 * Copyright (C) 2026 TerminalMC
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

package dev.terminalmc.autoreconnectrf.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static dev.terminalmc.autoreconnectrf.config.Config.options;

public class MessageUtil {

    /**
     * Handle a list of messages or commands to send by the player to the current connection.
     *
     * @param messages String iterator of messages or commands to send.
     * @param delay    Delay in milliseconds before the first and between each following message.
     */
    public static void sendAll(
            Iterator<String> messages,
            int delay
    ) {
        @SuppressWarnings("resource")
        ScheduledExecutorService execService = Executors.newSingleThreadScheduledExecutor();
        execService.scheduleWithFixedDelay(
                () -> {
                    if (!messages.hasNext()) {
                        execService.shutdown();
                        return;
                    }
                    sendMessage(messages.next());
                }, delay, delay, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Handles sending of a single message or command by the player.
     *
     * @param message String with the message or command to send.
     */
    private static void sendMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;
        if (message.startsWith("/")) {
            if (options().commandSigning) {
                player.connection.sendCommand(message.substring(1));
            } else {
                player.connection.sendUnattendedCommand(
                        message.substring(1),
                        Minecraft.getInstance().gui.screen()
                );
            }
        } else {
            player.connection.sendChat(message);
        }
    }
}
