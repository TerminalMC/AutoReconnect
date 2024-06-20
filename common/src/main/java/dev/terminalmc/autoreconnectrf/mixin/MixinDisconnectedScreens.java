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

package dev.terminalmc.autoreconnectrf.mixin;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.DisconnectedRealmsScreen;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

@Mixin({ DisconnectedScreen.class, DisconnectedRealmsScreen.class })
public class MixinDisconnectedScreens extends Screen {
    @Unique
    private Button reconnectButton, cancelButton, backButton;
    @Unique
    private boolean shouldAutoReconnect;

    protected MixinDisconnectedScreens(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        backButton = AutoReconnect.findBackButton(this)
            .orElseThrow(() -> new NoSuchElementException("Couldn't find the back button on the disconnect screen"));

        shouldAutoReconnect = Config.get().hasAttempts();

        reconnectButton = Button.builder(
                localized("message", "reconnect"),
                btn -> AutoReconnect.schedule(() -> Minecraft.getInstance().execute(this::manualReconnect), 100, TimeUnit.MILLISECONDS))
            .bounds(0, 0, 0, 20).build();

        // put reconnect (and cancel button) where back button is and push that down
        reconnectButton.setX(backButton.getX());
        reconnectButton.setY(backButton.getY());
        if (shouldAutoReconnect) {
            reconnectButton.setWidth(backButton.getWidth() - backButton.getHeight() - 4);

            cancelButton = Button.builder(
                    Component.literal("✕")
                        .withStyle(s -> s.withColor(ChatFormatting.RED)),
                    btn -> cancelCountdown())
                .bounds(
                    backButton.getX() + backButton.getWidth() - backButton.getHeight(),
                    backButton.getY(),
                    backButton.getHeight(),
                    backButton.getHeight())
                .build();

            addRenderableWidget(cancelButton);
        } else {
            reconnectButton.setWidth(backButton.getWidth());
        }
        addRenderableWidget(reconnectButton);
        backButton.setY(backButton.getY() + backButton.getHeight() + 4);

        if (shouldAutoReconnect) {
            AutoReconnect.startCountdown(this::countdownCallback);
        }
    }

    @Unique
    private void manualReconnect() {
        AutoReconnect.cancelAutoReconnect();
        AutoReconnect.reconnect();
    }

    @Unique
    private void cancelCountdown() {
        AutoReconnect.cancelAutoReconnect();
        shouldAutoReconnect = false;
        removeWidget(cancelButton);
        reconnectButton.active = true; // in case it was deactivated after running out of attempts
        reconnectButton.setMessage(localized("message", "reconnect"));
        reconnectButton.setWidth(backButton.getWidth()); // reset to full width
    }

    @Unique
    private void countdownCallback(int seconds) {
        if (seconds < 0) {
            // indicates that we're out of attempts
            reconnectButton.setMessage(localized("message", "reconnect_failed")
                    .withStyle(s -> s.withColor(ChatFormatting.RED)));
            reconnectButton.active = false;
        } else {
            reconnectButton.setMessage(localized("message", "reconnect_in", seconds)
                    .withStyle(s -> s.withColor(ChatFormatting.GREEN)));
        }
    }

    // cancel auto reconnect when pressing escape, higher priority than exiting the screen
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldAutoReconnect) {
            cancelCountdown();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
