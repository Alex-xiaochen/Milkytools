package com.milky.milkytools.mixin;

import com.milky.milkytools.features.ViewArrow;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把 F3+B 调试渲染中实体的蓝色视线箭头改为可配置长度。
 * 原版 showHitboxes 里用 {@code getViewVector(partialTick).scale(2.0)} 构造箭头终点，
 * 这里重定向该缩放调用，交由 {@link ViewArrow} 决定实际长度。
 * <p>
 * {@code showHitboxes} 是私有方法但四个版本签名一致，且 {@code Vec3.scale(D)} 在
 * {@code EntityHitboxDebugRenderer} 内只出现这一次，所以固定目标即可，不需要版本条件。
 */
@Mixin(EntityHitboxDebugRenderer.class)
public class ViewArrowMixin {

    @Redirect(
            method = "showHitboxes(Lnet/minecraft/world/entity/Entity;FZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"
            ),
            require = 1
    )
    private Vec3 milkytools$extendViewArrow(Vec3 viewVector, double vanillaScale) {
        return ViewArrow.scale(viewVector, vanillaScale);
    }
}
