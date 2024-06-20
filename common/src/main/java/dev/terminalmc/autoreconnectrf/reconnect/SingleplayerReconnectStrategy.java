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
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class SingleplayerReconnectStrategy extends ReconnectStrategy {
    private final String worldName;

    public SingleplayerReconnectStrategy(String worldName) {
        this.worldName = worldName;
    }

    @Override
    public String getName() {
        return worldName;
    }

    /**
     * @see net.minecraft.client.quickplay.QuickPlay#joinSingleplayerWorld(Minecraft, String)
     */
    @Override
    public void reconnect() {
        Minecraft client = Minecraft.getInstance();
        if (!client.getLevelSource().levelExists(getName())) return;
        client.forceSetScreen(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
        client.createWorldOpenFlows().openWorld(getName(), () -> client.setScreen(new TitleScreen()));
    }
}
