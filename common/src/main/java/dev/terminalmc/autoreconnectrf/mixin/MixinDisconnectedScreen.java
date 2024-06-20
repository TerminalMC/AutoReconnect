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
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisconnectedScreen.class)
public class MixinDisconnectedScreen extends Screen {
    @Shadow
    @Final
    @Mutable
    private Screen parent;

    protected MixinDisconnectedScreen(Component title) {
        super(title);
    }

    @Inject(
            method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN")
    )
    private void constructor(Screen parent, Component title, Component reason, Component buttonLabel, CallbackInfo info) {
        if (AutoReconnect.isPlayingSingleplayer()) {
            // make back button redirect to SelectWorldScreen instead of MultiPlayerScreen (https://bugs.mojang.com/browse/MC-45602)
            this.parent = new SelectWorldScreen(new TitleScreen());
        }
    }

    @Inject(at = @At("RETURN"), method = "init")
    private void init(CallbackInfo info) {
        if (AutoReconnect.isPlayingSingleplayer()) {
            // change back button text to "Back" instead of "Back to World List" bcs of bug fix above
            AutoReconnect.findBackButton(this).ifPresent(btn -> btn.setMessage(Component.translatable("gui.toWorld")));
        }
    }

    // make this screen closable by pressing escape
    @Inject(at = @At("RETURN"), method = "shouldCloseOnEsc", cancellable = true)
    private void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }

    // actually return to parent screen and not to the title screen
    @Override
    public void onClose() {
        assert this.minecraft != null;
        this.minecraft.setScreen(parent);
    }
}
