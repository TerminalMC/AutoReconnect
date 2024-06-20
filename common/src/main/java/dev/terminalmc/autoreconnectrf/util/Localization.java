/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package dev.terminalmc.autoreconnectrf.util;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Localization {
    public static String translationKey(String path) {
        return AutoReconnect.MOD_ID + "." + path;
    }

    public static String translationKey(String domain, String path) {
        return domain + "." + AutoReconnect.MOD_ID + "." + path;
    }

    public static MutableComponent localized(String path, Object... args) {
        return Component.translatable(translationKey(path), args);
    }

    public static MutableComponent localized(String domain, String path, Object... args) {
        return Component.translatable(translationKey(domain, path), args);
    }
}
