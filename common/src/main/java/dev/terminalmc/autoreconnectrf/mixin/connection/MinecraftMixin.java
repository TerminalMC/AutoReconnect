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

package dev.terminalmc.autoreconnectrf.mixin.connection;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.reconnect.WorldReconnectStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /**
     * Singleplayer connection event notifier.
     */
    @Inject(
            method = "doWorldLoad",
            at = @At("HEAD")
    )
    private void onWorldConnect(
            LevelStorageSource.LevelStorageAccess storage,
            PackRepository packRepo,
            WorldStem worldStem,
            boolean newWorld,
            CallbackInfo ci
    ) {
        AutoReconnect.setReconnectStrategy(
                new WorldReconnectStrategy(worldStem.worldData().getLevelName()));
    }
}
