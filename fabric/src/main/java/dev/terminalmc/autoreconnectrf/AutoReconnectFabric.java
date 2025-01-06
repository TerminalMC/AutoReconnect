/*
 * AutoReconnect
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
