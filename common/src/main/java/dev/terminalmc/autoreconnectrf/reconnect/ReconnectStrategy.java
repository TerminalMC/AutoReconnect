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

public abstract class ReconnectStrategy {
    private int attempt = -1;

    // package-private constructor
    ReconnectStrategy() { }

    public abstract void reconnect();

    public abstract String getName();

    public final int nextAttempt() {
        return ++attempt;
    }

    public final boolean isAttempting() {
        return attempt >= 0;
    }

    public final void resetAttempts() {
        attempt = -1;
    }
}
