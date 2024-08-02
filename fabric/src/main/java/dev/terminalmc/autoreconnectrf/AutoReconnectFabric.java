/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.terminalmc.autoreconnectrf;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoReconnectFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(AutoReconnect::onEndTick);

        // Main initialization
        AutoReconnect.init();

        // Debug
//        KeyMapping key = new KeyMapping("AutoReconnect", InputConstants.KEY_H, "Disconnect");
//        KeyBindingHelper.registerKeyBinding(key);
//        ClientTickEvents.END_CLIENT_TICK.register((mc) -> {
//            while (key.consumeClick()) {
//                mc.player.connection.getConnection().disconnect(Component.literal("adios"));
//            }
//        });
    }
}
