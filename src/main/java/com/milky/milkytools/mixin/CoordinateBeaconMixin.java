package com.milky.milkytools.mixin;

import com.milky.milkytools.features.CoordinateBeacon;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在世界渲染阶段把坐标光柱交给 Gizmo 系统绘制，与刷怪笼/宝库描框使用同一渲染通道。
 */
@Mixin(DebugRenderer.class)
public class CoordinateBeaconMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawCoordinateBeacons(Frustum frustum, double camX, double camY, double camZ,
                                                  float partialTick, CallbackInfo ci) {
        CoordinateBeacon.onRenderGizmos();
    }
}
