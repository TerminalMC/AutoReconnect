/*
 * AutoReconnect
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

package dev.terminalmc.autoreconnectrf.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.autoreconnectrf.AutoReconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands<S> extends CommandDispatcher<S> {

    public void register(CommandDispatcher<S> dispatcher, CommandBuildContext buildContext) {
        Minecraft mc = Minecraft.getInstance();
        dispatcher.register((LiteralArgumentBuilder<S>) literal(AutoReconnect.MOD_ID)
                .then(literal("disconnect")
                        .executes(ctx -> {
                            if (mc.player != null) {
                                mc.player.connection.getConnection().disconnect(
                                        localized("message", "debug.disconnected")
                                );
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }
}
