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

package dev.terminalmc.autoreconnectrf.mixin.screen;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.util.DisconnectScreenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.DisconnectedRealmsScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

/**
 * {@link DisconnectedRealmsScreen} is used on disconnection from realms. For worlds and servers,
 * refer to {@link DisconnectedScreenMixin}.
 */
@Mixin(DisconnectedRealmsScreen.class)
public class DisconnectedRealmsScreenMixin extends Screen {

    @Unique
    private boolean autoreconnectrf$canAutoReconnect;

    @Unique
    private @Nullable Runnable autoreconnectrf$manualCancel;

    protected DisconnectedRealmsScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void init(CallbackInfo ci) {
        // Find the 'back' button
        @Nullable Button backButton = DisconnectScreenUtil.findBackButton(this);
        if (backButton == null) {
            AutoReconnect.LOG.warn("Couldn't find the back button on the disconnect screen");
            return;
        }

        // Check for a reconnect strategy

        autoreconnectrf$canAutoReconnect = AutoReconnect.canReconnect();
        if (!autoreconnectrf$canAutoReconnect)
            return;

        // Add the extra GUI buttons
        Button reconnectButton = autoreconnectrf$addButtons(backButton);

        if (autoreconnectrf$canAutoReconnect) {
            DisconnectScreenUtil.startCountdown(reconnectButton);
        }
    }

    /**
     * Allows pressing ESC to cancel automatic reconnection.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && autoreconnectrf$canAutoReconnect) {
            if (autoreconnectrf$manualCancel != null) {
                autoreconnectrf$manualCancel.run();
            }
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    /**
     * Creates and adds the AutoReconnect control buttons to the screen.
     */
    @Unique
    private Button autoreconnectrf$addButtons(Button backButton) {
        // Add the 'reconnect' button
        Button reconnectButton = Button.builder(
                localized("message", "reconnect"),
                btn -> AutoReconnect.schedule(
                        () -> Minecraft.getInstance().execute(AutoReconnect::manualReconnect),
                        100,
                        TimeUnit.MILLISECONDS
                )
        ).bounds(
                backButton.getX(),
                backButton.getY() + backButton.getHeight() + 4,
                backButton.getWidth(),
                backButton.getHeight()
        ).build();
        addRenderableWidget(reconnectButton);

        if (autoreconnectrf$canAutoReconnect) {
            // Add the 'cancel' button
            @SuppressWarnings("UnnecessaryUnicodeEscape")
            Button cancelButton = Button.builder(
                    Component.literal("\u274C").withStyle(ChatFormatting.RED), btn -> {
                        AutoReconnect.cancelActiveReconnect();
                        autoreconnectrf$canAutoReconnect = false;
                        removeWidget(this);
                        reconnectButton.active =
                                true; // in case it was deactivated after running out of attempts
                        reconnectButton.setMessage(localized("message", "reconnect"));
                        reconnectButton.setWidth(backButton.getWidth()); // reset to full width
                    }
            ).bounds(
                    reconnectButton.getX(),
                    reconnectButton.getY() + reconnectButton.getHeight() + 4,
                    backButton.getWidth(),
                    backButton.getHeight()
            ).build();
            addRenderableWidget(cancelButton);

            // Create the cancellation runnable
            autoreconnectrf$manualCancel = () -> {
                // Cancel the current reconnect attempt and countdown
                AutoReconnect.cancelActiveReconnect();
                autoreconnectrf$canAutoReconnect = false;
                // Remove the 'cancel' button
                removeWidget(cancelButton);
                // Reset the 'reconnect' button
                reconnectButton.active = true;
                reconnectButton.setMessage(localized("message", "reconnect"));
            };
        }

        return reconnectButton;
    }
}
