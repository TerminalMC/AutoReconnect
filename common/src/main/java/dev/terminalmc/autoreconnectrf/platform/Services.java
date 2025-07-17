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

package dev.terminalmc.autoreconnectrf.platform;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.platform.services.IPlatformServices;

import java.util.ServiceLoader;

public class Services {

    public static final IPlatformServices PLATFORM = load(IPlatformServices.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName()));
        AutoReconnect.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
