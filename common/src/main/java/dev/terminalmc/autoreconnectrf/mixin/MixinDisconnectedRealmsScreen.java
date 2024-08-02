package dev.terminalmc.autoreconnectrf.mixin;

import dev.terminalmc.autoreconnectrf.AutoReconnect;
import dev.terminalmc.autoreconnectrf.util.ScreenMixinUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.DisconnectedRealmsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import static dev.terminalmc.autoreconnectrf.util.Localization.localized;

@Mixin(DisconnectedRealmsScreen.class)
public class MixinDisconnectedRealmsScreen extends Screen {
    @Unique
    private boolean autoReconnect$firstInit = true;
    @Unique
    private boolean autoReconnect$shouldAutoReconnect;
    @Unique
    private Runnable autoReconnect$cancelCountdown;

    protected MixinDisconnectedRealmsScreen(Component title) {
        super(title);
    }

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void init(CallbackInfo info) {
        if (!AutoReconnect.canReconnect()) {
            autoReconnect$shouldAutoReconnect = false;
        }
        else {
            autoReconnect$shouldAutoReconnect = ScreenMixinUtil.checkConditions(this);

            List<Button> buttons = autoReconnect$addButtons(autoReconnect$shouldAutoReconnect);
            Button reconnectButton = buttons.getFirst();
            Button cancelButton = buttons.size() == 2 ? buttons.getLast() : null;

            if (!autoReconnect$firstInit && autoReconnect$shouldAutoReconnect) {
                AutoReconnect.startCountdown(
                        (seconds) -> ScreenMixinUtil.countdownCallback(reconnectButton, seconds));
            }

            autoReconnect$cancelCountdown = () -> {
                AutoReconnect.cancelActiveReconnect();
                autoReconnect$shouldAutoReconnect = false;
                if (cancelButton != null) removeWidget(cancelButton);
                reconnectButton.active = true; // in case it was deactivated after running out of attempts
                reconnectButton.setMessage(localized("message", "reconnect"));
            };
        }
        autoReconnect$firstInit = false;
    }

    @Unique
    private List<Button> autoReconnect$addButtons(boolean canCancel) {
        List<Button> buttons = new ArrayList<>();

        Button backButton = ScreenMixinUtil.findBackButton(this)
                .orElseThrow(() -> new NoSuchElementException(
                        "Couldn't find the back button on the disconnect screen"));

        Button reconnectButton = Button.builder(
                        localized("message", "reconnect"),
                        btn -> AutoReconnect.schedule(() -> Minecraft.getInstance().execute(
                                AutoReconnect::manualReconnect), 100, TimeUnit.MILLISECONDS))
                .bounds(backButton.getX(), backButton.getY() + backButton.getHeight() + 4,
                        backButton.getWidth(), backButton.getHeight())
                .build();
        addRenderableWidget(reconnectButton);
        buttons.add(reconnectButton);

        if (canCancel) {
            Button cancelButton;
            cancelButton = Button.builder(
                            Component.literal("✕")
                                    .withStyle(s -> s.withColor(ChatFormatting.RED)),
                            btn -> {
                                AutoReconnect.cancelActiveReconnect();
                                autoReconnect$shouldAutoReconnect = false;
                                removeWidget(this);
                                reconnectButton.active = true; // in case it was deactivated after running out of attempts
                                reconnectButton.setMessage(localized("message", "reconnect"));
                                reconnectButton.setWidth(backButton.getWidth()); // reset to full width
                            })
                    .bounds(reconnectButton.getX(), reconnectButton.getY() + reconnectButton.getHeight() + 4,
                            backButton.getWidth(), backButton.getHeight())
                    .build();

            addRenderableWidget(cancelButton);
            buttons.add(cancelButton);
        }

        return buttons;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && autoReconnect$shouldAutoReconnect) {
            if (autoReconnect$cancelCountdown != null) {
                autoReconnect$cancelCountdown.run();
            }
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }
}
