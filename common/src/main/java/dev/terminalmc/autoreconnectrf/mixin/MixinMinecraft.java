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
    private void startIntegratedServer(LevelStorageSource.LevelStorageAccess session,
                                       PackRepository dataPackManager, WorldStem saveLoader,
                                       boolean newWorld, CallbackInfo info) {
        AutoReconnect.setReconnectStrategy(new SingleplayerReconnectStrategy(
                saveLoader.worldData().getLevelName()));
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
