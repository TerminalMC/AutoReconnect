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
import dev.terminalmc.autoreconnectrf.reconnect.SingleplayerReconnectStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Shadow
    public Screen screen;

    @Inject(
            method = "doWorldLoad",
            at = @At("HEAD")
    )
    private void startIntegratedServer(LevelStorageSource.LevelStorageAccess session, PackRepository dataPackManager, WorldStem saveLoader, boolean newWorld, CallbackInfo info) {
        AutoReconnect.setReconnectHandler(new SingleplayerReconnectStrategy(saveLoader.worldData().getLevelName()));
    }

    @Inject(
            method = "setScreen",
            at = @At(
                    value = "FIELD",
                    opcode = 181, // PUTFIELD
                    target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"
            )
    )
    private void setScreen(Screen newScreen, CallbackInfo info) {
        AutoReconnect.onScreenChanged(screen, newScreen);
    }
}
