/*
 * AutoReconnect
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

package dev.terminalmc.autoreconnectrf.gui.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

/**
 * Wraps the config screen implementation and provides a backup screen for
 * use when the config lib mod is not loaded. This allows the dependency to be
 * defined as optional.
 */
public class ConfigScreenProvider {

    public static Screen getConfigScreen(Screen parent) {
        try {
            return YaclScreenProvider.getConfigScreen(parent);
        } catch (NoClassDefFoundError ignored) {
            return new BackupScreen(parent, "installYacl", "https://modrinth.com/mod/1eAoo2KR");
        }
    }

    static class BackupScreen extends Screen {
        private final Screen parent;
        private final String modKey;
        private final String modUrl;

        public BackupScreen(Screen parent, String modKey, String modUrl) {
            super(localized("name"));
            this.parent = parent;
            this.modKey = modKey;
            this.modUrl = modUrl;
        }

        @Override
        public void init() {
            MultiLineTextWidget messageWidget = new MultiLineTextWidget(
                    width / 2 - 120, height / 2 - 40,
                    localized("message", modKey),
                    minecraft.font);
            messageWidget.setMaxWidth(240);
            messageWidget.setCentered(true);
            addRenderableWidget(messageWidget);

            Button openLinkButton = Button.builder(localized("message", "viewModrinth"),
                            (button) -> minecraft.setScreen(new ConfirmLinkScreen(
                                    (open) -> {
                                        if (open) Util.getPlatform().openUri(modUrl);
                                        minecraft.setScreen(parent);
                                    }, modUrl, true)))
                    .pos(width / 2 - 120, height / 2)
                    .size(115, 20)
                    .build();
            addRenderableWidget(openLinkButton);

            Button exitButton = Button.builder(CommonComponents.GUI_OK,
                            (button) -> onClose())
                    .pos(width / 2 + 5, height / 2)
                    .size(115, 20)
                    .build();
            addRenderableWidget(exitButton);
        }
    }
}
