package com.milky.milkytools.mixin;

import com.milky.milkytools.features.ScreenManagement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 自动打开潜影盒时阻止容器 UI 闪出。
 * <p>
 * 注入目标 {@code setScreen} 在 26.2 从 {@code Minecraft} 搬到了 {@code Gui}。两处的
 * 签名完全相同，所以只有 {@code @Mixin} 的目标类需要按版本分叉，方法体只写一遍。
 * 注解里用全限定名，避免为两个版本各写一个条件化的 import。
 */
//? if >=26.2 {
@Mixin(net.minecraft.client.gui.Gui.class)
//?} else {
/*@Mixin(net.minecraft.client.Minecraft.class)
*///?}
public class ScreenManagementMixin {
    @Inject(
            method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void milkytools$setScreen(Screen screen, CallbackInfo ci) {
        if (ScreenManagement.closeScreen > 0 && screen instanceof AbstractContainerScreen<?>) {
            ScreenManagement.closeScreen--;
            ScreenManagement.screen = screen;
            ci.cancel();
        }
    }
}
