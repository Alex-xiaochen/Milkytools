package com.milky.milkytools.mixin;

import com.milky.milkytools.features.HighlightWaypoint;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界渲染阶段把高亮坐标点交给 Gizmo 系统绘制（文字标签与光柱）。
 * 与坐标光柱使用同一个渲染通道，但各自独立注入。
 */
@Mixin(DebugRenderer.class)
public class HighlightWaypointMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void milkytools$drawHighlightWaypoint(Frustum frustum, double camX, double camY, double camZ,
                                                  float partialTick, CallbackInfo ci) {
        HighlightWaypoint.onRenderGizmos();
    }
}
