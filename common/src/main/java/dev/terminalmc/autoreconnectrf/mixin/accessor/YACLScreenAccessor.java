package dev.terminalmc.autoreconnectrf.mixin.accessor;

import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(YACLScreen.class)
public interface YACLScreenAccessor {
    @Accessor
    Screen getParent();
}
