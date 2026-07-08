package com.milky.milkytools.mixin;

import com.milky.milkytools.features.TotemParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 ClientLevel.addParticle 中由不死图腾生成的默认金色粒子，
 * 取消它并改用自定义颜色与速度的粒子（DustParticleOptions，可着色）。
 */
@Mixin(ClientLevel.class)
public class TotemParticleMixin {

    @Inject(
            method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void milkytools$replaceTotemParticle(ParticleOptions options, double x, double y, double z,
                                                 double vx, double vy, double vz, CallbackInfo ci) {
        if (!TotemParticle.isActive() || !TotemParticle.shouldReplace(options)) {
            return;
        }

        ci.cancel();

        double xz = TotemParticle.scaleXZ();
        double yScale = TotemParticle.scaleY();
        int color = TotemParticle.fadedColor();

        // 递归调用同一方法：本次传入的是 Dust 粒子，shouldReplace 为 false，不会被再次取消。
        ((ClientLevel) (Object) this).addParticle(
                new DustParticleOptions(color, 1.5F),
                x, y, z,
                vx * xz, vy * yScale, vz * xz
        );
    }
}
