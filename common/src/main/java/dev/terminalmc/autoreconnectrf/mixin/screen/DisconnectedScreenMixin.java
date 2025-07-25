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
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.mixin.accessor.DisconnectedScreenAccessor;
import dev.terminalmc.autoreconnectrf.util.DisconnectScreenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

/**
 * {@link DisconnectedScreen} is used on disconnection from worlds and servers, and since 1.21.6,
 * realms.
 */
@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {

    @Shadow
    @Mutable
    private @Final Screen parent;

    @Unique
    private boolean autoreconnectrf$canAutoReconnect;

    @Unique
    private @Nullable Runnable autoreconnectrf$manualCancel;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    /**
     * Adds the AutoReconnect widgets to the screen.
     */
    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void afterInit(CallbackInfo ci) {
        // Find the 'back' button
        @Nullable Button backButton = DisconnectScreenUtil.findBackButton(this);
        if (backButton == null) {
            AutoReconnect.LOG.warn("Couldn't find the back button on the disconnect screen");
            return;
        }

        // Fix MC-45602
        autoreconnectrf$fixBackButton(backButton);

        // Check for a reconnect strategy
        autoreconnectrf$canAutoReconnect = AutoReconnect.canReconnect();
        if (!autoreconnectrf$canAutoReconnect)
            return;

        // Check whether the conditions allow a reconnect
        autoreconnectrf$canAutoReconnect = autoreconnectrf$canAutoReconnect();

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
     * Allows pressing ESC (again) to close this screen.
     */
    @Inject(
            method = "shouldCloseOnEsc",
            at = @At("RETURN"),
            cancellable = true
    )
    private void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /**
     * Redirects to the parent screen instead of the title screen on close.
     */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Fixes <a href="https://bugs.mojang.com/browse/MC-45602">MC-45602</a>.
     */
    @Unique
    private void autoreconnectrf$fixBackButton(Button backButton) {
        if (AutoReconnect.isPlayingSingleplayer()) {
            // Make the 'back' button redirect to SelectWorldScreen instead of JoinMultiplayerScreen
            parent = new SelectWorldScreen(new TitleScreen());
            // Change back button text to "Back to World List" instead of "Back to Server List"
            backButton.setMessage(Component.translatable("gui.toWorld"));
        }
    }

    /**
     * Checks the conditions to determine whether to attempt to reconnect.
     */
    @Unique
    private boolean autoreconnectrf$canAutoReconnect() {
        Component reason =
                ((DisconnectedScreenAccessor) this).autoreconnectrf$getDetails().reason();
        String reasonStr = reason.getString();
        AutoReconnect.lastDcReasonStr = reasonStr;
        AutoReconnect.lastDcReasonKey = null;
        boolean match = false;

        if (reason.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            AutoReconnect.lastDcReasonKey = key;

            // Check for transfer packet
            if (key.equals("disconnect.transfer"))
                return false;

            // Check key conditions
            for (String condition : Config.options().conditionKeys) {
                if (key.contains(condition)) {
                    AutoReconnect.LOG.info(
                            "Matched key '{}' against reason key '{}'",
                            condition,
                            key
                    );
                    match = true;
                    break;
                }
            }
        }

        if (!match) {
            // Check regex conditions
            for (Pattern condition : AutoReconnect.conditionPatterns) {
                if (condition.matcher(reasonStr).find()) {
                    AutoReconnect.LOG.info(
                            "Matched pattern '{}' against reason '{}'",
                            condition,
                            reasonStr
                    );
                    match = true;
                    break;
                }
            }
        }

        if (Config.options().conditionType) {
            return match && Config.get().hasAttempts();
        } else {
            return !match && Config.get().hasAttempts();
        }
    }

    /**
     * Creates and adds the AutoReconnect control buttons to the screen.
     */
    @Unique
    private Button autoreconnectrf$addButtons(Button backButton) {
        // Retrieve the existing layout
        LinearLayout layout = ((DisconnectedScreenAccessor) this).autoreconnectrf$getLayout();

        // Add the 'reconnect' button
        Button reconnectButton = Button.builder(
                localized("message", "reconnect"),
                btn -> AutoReconnect.schedule(
                        () -> Minecraft.getInstance().execute(AutoReconnect::manualReconnect),
                        100,
                        TimeUnit.MILLISECONDS
                )
        ).bounds(0, 0, backButton.getWidth(), backButton.getHeight()).build();
        layout.addChild(reconnectButton);

        if (autoreconnectrf$canAutoReconnect) {
            // Add the 'cancel' button
            @SuppressWarnings("UnnecessaryUnicodeEscape")
            Button cancelButton = Button.builder(
                    Component.literal("\u274C").withStyle(ChatFormatting.RED),
                    btn -> {
                        if (autoreconnectrf$manualCancel != null)
                            autoreconnectrf$manualCancel.run();
                    }
            ).bounds(0, 0, backButton.getWidth(), backButton.getHeight()).build();
            layout.addChild(cancelButton);

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

        // Reload the GUI
        layout.arrangeElements();
        repositionElements();
        clearWidgets();
        layout.visitWidgets(this::addRenderableWidget);

        return reconnectButton;
    }
}
