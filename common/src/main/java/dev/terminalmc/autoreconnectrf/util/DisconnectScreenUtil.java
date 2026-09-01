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

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

/**
 * Contains utility methods used by disconnect screen mixin classes.
 */
public class DisconnectScreenUtil {

    /**
     * Attempts to locate the 'back' button on the specified screen.
     */
    public static @Nullable Button findBackButton(Screen screen) {
        for (GuiEventListener widget : screen.children()) {
            if (!(widget instanceof Button button))
                continue;

            String key;
            if (button.getMessage() instanceof TranslatableContents tc)
                key = tc.getKey();
            else if (button.getMessage().getContents() instanceof TranslatableContents tc)
                key = tc.getKey();
            else
                continue;

            if (key.equals("gui.back") || key.startsWith("gui.to"))
                return button;
        }
        return null;
    }

    /**
     * Starts the reconnect countdown with a listener to update the button text.
     */
    public static void startCountdown(Button reconnectButton) {
        AutoReconnect.startCountdownPrecise((seconds) -> {
            if (seconds < 0) {
                // Out of attempts; deactivate button
                reconnectButton.setMessage(localized("message", "reconnectFailed")
                        .withStyle(s -> s.withColor(ChatFormatting.RED)));
                reconnectButton.active = false;
            } else {
                // Attempts ongoing; update time on button
                reconnectButton.setMessage(localized("message", "reconnectIn", Float.toString((float) seconds))
                        .withStyle(s -> s.withColor(ChatFormatting.GREEN)));
            }
        });
    }
}
