package com.milky.milkytools.mixin;

import com.milky.milkytools.features.ObsidianBoxes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界渲染阶段（DebugRenderer.emitGizmos）把黑曜石的方框交给 Gizmo 系统绘制，
 * 与试炼描框使用同一渲染通道。
 */
@Mixin(DebugRenderer.class)
public class ObsidianBoxesMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawObsidianBoxes(Frustum frustum, double camX, double camY, double camZ,
                                              float partialTick, CallbackInfo ci) {
        ObsidianBoxes.onRenderGizmos();
    }
}
