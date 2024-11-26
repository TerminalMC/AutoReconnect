/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
 * Copyright (C) 2024 TerminalMC
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

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public class RealmsReconnectStrategy extends ReconnectStrategy {
    private final RealmsServer realmsServer;

    public RealmsReconnectStrategy(RealmsServer realmsServer) {
        this.realmsServer = realmsServer;
    }

    @Override
    public String getName() {
        return realmsServer.getName();
    }

    /**
     * @see net.minecraft.client.quickplay.QuickPlay#joinRealmsWorld(Minecraft, RealmsClient, String)
     */
    @Override
    public void reconnect() {
        TitleScreen titleScreen = new TitleScreen();
        GetServerDetailsTask realmsPrepareConnectionTask = new GetServerDetailsTask(
                new RealmsMainScreen(titleScreen), realmsServer);
        Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(
                titleScreen, realmsPrepareConnectionTask));
    }
}
