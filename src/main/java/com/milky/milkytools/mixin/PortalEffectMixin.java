package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 禁用地狱门传送时的全部视觉效果。
 * <p>
 * 原版的传送门强度 {@code LocalPlayer.portalEffectIntensity} 同时驱动两件事：
 * {@code GameRenderer} 的画面旋转/拉伸扭曲，以及 {@code Gui} 的紫色传送门叠加层。
 * 每 tick 在 {@code handlePortalTransitionEffect} 结束后把它和上一 tick 的插值值一起清零，
 * 两者就都不会出现；反胃（Nausea）走的是另一条强度，不受影响。
 * <p>
 * 只在显示层生效：实际的传送由 {@code PortalProcessor} 处理，不读取这个字段。
 * 换维度时 {@code ClientPacketListener} 会把旧玩家的强度复制给新玩家，这里持续清零，
 * 复制过去的也是 0，因此传送落地后不会残留淡出效果。
 */
@Mixin(LocalPlayer.class)
public class PortalEffectMixin {

    @Inject(method = "handlePortalTransitionEffect(Z)V", at = @At("TAIL"))
    private void milkytools$hidePortalEffect(boolean inPortal, CallbackInfo ci) {
        if (!Configs.PORTAL_EFFECT_DISABLED.getBooleanValue()) {
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;
        self.portalEffectIntensity = 0.0F;
        self.oPortalEffectIntensity = 0.0F;
    }
}
