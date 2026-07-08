package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Random;

/**
 * 自定义图腾粒子（TotemParticle）。
 * 仿照 1.12.1 的 TotemParticle 模块逻辑：在不死图腾激活、原版生成金色粒子时，
 * 取消默认粒子并改用自定义颜色（在颜色1/颜色2 之间随机渐变）与自定义速度（水平/垂直分别缩放）的粒子。
 * 现代 MC 中图腾粒子由 Level.addParticle 生成，因此这里拦截该调用而非监听事件。
 */
public class TotemParticle {
    private static final Random RANDOM = new Random();

    public static boolean isActive() {
        return Configs.TOTEM_PARTICLE_ENABLED.getBooleanValue();
    }

    /** 是否应当替换该粒子（仅原版不死图腾粒子）。 */
    public static boolean shouldReplace(ParticleOptions options) {
        return options.getType() == ParticleTypes.TOTEM_OF_UNDYING;
    }

    /** 水平（X/Z）速度缩放系数（1.0 为原版）。 */
    public static double scaleXZ() {
        return Configs.TOTEM_PARTICLE_VELOCITY_XZ.getDoubleValue() / 100.0;
    }

    /** 垂直（Y）速度缩放系数（1.0 为原版）。 */
    public static double scaleY() {
        return Configs.TOTEM_PARTICLE_VELOCITY_Y.getDoubleValue() / 100.0;
    }

    /** 在颜色1与颜色2之间按随机比例渐变，打包成 0xRRGGBB 整数。 */
    public static int fadedColor() {
        Color4f c1 = Configs.TOTEM_PARTICLE_COLOR.getColor();
        Color4f c2 = Configs.TOTEM_PARTICLE_COLOR2.getColor();
        double t = RANDOM.nextDouble();
        int r = lerp(c1.ri, c2.ri, t);
        int g = lerp(c1.gi, c2.gi, t);
        int b = lerp(c1.bi, c2.bi, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, double t) {
        return (int) (a + (b - a) * t);
    }
}
