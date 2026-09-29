package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import net.minecraft.world.phys.Vec3;

/**
 * 视线箭头延长（ViewArrow）。
 * 原版 F3+B 调试渲染会从实体眼睛位置沿其朝向 {@code getViewVector()} 画一条 2 格长的蓝色箭头，
 * 本功能通过重定向该视线向量的缩放长度，把它改为“视线箭头长度”配置的格数。
 * 仅在开启实体碰撞箱调试显示（F3+B）时可见，纯客户端渲染，不影响任何游戏逻辑。
 */
public final class ViewArrow {
    private ViewArrow() {
    }

    /**
     * 计算蓝色视线箭头的方向向量。
     *
     * @param viewVector   实体在当前帧的朝向单位向量（已含插值）
     * @param vanillaScale 原版使用的缩放长度（2.0），关闭功能时原样返回
     * @return 缩放后的视线向量
     */
    public static Vec3 scale(Vec3 viewVector, double vanillaScale) {
        if (!Configs.VIEW_ARROW_ENABLED.getBooleanValue()) {
            return viewVector.scale(vanillaScale);
        }

        return viewVector.scale(Configs.VIEW_ARROW_LENGTH.getDoubleValue());
    }
}
