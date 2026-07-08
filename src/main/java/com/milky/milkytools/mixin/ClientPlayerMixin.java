package com.milky.milkytools.mixin;

import com.milky.milkytools.features.QuickShulkerSupport;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 容器关闭时清理快捷盒子取物状态，避免状态残留。
 */
@Mixin(LocalPlayer.class)
public class ClientPlayerMixin {
    @Inject(method = "closeContainer", at = @At("TAIL"), require = 0)
    private void milkytools$closeContainer(CallbackInfo ci) {
        QuickShulkerSupport.reset();
    }
}
