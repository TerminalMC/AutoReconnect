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

package dev.terminalmc.autoreconnectrf.platform.services;

import java.nio.file.Path;

@SuppressWarnings("unused")
public interface IPlatformServices {

    /**
     * @return {@code true} if in a development environment.
     */
    boolean isDevEnv();

    /**
     * @return {@code true} if the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * @return the name of the current platform.
     */
    String getPlatformName();

    /**
     * @return the game directory of the instance.
     */
    Path getGameDir();

    /**
     * @return the configuration directory of the instance.
     */
    Path getConfigDir();

    /**
     * @return the name of the environment type.
     */
    default String getEnvName() {
        return isDevEnv() ? "development" : "production";
    }
}
