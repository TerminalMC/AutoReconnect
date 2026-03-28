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

package dev.terminalmc.autoreconnectrf.gui.screen;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.string.IStringController;
import dev.isxander.yacl3.gui.controllers.string.StringControllerElement;
import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.config.Config;
import dev.terminalmc.autoreconnectrf.config.Config.Options;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

public class YaclScreenProvider {

    static boolean regexIdsTemp;

    /**
     * Builds and returns a YACL options screen.
     *
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the YACL mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.options();

        YetAnotherConfigLib.Builder builder =
                YetAnotherConfigLib.createBuilder().title(localized("name")).save(Config::save);

        // Attempts

        ConfigCategory.Builder attemptsCat =
                ConfigCategory.createBuilder().name(localized("option", "attempts"));

        attemptsCat.group(ListOption.<Integer>createBuilder()
                .name(localized("option", "attempts.delays"))
                .description(OptionDescription.of(localized("option", "attempts.delays.tooltip")))
                .binding(
                        Config.Options.delaysDefault.get(),
                        () -> options.delays,
                        val -> options.delays = val
                )
                .controller(option -> IntegerFieldControllerBuilder.create(option).min(1)
                        .formatValue((val) -> localized("option", "attempts.delays.value", val)))
                .initial(0)
                .insertEntriesAtEnd(true)
                .minimumNumberOfEntries(1)
                .build());

        attemptsCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "attempts.infinite"))
                .description(OptionDescription.of(localized("option", "attempts.infinite.tooltip")))
                .binding(
                        Config.Options.infiniteDefault,
                        () -> options.infinite,
                        val -> options.infinite = val
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        // Conditions

        ConfigCategory.Builder conditionsCat =
                ConfigCategory.createBuilder().name(localized("option", "conditions"));

        conditionsCat.option(LabelOption.createBuilder()
                .lines(List.of(localized("option", "conditions.tooltip")))
                .build());

        conditionsCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "conditions.type"))
                .description(OptionDescription.of(localized(
                        "option",
                        "conditions.type.tooltip",
                        localized(
                                "option",
                                "conditions.type.positive"
                        ).withStyle(ChatFormatting.GREEN),
                        localized(
                                "option",
                                "conditions.type.negative"
                        ).withStyle(ChatFormatting.RED)
                )))
                .binding(
                        Config.Options.conditionTypeDefault,
                        () -> options.conditionType,
                        val -> options.conditionType = val
                )
                .controller(option -> BooleanControllerBuilder.create(option)
                        .formatValue(val2 -> val2
                                ? localized("option", "conditions.type.positive")
                                : localized("option", "conditions.type.negative"))
                        .coloured(true))
                .build());

        conditionsCat.group(ListOption.<String>createBuilder()
                .name(localized("option", "conditions.keys"))
                .description(OptionDescription.of(localized(
                        "option",
                        "conditions.keys.tooltip",
                        AutoReconnect.lastDcReasonKey == null ? localized(
                                "option",
                                "conditions.last.none"
                        ) : String.format("\"%s\"", AutoReconnect.lastDcReasonKey)
                )))
                .binding(
                        Config.Options.conditionKeysDefault.get(),
                        () -> options.conditionKeys,
                        val -> options.conditionKeys = val
                )
                .controller(option -> DropdownStringControllerBuilder.create(option)
                        .values(DISCONNECT_KEYS)
                        .allowAnyValue(true)
                        .allowEmptyValue(false))
                .initial("")
                .insertEntriesAtEnd(true)
                .build());

        conditionsCat.group(ListOption.<String>createBuilder()
                .name(localized("option", "conditions.patterns"))
                .description(OptionDescription.of(localized(
                        "option",
                        "conditions.patterns.tooltip",
                        AutoReconnect.lastDcReasonStr == null
                                ? localized("option", "conditions.last.none")
                                : String.format("\"%s\"", AutoReconnect.lastDcReasonStr)
                )))
                .binding(
                        Config.Options.conditionPatternsDefault.get(),
                        () -> options.conditionPatterns,
                        val -> options.conditionPatterns = val
                )
                .controller(option -> IRestrictedStringControllerBuilder.create(option)
                        .validator(val -> {
                            try {
                                Pattern.compile(val);
                                return Optional.empty();
                            } catch (PatternSyntaxException e) {
                                return Optional.of(fixRegexMessage(e.getMessage()));
                            }
                        })
                )
                .initial("")
                .insertEntriesAtEnd(true)
                .build());

        // Auto messages

        ConfigCategory.Builder messagesCat =
                ConfigCategory.createBuilder().name(localized("option", "messages"));

        messagesCat.option(ButtonOption.createBuilder()
                .name(localized("option", "messages.instance.add").withStyle(ChatFormatting.GREEN))
                .action((yaclScreen, buttonOption) -> {
                    options.autoMessages.add(new Config.AutoMessage());
                    reload(yaclScreen, parent);
                })
                .build());

        int i = options.autoMessages.size();
        for (Config.AutoMessage am : options.autoMessages.reversed()) {
            OptionGroup.Builder amGroup = OptionGroup.createBuilder();
            amGroup.name(localized("option", "messages.instance", i--));
            amGroup.description(OptionDescription.of(localized(
                    "option",
                    "messages.instance.tooltip"
            )));

            amGroup.option(ButtonOption.createBuilder()
                    .name(localized(
                            "option",
                            "messages.instance.delete"
                    ).withStyle(ChatFormatting.RED))
                    .action((screen, buttonOption) -> {
                        options.autoMessages.remove(am);
                        reload(screen, parent);
                    })
                    .build());

            amGroup.option(Option.<String>createBuilder()
                    .name(localized("option", "messages.instance.id"))
                    .description(OptionDescription.of(localized(
                            "option",
                            "messages.instance.id.tooltip"
                    )))
                    .binding(Config.AutoMessage.idDefault, () -> am.id, val -> am.id = val)
                    .controller(option -> IRestrictedStringControllerBuilder.create(option)
                            .validator(val -> {
                                if (regexIdsTemp) {
                                    try {
                                        Pattern.compile(val);
                                        return Optional.empty();
                                    } catch (PatternSyntaxException e) {
                                        return Optional.of(fixRegexMessage(e.getMessage()));
                                    }
                                } else {
                                    return Optional.empty();
                                }
                            })
                    )
                    .build());

            amGroup.option(Option.<Float>createBuilder()
                    .name(localized("option", "messages.instance.delay"))
                    .description(OptionDescription.of(localized(
                            "option",
                            "messages.instance.delay.tooltip"
                    )))
                    .binding(Config.AutoMessage.delayDefault, () -> am.delay, val -> am.delay = val)
                    .controller(option -> FloatFieldControllerBuilder.create(option).min(0F)
                            .formatValue((val) -> localized(
                                    "option",
                                    "messages.instance.delay.value",
                                    val
                            )))
                    .build());

            messagesCat.group(amGroup.build());

            messagesCat.group(ListOption.<String>createBuilder()
                    .name(localized("option", "messages.instance.messages"))
                    .description(OptionDescription.of(localized(
                            "option",
                            "messages.instance.messages.tooltip"
                    )))
                    .binding(
                            Config.AutoMessage.messagesDefault.get(),
                            () -> am.messages,
                            val -> am.messages = val
                    )
                    .controller(StringControllerBuilder::create)
                    .initial("")
                    .insertEntriesAtEnd(true)
                    .build());
        }

        // Misc

        ConfigCategory.Builder miscCat =
                ConfigCategory.createBuilder().name(localized("option", "misc"));

        miscCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "misc.commandSigning"))
                .description(OptionDescription.of(localized(
                        "option",
                        "misc.commandSigning.tooltip"
                )))
                .binding(
                        Options.commandSigningDefault,
                        () -> options.commandSigning,
                        val -> options.commandSigning = val
                )
                .controller(BooleanControllerBuilder::create)
                .build());

        miscCat.option(Option.<Boolean>createBuilder()
                .name(localized("option", "misc.regexIds"))
                .description(OptionDescription.of(localized(
                        "option",
                        "misc.regexIds.tooltip"
                )))
                .binding(
                        Options.regexIdsDefault,
                        () -> options.regexIds,
                        val -> options.regexIds = val
                )
                .addListener((option, event) ->
                        YaclScreenProvider.regexIdsTemp = option.pendingValue())
                .controller(BooleanControllerBuilder::create)
                .build());

        builder.category(attemptsCat.build());
        builder.category(conditionsCat.build());
        builder.category(messagesCat.build());
        builder.category(miscCat.build());

        YetAnotherConfigLib yacl = builder.build();
        return yacl.generateScreen(parent);
    }

    /**
     * Creates a new YACL screen and switches to it.
     *
     * @param screen the current screen.
     * @param parent the current screen's parent.
     */
    private static void reload(YACLScreen screen, Screen parent) {
        try {
            int tab = screen.tabNavigationBar == null
                    ? 0
                    : screen.tabNavigationBar.getTabs().indexOf(screen.tabManager.getCurrentTab());
            if (tab == -1)
                tab = 0;
            screen.finishOrSave();
            screen.onClose(); // In case finishOrSave doesn't close it.
            YACLScreen newScreen = (YACLScreen) ConfigScreenProvider.getConfigScreen(parent);
            newScreen.init(screen.width, screen.height);
            try {
                newScreen.tabNavigationBar.selectTab(tab, false);
            } catch (IndexOutOfBoundsException e) {
                AutoReconnect.LOG.warn(
                        "YACL reload hack attempted to select tab {} but max index was {}",
                        tab,
                        newScreen.tabNavigationBar.getTabs().size() - 1
                );
            }
            Minecraft.getInstance().setScreen(newScreen);
        } catch (Exception e) {
            Minecraft.getInstance().setScreen(parent);
            AutoReconnect.LOG.error("YACL reload hack failed with exception\n{}", e);
        }
    }

    /**
     * Adjusts {@link PatternSyntaxException} description messages for correct display in tooltips.
     * <p>
     * Messages are intended for display using monospaced fonts, so the caret indicating the error
     * position will usually be in the wrong place when displayed using the Minecraft font. This
     * method simply moves the caret to a new position as close as possible to the correct one.
     * <p>
     * Also, messages may contain carriage-return characters which don't play well with Minecraft so
     * this method removes them.
     */
    public static String fixRegexMessage(String str) {
        // Remove carriage returns
        str = str.replaceAll("\\u000D", "");

        // If there is a cursor, fix its position
        if (str.endsWith("^")) {
            Matcher indexMatcher = Pattern.compile("near index (\\d+)\n").matcher(str);
            if (indexMatcher.find()) {
                Font font = Minecraft.getInstance().font;
                // Get the index that the cursor is pointing to
                int index = Integer.parseInt(indexMatcher.group(1));
                // Determine the cursor offset distance
                int startPos = indexMatcher.end();
                int cursorPos = startPos + index;
                int cursorOffset = font.width(str.substring(startPos, cursorPos));
                // Construct the new offset space
                char[] charArray = new char[cursorOffset / font.width(" ")];
                Arrays.fill(charArray, ' ');
                String newSpace = new String(charArray);

                Matcher cursorMatcher = Pattern.compile("\n( *\\^)$").matcher(str);
                if (cursorMatcher.find(cursorPos)) {
                    // Get the original cursor and its offset space
                    String cursorLine = cursorMatcher.group(1);
                    // Replace the old space with the new
                    str = str.replaceAll(Pattern.quote(cursorLine) + "$", newSpace + "^");
                }
            }
        }
        return str;
    }

    // Various shenanigans to implement a custom string option validator, of sorts
    // If you're overly concerned about code quality, look away

    public interface IRestrictedStringControllerBuilder extends ControllerBuilder<String> {

        static IRestrictedStringControllerBuilder create(Option<String> option) {
            return new RestrictedStringControllerBuilder(option);
        }

        IRestrictedStringControllerBuilder validator(Function<String, Optional<String>> validator);
    }

    public static abstract class CustomAbstractControllerBuilder<T>
            implements ControllerBuilder<T> {

        protected final Option<T> option;

        protected CustomAbstractControllerBuilder(Option<T> option) {
            this.option = option;
        }
    }

    public static class RestrictedStringControllerBuilder
            extends CustomAbstractControllerBuilder<String>
            implements IRestrictedStringControllerBuilder {

        private Function<String, Optional<String>> validator;

        public RestrictedStringControllerBuilder(Option<String> option) {
            super(option);
        }

        public RestrictedStringControllerBuilder validator(
                Function<String, Optional<String>> validator
        ) {
            this.validator = validator;
            return this;
        }

        public Controller<String> build() {
            return new RestrictedStringController(this.option, validator);
        }
    }

    private static class RestrictedStringController implements IStringController<String> {

        private final Option<String> option;
        private final @Nullable Function<String, Optional<String>> validator;
        private @Nullable String displayValue;
        private @Nullable YaclScreenProvider.RestrictedStringControllerElement widget;

        public RestrictedStringController(
                Option<String> option,
                @Nullable Function<String, Optional<String>> validator
        ) {
            this.option = option;
            this.validator = validator;
        }

        @Override
        public Component formatValue() {
            if (displayValue == null) {
                return Component.literal(getString());
            } else {
                return Component.literal(displayValue).withStyle(ChatFormatting.RED);
            }
        }

        @Override
        public Option<String> option() {
            return option;
        }

        @Override
        public String getString() {
            return option().pendingValue();
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
            if (widget != null)
                widget.setTooltip(null);
            option.requestSet(value);
        }

        @Override
        public AbstractWidget provideWidget(
                YACLScreen screen,
                dev.isxander.yacl3.api.utils.Dimension<Integer> widgetDimension
        ) {
            widget = new RestrictedStringControllerElement(this, screen, widgetDimension, true);
            return widget;
        }
    }

    private static class RestrictedStringControllerElement extends StringControllerElement {

        private final WidgetTooltipHolder tooltip = new WidgetTooltipHolder();

        public RestrictedStringControllerElement(
                IStringController<?> control,
                YACLScreen screen,
                Dimension<Integer> dim,
                boolean instantApply
        ) {
            super(control, screen, dim, instantApply);
        }

        public void setTooltip(@Nullable Tooltip tooltip) {
            this.tooltip.set(tooltip);
        }

        @Override
        public void extractRenderState(
                @NotNull GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float delta
        ) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            this.tooltip.refreshTooltipForNextRenderPass(
                    graphics,
                    mouseX,
                    mouseY,
                    super.isMouseOver(mouseX, mouseY),
                    super.isFocused(),
                    super.getRectangle()
            );
        }
    }

    /**
     * A list of disconnect reason keys, obtained by searching for "disconnect." and ".disconnect"
     * in 1.21.1 and 1.21.8 lang files.
     */
    public static final List<String> DISCONNECT_KEYS = List.of(
            "disconnect.closed",
            "disconnect.disconnected",
            "disconnect.endOfStream",
            "disconnect.exceeded_packet_rate",
            "disconnect.genericReason", // arg
            "disconnect.ignoring_status_request",
//            "disconnect.kicked",
            "disconnect.loginFailed",
            "disconnect.loginFailedInfo", // arg
            "disconnect.loginFailedInfo.insufficientPrivileges",
            "disconnect.loginFailedInfo.invalidSession",
            "disconnect.loginFailedInfo.serversUnavailable",
            "disconnect.loginFailedInfo.userBanned",
            "disconnect.lost",
            "disconnect.overflow",
            "disconnect.packetError",
//            "disconnect.quitting",
            "disconnect.spam",
            "disconnect.timeout",
            "disconnect.transfer",
            "disconnect.unknownHost",

            "menu.custom_screen_info.disconnect",

            "multiplayer.disconnect.authservers_down",
            "multiplayer.disconnect.bad_chat_index",
            "multiplayer.disconnect.banned",
//            "multiplayer.disconnect.banned_ip.expiration", // arg
            "multiplayer.disconnect.banned_ip.reason", // arg
//            "multiplayer.disconnect.banned.expiration", // arg
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
            "multiplayer.disconnect.invalid_public_key_signature.new",
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
