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

package dev.terminalmc.autoreconnectrf.util;

import dev.terminalmc.autoreconnectrf.platform.services.PlatformServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

@SuppressWarnings("unused")
public class Logging {

    private Logging() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static Logger getLogger(String name) {
        if (PlatformServices.getInstance().isDevEnv()
                || PlatformServices.getInstance().hasNamedLogger()) {
            return LogManager.getLogger(name);
        } else {
            return LogManager.getLogger(name, new PrefixingMessageFactory("[" + name + "/]: "));
        }
    }

    private static final class PrefixingMessageFactory extends AbstractMessageFactory {

        private final String prefix;

        public PrefixingMessageFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Message newMessage(String message) {
            return new SimpleMessage(prefix + message);
        }

        @Override
        public Message newMessage(String message, Object... params) {
            return new FormattedMessage(prefix + message, params);
        }
    }
}
