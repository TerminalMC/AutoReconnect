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
import dev.terminalmc.autoreconnectrf.mixin.accessor.DisconnectedScreenAccessor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    private Button autoReconnect$reconnectButton, autoReconnect$cancelButton, autoReconnect$backButton;
    @Unique
    private boolean autoReconnect$shouldAutoReconnect;

    protected MixinDisconnectedScreens(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        autoReconnect$backButton = AutoReconnect.findBackButton(this)
            .orElseThrow(() -> new NoSuchElementException(
                    "Couldn't find the back button on the disconnect screen"));

        autoReconnect$shouldAutoReconnect = Config.get().hasAttempts();

        if ((Screen)this instanceof DisconnectedScreen ds) {
            Component reason = ((DisconnectedScreenAccessor)ds).getDetails().reason();
            String reasonStr = reason.getString();
            AutoReconnect.lastDcReasonStr = reasonStr;
            AutoReconnect.lastDcReasonKey = null;
            boolean match = false;

            for (String condition : Config.get().options.conditionPatterns) {
                try {
                    if (Pattern.compile(condition).matcher(reasonStr).find()) {
                        AutoReconnect.LOG.info("Matched pattern '{}' against reason '{}'",
                                condition, reasonStr);
                        match = true;
                        break;
                    }
                } catch (PatternSyntaxException e) {
                    AutoReconnect.LOG.error(String.format("Invalid pattern: %s\n%s", condition,
                            e.getMessage()));
                }
            }
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
                autoReconnect$shouldAutoReconnect = match;
            } else {
                autoReconnect$shouldAutoReconnect = !match;
            }
        }

        autoReconnect$reconnectButton = Button.builder(
                localized("message", "reconnect"),
                btn -> AutoReconnect.schedule(() -> Minecraft.getInstance().execute(this::autoReconnect$manualReconnect),
                        100, TimeUnit.MILLISECONDS))
            .bounds(0, 0, 0, 20).build();

        // put reconnect (and cancel button) where back button is and push that down
        autoReconnect$reconnectButton.setX(autoReconnect$backButton.getX());
        autoReconnect$reconnectButton.setY(autoReconnect$backButton.getY());
        if (autoReconnect$shouldAutoReconnect) {
            autoReconnect$reconnectButton.setWidth(autoReconnect$backButton.getWidth() - autoReconnect$backButton.getHeight() - 4);

            autoReconnect$cancelButton = Button.builder(
                    Component.literal("✕")
                        .withStyle(s -> s.withColor(ChatFormatting.RED)),
                    btn -> autoReconnect$cancelCountdown())
                .bounds(
                    autoReconnect$backButton.getX() + autoReconnect$backButton.getWidth() - autoReconnect$backButton.getHeight(),
                    autoReconnect$backButton.getY(),
                    autoReconnect$backButton.getHeight(),
                    autoReconnect$backButton.getHeight())
                .build();

            addRenderableWidget(autoReconnect$cancelButton);
        } else {
            autoReconnect$reconnectButton.setWidth(autoReconnect$backButton.getWidth());
        }
        addRenderableWidget(autoReconnect$reconnectButton);
        autoReconnect$backButton.setY(autoReconnect$backButton.getY() + autoReconnect$backButton.getHeight() + 4);

        if (autoReconnect$shouldAutoReconnect) {
            AutoReconnect.startCountdown(this::autoReconnect$countdownCallback);
        }
    }

    @Unique
    private void autoReconnect$manualReconnect() {
        AutoReconnect.cancelAutoReconnect();
        AutoReconnect.reconnect();
    }

    @Unique
    private void autoReconnect$cancelCountdown() {
        AutoReconnect.cancelAutoReconnect();
        autoReconnect$shouldAutoReconnect = false;
        removeWidget(autoReconnect$cancelButton);
        autoReconnect$reconnectButton.active = true; // in case it was deactivated after running out of attempts
        autoReconnect$reconnectButton.setMessage(localized("message", "reconnect"));
        autoReconnect$reconnectButton.setWidth(autoReconnect$backButton.getWidth()); // reset to full width
    }

    @Unique
    private void autoReconnect$countdownCallback(int seconds) {
        if (seconds < 0) {
            // indicates that we're out of attempts
            autoReconnect$reconnectButton.setMessage(localized("message", "reconnect_failed")
                    .withStyle(s -> s.withColor(ChatFormatting.RED)));
            autoReconnect$reconnectButton.active = false;
        } else {
            autoReconnect$reconnectButton.setMessage(localized("message", "reconnect_in", seconds)
                    .withStyle(s -> s.withColor(ChatFormatting.GREEN)));
        }
    }

    // cancel auto reconnect when pressing escape, higher priority than exiting the screen
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && autoReconnect$shouldAutoReconnect) {
            autoReconnect$cancelCountdown();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
