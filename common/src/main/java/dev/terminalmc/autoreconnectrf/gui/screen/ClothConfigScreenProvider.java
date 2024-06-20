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

package dev.terminalmc.autoreconnectrf.gui.screen;

import dev.terminalmc.autoreconnectrf.config.Config;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.gui.entries.IntegerListListEntry;
import me.shedaniel.clothconfig2.gui.entries.MultiElementListEntry;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

public class ClothConfigScreenProvider {
    /**
     * Builds and returns a Cloth Config options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not
     * available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.get().options;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("screen", "options"))
                .setSavingRunnable(Config::getAndSave);

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(localized("option", "general"));

        general.addEntry(eb.startIntList(localized("option", "delays"), options.delays)
                .setTooltip(localized("option", "delays.tooltip"))
                .setCreateNewInstance(list -> new IntegerListListEntry.IntegerListCell(Config.Options.defaultDelay, list))
                .setInsertInFront(false)
                .setMin(1)
                .setExpanded(true)
                .setDefaultValue(Config.Options.defaultDelays)
                .setSaveConsumer(val -> options.delays = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "infinite"), options.infinite)
                .setTooltip(localized("option", "infinite.tooltip"))
                .setDefaultValue(Config.Options.defaultInfinite)
                .setSaveConsumer(val -> options.infinite = val)
                .build());
        
        general.addEntry(new NestedListListEntry<Config.AutoMessage, MultiElementListEntry<Config.AutoMessage>>(
                localized("option", "automessages"),
                options.autoMessages,
                true,
                () -> Optional.of(new Component[]{localized("option", "automessages.tooltip")}),
                list -> options.autoMessages = list,
                () -> Config.Options.defaultAutoMessages,
                eb.getResetButtonKey(),
                true,
                false,
                (autoMessages, listListEntry) -> createAutoMessageEntry(
                        eb, autoMessages != null ? autoMessages : new Config.AutoMessage())));

        return builder.build();
    }

    private static MultiElementListEntry<Config.AutoMessage> createAutoMessageEntry(
            ConfigEntryBuilder eb, Config.AutoMessage autoMessage) {
        MultiElementListEntry<Config.AutoMessage> entry = new MultiElementListEntry<>(
                localized("option", "automessage"), autoMessage, Arrays.asList(
                        eb.startTextField(localized("option", "automessage.name"),
                                        autoMessage.name)
                        .setErrorSupplier(ClothConfigScreenProvider::emptyStringErrorSupplier)
                        .setDefaultValue(Config.AutoMessage.defaultName)
                        .setTooltip(localized("option", "automessage.name.tooltip"))
                        .setSaveConsumer(name -> autoMessage.name = name)
                        .build(), 
                eb.startStrList(localized("option", "automessage.messages"),
                                autoMessage.messages)
                        .setErrorSupplier(ClothConfigScreenProvider::emptyListErrorSupplier)
                        .setCellErrorSupplier(ClothConfigScreenProvider::emptyStringErrorSupplier)
                        .setDefaultValue(Config.AutoMessage.defaultMessages)
                        .setInsertInFront(false)
                        .setExpanded(true)
                        .setTooltip(localized("option", "automessage.messages.tooltip"))
                        .setSaveConsumer(messages -> autoMessage.messages = messages)
                        .build(), 
                eb.startIntField(localized("option", "automessage.delay"),
                                autoMessage.delay)
                        .setDefaultValue(Config.AutoMessage.defaultDelay)
                        .setTooltip(localized("option", "automessage.delay.tooltip"))
                        .setMin(1)
                        .setSaveConsumer(delay -> autoMessage.delay = delay)
                        .build()
        ), true);
        entry.setTooltipSupplier(() -> Optional.of(new Component[] {
                localized("option", "automessage.tooltip")
        }));
        return entry;
    }

    private static Optional<Component> emptyListErrorSupplier(List<?> list) {
        return list == null || list.isEmpty() ? Optional.of(localized(
                "option", "error.empty_list")) : Optional.empty();
    }

    private static Optional<Component> emptyStringErrorSupplier(String str) {
        return str == null || str.isEmpty() ? Optional.of(localized(
                "option", "error.empty_string")) : Optional.empty();
    }
}
