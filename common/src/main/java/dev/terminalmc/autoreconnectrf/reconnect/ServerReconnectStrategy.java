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

package dev.terminalmc.autoreconnectrf.reconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class ServerReconnectStrategy extends ReconnectStrategy {

    private final ServerData serverData;
    private final TransferState transferState;

    public ServerReconnectStrategy(ServerData serverData, TransferState transferState) {
        this.serverData = serverData;
        this.transferState = transferState;
    }

    @Override
    public String getId() {
        // Servers are identified by their IP
        return serverData.ip;
    }

    /**
     * @see net.minecraft.client.quickplay.QuickPlay#joinMultiplayerWorld
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public void reconnect() {
        ConnectScreen.startConnecting(
                new JoinMultiplayerScreen(new TitleScreen()),
                Minecraft.getInstance(),
                ServerAddress.parseString(serverData.ip),
                serverData,
                false,
                transferState
        );
    }
}
