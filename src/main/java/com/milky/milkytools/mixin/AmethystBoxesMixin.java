package com.milky.milkytools.mixin;

import com.milky.milkytools.features.AmethystBoxes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界渲染阶段（DebugRenderer.emitGizmos）把紫水晶母岩的方框交给 Gizmo 系统绘制，
 * 与试炼描框使用同一渲染通道。
 */
@Mixin(DebugRenderer.class)
public class AmethystBoxesMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawAmethystBoxes(Frustum frustum, double camX, double camY, double camZ,
                                              float partialTick, CallbackInfo ci) {
        AmethystBoxes.onRenderGizmos();
    }
}
