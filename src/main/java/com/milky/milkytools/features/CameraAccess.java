package com.milky.milkytools.features;

import net.minecraft.client.Minecraft;
//? if >=26.1.2 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;*/
//?}

/**
 * 取当前帧的相机渲染状态，屏蔽各版本不同的访问路径。
 * <p>
 * 入口与包路径都随版本变化：
 * <ul>
 *   <li>26.2 起：{@code gameRenderer.gameRenderState()}（record 风格访问器）</li>
 *   <li>26.1.2：{@code gameRenderer.getGameRenderState()}（JavaBean 风格）</li>
 *   <li>1.21.11：还没有 GameRenderState 这一层，直接 {@code gameRenderer.getLevelRenderState()}</li>
 * </ul>
 * 另外 1.21.11 的 {@code CameraRenderState} 在 {@code client.renderer.state}（没有 {@code .level}），
 * 且只有 pos/orientation 等字段，没有投影矩阵。
 */
public final class CameraAccess {
    private CameraAccess() {
    }

    /** 当前帧的相机渲染状态。 */
    public static CameraRenderState camera() {
        //? if >=26.2 {
        return Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        //?} else if >=26.1.2 {
        /*return Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;*/
        //?} else {
        /*return Minecraft.getInstance().gameRenderer.getLevelRenderState().cameraRenderState;*/
        //?}
    }
}
