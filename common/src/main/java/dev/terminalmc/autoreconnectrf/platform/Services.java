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

package dev.terminalmc.autoreconnectrf.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public class Services {

    private static final Logger LOGGER = LoggerFactory.getLogger("AutoReconnect (Service)");

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, clazz.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException(
                        "Failed to load service for " + clazz.getName()));
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }

    public static <T> T loadOr(Class<T> clazz, Supplier<T> supplier) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElse(supplier.get());
        LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
