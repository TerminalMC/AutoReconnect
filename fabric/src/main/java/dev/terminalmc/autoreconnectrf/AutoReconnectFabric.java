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
    }
}
