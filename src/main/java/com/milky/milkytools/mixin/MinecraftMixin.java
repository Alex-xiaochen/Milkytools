package com.milky.milkytools.mixin;

import com.milky.milkytools.features.ScreenManagement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 自动打开潜影盒时阻止容器 UI 闪出。
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void milkytools$setScreen(Screen screen, CallbackInfo ci) {
        if (ScreenManagement.closeScreen > 0 && screen instanceof AbstractContainerScreen<?>) {
            ScreenManagement.closeScreen--;
            ScreenManagement.screen = screen;
            ci.cancel();
        }
    }
}
