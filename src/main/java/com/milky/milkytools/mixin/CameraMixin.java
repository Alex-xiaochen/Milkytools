package com.milky.milkytools.mixin;

import com.milky.milkytools.features.MotionCamera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把渲染用的相机原点（CameraRenderState.pos）替换为运动相机计算出的平滑虚拟坐标。
 * 每帧先记录真实相机位置作为跟随目标，启用时再把 state.pos 改成插值后的虚拟坐标。
 */
@Mixin(net.minecraft.client.Camera.class)
public class CameraMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void milkytools$captureAndApply(CameraRenderState state, float partialTicks, CallbackInfo ci) {
        if (state.pos == null) {
            return;
        }

        MotionCamera.captureTarget(state.pos.x, state.pos.y, state.pos.z);

        if (MotionCamera.isActive()) {
            state.pos = MotionCamera.interpolatedPosition(partialTicks);
        }
    }
}
