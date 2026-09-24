package com.milky.milkytools.mixin;

import com.milky.milkytools.features.SpawnerBoxes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界渲染阶段（DebugRenderer.emitGizmos）把刷怪笼/宝库的方框交给 Gizmo 系统绘制，
 * 与箭矢/珍珠轨迹使用同一渲染通道。
 */
@Mixin(DebugRenderer.class)
public class SpawnerBoxesMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawSpawnerBoxes(Frustum frustum, double camX, double camY, double camZ,
                                             float partialTick, CallbackInfo ci) {
        SpawnerBoxes.onRenderGizmos();
    }
}
