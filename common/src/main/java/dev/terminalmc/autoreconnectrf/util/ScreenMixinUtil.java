/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
 * Copyright (C) 2024 TerminalMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.mixin.accessor.DisconnectedScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;
import java.util.regex.Pattern;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

/**
 * Yeah, I know it's bad practice.
 */
public class ScreenMixinUtil {
    public static boolean checkConditions(Screen screen) {
        if (screen instanceof DisconnectedScreen ds) {
            // Doesn't work with realms disconnect screen
            Component reason = ((DisconnectedScreenAccessor)ds).getDetails().reason();
            String reasonStr = reason.getString();
            AutoReconnect.lastDcReasonStr = reasonStr;
            AutoReconnect.lastDcReasonKey = null;
            boolean match = false;

            // Check regex conditions
            for (Pattern condition : AutoReconnect.conditionPatterns) {
                if (condition.matcher(reasonStr).find()) {
                    AutoReconnect.LOG.info("Matched pattern '{}' against reason '{}'",
                            condition, reasonStr);
                    match = true;
                    break;
                }
            }
            // Check key conditions
            if (!match && reason.getContents() instanceof TranslatableContents tc) {
                String key = tc.getKey();
                AutoReconnect.lastDcReasonKey = key;
                for (String condition : Config.get().options.conditionKeys) {
                    if (key.contains(condition)) {
                        AutoReconnect.LOG.info("Matched key '{}' against reason key '{}'",
                                condition, key);
                        match = true;
                        break;
                    }
                }
            }
            if (Config.get().options.conditionType) {
                return match;
            } else {
                return !match;
            }
        }
        return Config.get().hasAttempts();
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

    @Unique
    public static void countdownCallback(Button reconnectButton, int seconds) {
        if (seconds < 0) {
            // indicates that we're out of attempts
            reconnectButton.setMessage(localized("message", "reconnectFailed")
                    .withStyle(s -> s.withColor(ChatFormatting.RED)));
            reconnectButton.active = false;
        } else {
            reconnectButton.setMessage(localized("message", "reconnectIn", seconds)
                    .withStyle(s -> s.withColor(ChatFormatting.GREEN)));
        }
    }
}
