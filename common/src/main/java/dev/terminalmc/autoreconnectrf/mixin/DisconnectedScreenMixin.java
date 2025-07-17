/*
 * AutoReconnect
 * Copyright (C) 2023 Bstn1802
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

package dev.terminalmc.autoreconnectrf.mixin;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.mixin.accessor.DisconnectedScreenAccessor;
import dev.terminalmc.autoreconnectrf.util.ScreenMixinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin extends Screen {

    @Shadow
    @Mutable
    private @Final Screen parent;

    @Unique
    private boolean autoreconnectrf$shouldAutoReconnect;

    @Unique
    private Runnable autoreconnectrf$cancelCountdown;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void init(CallbackInfo info) {
        if (AutoReconnect.isPlayingSingleplayer()) {
            // Change back button text to "Back" instead of "Back to World List" bcs of bug fix above
            ScreenMixinUtil.findBackButton(this)
                    .ifPresent(btn -> btn.setMessage(Component.translatable("gui.toWorld")));
        }

        if (AutoReconnect.isPlayingSingleplayer()) {
            // Make back button redirect to SelectWorldScreen instead of MultiPlayerScreen
            // (https://bugs.mojang.com/browse/MC-45602)
            this.parent = new SelectWorldScreen(new TitleScreen());
        }

        if (!AutoReconnect.canReconnect()) {
            autoreconnectrf$shouldAutoReconnect = false;
        } else {
            autoreconnectrf$shouldAutoReconnect = ScreenMixinUtil.checkConditions(this);

            List<Button> buttons = autoreconnectrf$addButtons(
                    ((DisconnectedScreenAccessor) this).autoreconnectrf$getLayout(),
                    autoreconnectrf$shouldAutoReconnect
            );
            Button reconnectButton = buttons.getFirst();
            Button cancelButton = buttons.size() == 2 ? buttons.getLast() : null;

            if (autoreconnectrf$shouldAutoReconnect) {
                AutoReconnect.startCountdown((seconds) -> ScreenMixinUtil.countdownCallback(
                        reconnectButton,
                        seconds
                ));
            }

            autoreconnectrf$cancelCountdown = () -> {
                AutoReconnect.cancelActiveReconnect();
                autoreconnectrf$shouldAutoReconnect = false;
                if (cancelButton != null)
                    removeWidget(cancelButton);
                reconnectButton.active =
                        true; // in case it was deactivated after running out of attempts
                reconnectButton.setMessage(localized("message", "reconnect"));
            };
        }
    }

    @Unique
    private List<Button> autoreconnectrf$addButtons(LinearLayout layout, boolean canCancel) {
        List<Button> buttons = new ArrayList<>();

        Button backButton = ScreenMixinUtil.findBackButton(this)
                .orElseThrow(() -> new NoSuchElementException(
                        "Couldn't find the back button on the disconnect screen"));

        Button reconnectButton = Button.builder(
                localized("message", "reconnect"),
                btn -> AutoReconnect.schedule(
                        () -> Minecraft.getInstance().execute(AutoReconnect::manualReconnect),
                        100,
                        TimeUnit.MILLISECONDS
                )
        ).bounds(0, 0, backButton.getWidth(), backButton.getHeight()).build();
        layout.addChild(reconnectButton);
        buttons.add(reconnectButton);

        if (canCancel) {
            Button cancelButton;
            cancelButton = Button.builder(
                    Component.literal("x").withStyle(ChatFormatting.RED), btn -> {
                        if (autoreconnectrf$cancelCountdown != null) {
                            autoreconnectrf$cancelCountdown.run();
                        }
                    }
            ).bounds(0, 0, backButton.getWidth(), backButton.getHeight()).build();

            layout.addChild(cancelButton);
            buttons.add(cancelButton);
        }

        layout.arrangeElements();
        repositionElements();
        clearWidgets();
        layout.visitWidgets(this::addRenderableWidget);

        return buttons;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && autoreconnectrf$shouldAutoReconnect) {
            if (autoreconnectrf$cancelCountdown != null) {
                autoreconnectrf$cancelCountdown.run();
            }
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    // Make this screen closable by pressing escape
    @Inject(
            method = "shouldCloseOnEsc",
            at = @At("RETURN"),
            cancellable = true
    )
    private void shouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    // Actually return to parent screen and not to the title screen
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
