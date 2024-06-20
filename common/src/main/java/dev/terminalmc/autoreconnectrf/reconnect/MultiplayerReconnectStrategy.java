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

package dev.terminalmc.autoreconnectrf.reconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class MultiplayerReconnectStrategy extends ReconnectStrategy {
    private final ServerData serverInfo;
    private final TransferState cookieStorage;

    public MultiplayerReconnectStrategy(ServerData serverInfo, TransferState cookieStorage) {
        this.serverInfo = serverInfo;
        this.cookieStorage = cookieStorage;
    }

    @Override
    public String getName() {
        return serverInfo.name;
    }

    /**
     * @see net.minecraft.client.quickplay.QuickPlay#joinMultiplayerWorld(Minecraft, String)
     */
    @Override
    public void reconnect() {
        ConnectScreen.startConnecting(
                new JoinMultiplayerScreen(new TitleScreen()),
                Minecraft.getInstance(),
                ServerAddress.parseString(serverInfo.ip),
                serverInfo,
                false,
                cookieStorage);
    }
}
