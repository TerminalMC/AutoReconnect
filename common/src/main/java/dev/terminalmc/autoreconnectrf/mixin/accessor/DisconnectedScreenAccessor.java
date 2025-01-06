/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
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

package dev.terminalmc.autoreconnectrf.mixin.accessor;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisconnectedScreen.class)
public interface DisconnectedScreenAccessor {
    @Accessor
    DisconnectionDetails getDetails();

    @Accessor
    LinearLayout getLayout();
}
