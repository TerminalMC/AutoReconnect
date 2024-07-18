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

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.mixin.accessor.YACLScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

public class YaclScreenProvider {
    /**
     * Builds and returns a YACL options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the YACL mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.get().options;

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(localized("screen", "options"))
                .save(Config::save);

        // Attempts

        ConfigCategory.Builder attemptsCat = ConfigCategory.createBuilder()
                .name(localized("option", "attempts"));

        attemptsCat.group(ListOption.<Integer>createBuilder()
                .name(localized("option", "attempts.delays"))
                .description(OptionDescription.of(
                        localized("option", "attempts.delays.tooltip")))
                .binding(Config.Options.defaultDelays,
                        () -> options.delays,
                        val -> options.delays = val)
                .controller(val -> IntegerFieldControllerBuilder.create(val).min(1))
                .initial(0)
                .insertEntriesAtEnd(true)
                .minimumNumberOfEntries(1)
                .build());

        attemptsCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "attempts.infinite"))
                .description(OptionDescription.of(
                        localized("option", "attempts.infinite.tooltip")))
                .binding(Config.Options.defaultInfinite,
                        () -> options.infinite,
                        val -> options.infinite = val)
                .controller(BooleanControllerBuilder::create)
                .build());

        // Conditions

        ConfigCategory.Builder conditionsCat = ConfigCategory.createBuilder()
                .name(localized("option", "conditions"));

        conditionsCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "conditions.type"))
                .description(OptionDescription.of(
                        localized("option", "conditions.type.tooltip",
                                localized("option", "conditions.type.positive")
                                        .withStyle(ChatFormatting.GREEN),
                                localized("option", "conditions.type.negative")
                                        .withStyle(ChatFormatting.RED))))
                .binding(Config.Options.defaultConditionType,
                        () -> options.conditionType,
                        val -> options.conditionType = val)
                .controller(val -> BooleanControllerBuilder.create(val)
                        .formatValue(val2 -> val2
                                ? localized("option", "conditions.type.positive")
                                : localized("option", "conditions.type.negative"))
                        .coloured(true))
                .build());

        conditionsCat.group(ListOption.<String>createBuilder()
                .name(localized("option", "conditions.keys"))
                .description(OptionDescription.of(localized("option", "conditions.keys.tooltip",
                        AutoReconnect.lastDcReasonKey == null
                                ? localized("option", "conditions.last.none")
                                : String.format("\"%s\"", AutoReconnect.lastDcReasonKey))))
                .binding(Config.Options.defaultConditionKeys,
                        () -> options.conditionKeys,
                        val -> options.conditionKeys = val)
                .controller(val -> DropdownStringControllerBuilder.create(val)
                        .values(AutoReconnect.DISCONNECT_KEYS)
                        .allowAnyValue(true)
                        .allowEmptyValue(false))
                .initial("")
                .insertEntriesAtEnd(true)
                .build());

        conditionsCat.group(ListOption.<String>createBuilder()
                .name(localized("option", "conditions.patterns"))
                .description(OptionDescription.of(localized("option", "conditions.patterns.tooltip",
                                AutoReconnect.lastDcReasonStr == null
                                        ? localized("option", "conditions.last.none")
                                        : String.format("\"%s\"", AutoReconnect.lastDcReasonStr))))
                .binding(Config.Options.defaultConditionPatterns,
                        () -> options.conditionPatterns,
                        val -> options.conditionPatterns = val)
                .controller(StringControllerBuilder::create)
                .initial("")
                .insertEntriesAtEnd(true)
                .build());

        // Auto messages

        ConfigCategory.Builder messagesCat = ConfigCategory.createBuilder()
                .name(localized("option", "messages"));

        int i = 0;
        for (Config.AutoMessage am : options.autoMessages) {
            i++;
            OptionGroup.Builder amGroup = OptionGroup.createBuilder();
            amGroup.name(localized("option", "messages.instance", i));
            amGroup.description(OptionDescription.of(
                    localized("option", "messages.instance.tooltip")));

            amGroup.option(ButtonOption.createBuilder()
                    .name(localized("option", "messages.instance.delete")
                            .withStyle(ChatFormatting.RED))
                    .action((screen, buttonOption) -> {
                        options.autoMessages.remove(am);
                        reload(screen);
                    })
                    .build());

            amGroup.option(Option.<String>createBuilder()
                    .name(localized("option", "messages.instance.name"))
                    .description(OptionDescription.of(
                            localized("option", "messages.instance.name.tooltip")))
                    .binding(Config.AutoMessage.defaultName,
                            () -> am.name,
                            val -> am.name = val)
                    .controller(StringControllerBuilder::create)
                    .build());

            amGroup.option(Option.<Integer>createBuilder()
                    .name(localized("option", "messages.instance.delay"))
                    .description(OptionDescription.of(
                            localized("option", "messages.instance.delay.tooltip")))
                    .binding(Config.AutoMessage.defaultDelay,
                            () -> am.delay,
                            val -> am.delay = val)
                    .controller(val -> IntegerFieldControllerBuilder.create(val).min(0))
                    .build());

            messagesCat.group(amGroup.build());

            messagesCat.group(ListOption.<String>createBuilder()
                    .name(localized("option", "messages.instance.messages"))
                    .description(OptionDescription.of(
                            localized("option", "messages.instance.messages.tooltip")))
                    .binding(Config.AutoMessage.defaultMessages,
                            () -> am.messages,
                            val -> am.messages = val)
                    .controller(StringControllerBuilder::create)
                    .initial("")
                    .insertEntriesAtEnd(true)
                    .build());
        }

        messagesCat.option(ButtonOption.createBuilder()
                .name(localized("option", "messages.instance.add")
                        .withStyle(ChatFormatting.GREEN))
                .action((yaclScreen, buttonOption) -> {
                    options.autoMessages.addFirst(new Config.AutoMessage());
                    reload(yaclScreen);
                })
                .build());

        builder.category(attemptsCat.build());
        builder.category(conditionsCat.build());
        builder.category(messagesCat.build());

        YetAnotherConfigLib yacl = builder.build();
        return yacl.generateScreen(parent);
    }

    public static void reload(YACLScreen screen) {
        int tab = screen.tabNavigationBar != null
                ? screen.tabNavigationBar.getTabs().indexOf(screen.tabManager.getCurrentTab())
                : 0;
        if (tab == -1) tab = 0;
        screen.finishOrSave();
        screen.onClose(); // In case finishOrSave doesn't close it.
        YACLScreen newScreen = (YACLScreen)ConfigScreenProvider.getConfigScreen(((YACLScreenAccessor)screen).getParent());
        Minecraft.getInstance().setScreen(newScreen);
        try {
            newScreen.tabNavigationBar.selectTab(tab, false);
        } catch (IndexOutOfBoundsException e) {
            AutoReconnect.LOG.warn("YACL reload hack attempted to set tab to index {} but max index was {}.",
                    tab, newScreen.tabNavigationBar.getTabs().size() - 1);
        }
    }
}
