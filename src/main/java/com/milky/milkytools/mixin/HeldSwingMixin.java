package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 第一人称手臂挥动速度调整（ViewModel 的一部分）。
 * 仅缩放 LocalPlayer 的攻击动画返回值（纯视觉），不影响攻击判定与 swingTime。
 */
@Mixin(LivingEntity.class)
public class HeldSwingMixin {

    @Inject(method = "getAttackAnim", at = @At("RETURN"), cancellable = true)
    private void milkytools$adjustSwingSpeed(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!Configs.HELD_MODEL_ENABLED.getBooleanValue()) {
            return;
        }
        if (!((Object) this instanceof LocalPlayer)) {
            return;
        }

        float speed = (float) Configs.HELD_MODEL_SWING_SPEED.getDoubleValue();
        if (speed == 1.0F) {
            return;
        }

        float base = cir.getReturnValueF();
        float adjusted = base * speed;
        if (adjusted > 1.0F) adjusted = 1.0F;
        if (adjusted < 0.0F) adjusted = 0.0F;
        cir.setReturnValue(adjusted);
    }
}
