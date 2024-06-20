/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.terminalmc.autoreconnectrf;

import dev.terminalmc.autoreconnectrf.gui.screen.ConfigScreenProvider;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;


@Mod(AutoReconnect.MOD_ID)
public class AutoReconnectNeoForge {
    public AutoReconnectNeoForge() {
        // Config screen
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent));

        // Main initialization
        AutoReconnect.init();
    }

    @EventBusSubscriber(modid = AutoReconnect.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Post event) {
            AutoReconnect.onEndTick(Minecraft.getInstance());
        }
    }
}
