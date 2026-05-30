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
import dev.terminalmc.autoreconnectrf.gui.screen.ConfigScreenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands<S> extends CommandDispatcher<S> {

    private Commands() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static <S> void register(CommandDispatcher<S> dispatcher, CommandBuildContext buildCtx) {
        Minecraft mc = Minecraft.getInstance();
        //noinspection unchecked
        dispatcher.register((LiteralArgumentBuilder<S>) literal(AutoReconnect.MOD_ID)
                .executes((ctx) -> {
                    mc.schedule(() -> mc.gui.setScreen(ConfigScreenProvider.getConfigScreen(null)));
                    return Command.SINGLE_SUCCESS;
                })
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
