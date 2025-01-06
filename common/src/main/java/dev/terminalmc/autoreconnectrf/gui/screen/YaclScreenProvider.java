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

package dev.terminalmc.autoreconnectrf.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import dev.isxander.yacl3.gui.controllers.string.StringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.mixin.accessor.YACLScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

public class YaclScreenProvider {
    /**
     * Builds and returns a YACL options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the YACL mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.options();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(localized("name"))
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
                .controller(option -> IntegerFieldControllerBuilder.create(option).min(1))
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
                .controller(option -> BooleanControllerBuilder.create(option)
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
                .controller(option -> DropdownStringControllerBuilder.create(option)
                        .values(DISCONNECT_KEYS)
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
                .controller(option -> IRestrictedStringControllerBuilder
                        .create(option)
                        .validator(val -> {
                            try {
                                Pattern.compile(val);
                                return Optional.empty();
                            } catch (PatternSyntaxException e) {
                                return Optional.of(e.getMessage().replaceAll("\\u000D", ""));
                            }
                        }))
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
                    .controller(option -> IntegerFieldControllerBuilder.create(option).min(0))
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

    // Various shenanigans to implement a custom string option validator, of sorts
    // If you're overly concerned about code quality, look away

    public interface IRestrictedStringControllerBuilder extends ControllerBuilder<String> {
        static IRestrictedStringControllerBuilder create(Option<String> option) {
            return new RestrictedStringControllerBuilder(option);
        }

        IRestrictedStringControllerBuilder validator(Function<String, Optional<String>> validator);
    }

    public static abstract class CustomAbstractControllerBuilder<T> implements ControllerBuilder<T> {
        protected final Option<T> option;
        protected CustomAbstractControllerBuilder(Option<T> option) {
            this.option = option;
        }
    }

    public static class RestrictedStringControllerBuilder extends CustomAbstractControllerBuilder<String>
            implements IRestrictedStringControllerBuilder {
        private Function<String, Optional<String>> validator;

        public RestrictedStringControllerBuilder(Option<String> option) {
            super(option);
        }

        public RestrictedStringControllerBuilder validator(Function<String, Optional<String>> validator) {
            this.validator = validator;
            return this;
        }

        public Controller<String> build() {
            return new RestrictedStringController(this.option, validator);
        }
    }

    private static class RestrictedStringController extends StringController {
        private final @Nullable Function<String, Optional<String>> validator;
        private @Nullable String displayValue;
        private @Nullable YaclScreenProvider.RestrictedStringControllerElement widget;

        public RestrictedStringController(Option<String> option,
                                          @Nullable Function<String, Optional<String>> validator) {
            super(option);
            this.validator = validator;
        }

        @Override
        public Component formatValue() {
            if (displayValue == null) {
                return super.formatValue();
            } else {
                return Component.literal(displayValue).withStyle(ChatFormatting.RED);
            }
        }

        @Override
        public void setFromString(String value) {
            if (validator != null) {
                Optional<String> error = validator.apply(value);
                if (error.isPresent()) {
                    displayValue = value;
                    if (widget != null) {
                        widget.setTooltip(Tooltip.create(Component.literal(error.get())));
                    }
                    return;
                }
            }
            displayValue = null;
            if (widget != null) widget.setTooltip(null);
            super.setFromString(value);
        }

        @Override
        public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
            widget = new RestrictedStringControllerElement(this, screen, widgetDimension, true);
            return widget;
        }
    }

    private static class RestrictedStringControllerElement extends StringControllerElement {
        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public RestrictedStringControllerElement(IStringController<?> control, YACLScreen screen, Dimension<Integer> dim, boolean instantApply) {
            super(control, screen, dim, instantApply);
        }

        public void setTooltip(@Nullable Tooltip tooltip) {
            this.tooltip.set(tooltip);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);
            this.tooltip.refreshTooltipForNextRenderPass(super.isMouseOver(mouseX, mouseY),
                    super.isFocused(), super.getRectangle());
        }
    }

    public static final List<String> DISCONNECT_KEYS = List.of(
            "disconnect.closed",
            "disconnect.disconnected",
            "disconnect.endOfStream",
            "disconnect.exceeded_packet_rate",
            "disconnect.genericReason", // arg
            "disconnect.ignoring_status_request",
            "disconnect.loginFailed",
            "disconnect.loginFailedInfo", // arg
            "disconnect.loginFailedInfo.insufficientPrivileges",
            "disconnect.loginFailedInfo.invalidSession",
            "disconnect.loginFailedInfo.serversUnavailable",
            "disconnect.loginFailedInfo.userBanned",
            "disconnect.lost",
            "disconnect.overflow",
            "disconnect.packetError",
            "disconnect.spam",
            "disconnect.timeout",
            "disconnect.transfer",
            "disconnect.unknownHost",

            "multiplayer.disconnect.authservers_down",
            "multiplayer.disconnect.banned",
            "multiplayer.disconnect.banned_ip.reason", // arg
            "multiplayer.disconnect.banned.reason", // arg
            "multiplayer.disconnect.chat_validation_failed",
            "multiplayer.disconnect.duplicate_login",
            "multiplayer.disconnect.expired_public_key",
            "multiplayer.disconnect.flying",
            "multiplayer.disconnect.generic",
            "multiplayer.disconnect.idling",
            "multiplayer.disconnect.illegal_characters",
            "multiplayer.disconnect.incompatible", // arg
            "multiplayer.disconnect.invalid_entity_attacked",
            "multiplayer.disconnect.invalid_packet",
            "multiplayer.disconnect.invalid_player_data",
            "multiplayer.disconnect.invalid_player_movement",
            "multiplayer.disconnect.invalid_public_key_signature",
            "multiplayer.disconnect.invalid_public_key_signature",
            "multiplayer.disconnect.invalid_vehicle_movement",
            "multiplayer.disconnect.ip_banned",
            "multiplayer.disconnect.kicked",
            "multiplayer.disconnect.missing_tags",
            "multiplayer.disconnect.name_taken",
            "multiplayer.disconnect.not_whitelisted",
            "multiplayer.disconnect.out_of_order_chat",
            "multiplayer.disconnect.outdated_client", // arg
            "multiplayer.disconnect.outdated_server", // arg
            "multiplayer.disconnect.server_full",
            "multiplayer.disconnect.server_shutdown",
            "multiplayer.disconnect.slow_login",
            "multiplayer.disconnect.too_many_pending_chats",
            "multiplayer.disconnect.transfers_disabled",
            "multiplayer.disconnect.unexpected_query_response",
            "multiplayer.disconnect.unsigned_chat",
            "multiplayer.disconnect.unverified_username",

            "multiplayer.requiredTexturePrompt.disconnect"
    );
}
