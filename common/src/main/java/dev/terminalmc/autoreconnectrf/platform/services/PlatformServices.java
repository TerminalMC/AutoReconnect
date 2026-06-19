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

package dev.terminalmc.autoreconnectrf.platform.services;

import dev.terminalmc.autoreconnectrf.platform.Services;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

@SuppressWarnings("unused")
public interface PlatformServices {

    PlatformServices INSTANCE = Services.load(PlatformServices.class);

    static PlatformServices getInstance() {
        return INSTANCE;
    }

    /**
     * @return {@code true} if in a development environment.
     */
    boolean isDevEnv();

    /**
     * @return {@code true} if the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * @return {@code true} if the platform uses a mod-named logger in production environments.
     */
    boolean hasNamedLogger();

    /**
     * @return the version of the mod if it is loaded, {@code null} otherwise.
     */
    @Nullable String getModVersion(String modId);

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
