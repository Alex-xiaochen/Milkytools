package com.milky.milkytools.mixin;

import com.milky.milkytools.features.CameraAccess;
import com.milky.milkytools.features.MotionCamera;
//? if >=26.1.2 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;*/
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把渲染用的相机原点替换为运动相机计算出的平滑虚拟坐标。
 * 每帧先记录真实相机位置作为跟随目标，启用时再把相机位置改成插值后的虚拟坐标。
 * <p>
 * 注入点随版本变化：
 * <ul>
 *   <li>26.1.2 起：{@code Camera#extractRenderState}（26.3 把第二个参数从 float 换成 DeltaTracker）</li>
 *   <li>1.21.11：{@code Camera} 还没有 extractRenderState，改注入
 *       {@code GameRenderer#extractCamera} —— 它是填充 CameraRenderState 的地方</li>
 * </ul>
 * 因此本 mixin 在 1.21.11 上的目标类是 GameRenderer，另外三个版本是 Camera。
 */
//? if >=26.1.2 {
@Mixin(net.minecraft.client.Camera.class)
//?} else {
/*@Mixin(net.minecraft.client.renderer.GameRenderer.class)
*///?}
public class CameraMixin {

    //? if >=26.3 {
    /*@Inject(
            method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("TAIL"), require = 1)
    private void milkytools$captureAndApply(CameraRenderState state, net.minecraft.client.DeltaTracker delta, CallbackInfo ci) {
        milkytools$apply(state, delta.getGameTimeDeltaPartialTick(false));
    }*/
    //?} else if >=26.1.2 {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/renderer/state/level/CameraRenderState;F)V",
            at = @At("TAIL"), require = 1)
    private void milkytools$captureAndApply(CameraRenderState state, float partialTicks, CallbackInfo ci) {
        milkytools$apply(state, partialTicks);
    }
    //?} else {
    /*@Inject(method = "extractCamera(F)V", at = @At("TAIL"), require = 1)
    private void milkytools$captureAndApply(float partialTicks, CallbackInfo ci) {
        milkytools$apply(CameraAccess.camera(), partialTicks);
    }*/
    //?}

    @Unique
    private void milkytools$apply(CameraRenderState state, float partialTicks) {
        if (state == null || state.pos == null) {
            return;
        }

        MotionCamera.captureTarget(state.pos.x, state.pos.y, state.pos.z);

        if (MotionCamera.isActive()) {
            state.pos = MotionCamera.interpolatedPosition(partialTicks);
        }
    }
}
