package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手持末影珍珠时，按投掷物理预测其轨迹，并用 Gizmo 绘制连线与落点标记。
 * 颜色取自配置项“珍珠轨迹颜色”。
 */
@Mixin(DebugRenderer.class)
public class PearlTrajectoryMixin {
    // 末影珍珠初速度约 1.5 格/tick，重力 0.03 格/tick²（与 ThrownEnderpearl / Projectile 一致）。
    private static final double THROW_SPEED = 1.5;
    private static final double GRAVITY = 0.03;
    private static final int MAX_TICKS = 200;

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawPearlTrajectory(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
        if (!Configs.PEARL_TRAJECTORY.getBooleanValue()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (!(mainHand.getItem() instanceof EnderpearlItem) && !(offHand.getItem() instanceof EnderpearlItem)) {
            return;
        }

        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int color = colorOf();

        Vec3 position = eye;
        Vec3 velocity = look.scale(THROW_SPEED);
        Vec3 previous = position;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            Vec3 next = position.add(velocity);
            ClipContext context = new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            HitResult hit = level.clip(context);

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                drawSegment(previous, hit.getLocation(), color);
                Gizmos.point(hit.getLocation(), color, 0.25F);
                break;
            }

            drawSegment(previous, next, color);
            previous = next;
            position = next;
            velocity = velocity.subtract(0.0, GRAVITY, 0.0);
        }
    }

    private static void drawSegment(Vec3 from, Vec3 to, int color) {
        if (from.distanceToSqr(to) < 1.0E-7) {
            return;
        }
        Gizmos.line(from, to, color);
    }

    private static int colorOf() {
        Color4f color = Configs.PEARL_TRAJECTORY_COLOR.getColor();
        // Gizmo 颜色按 ARGB 解释，必须把 alpha 置为不透明，否则线条完全透明不可见。
        int rgb = (color.ri << 16) | (color.gi << 8) | color.bi;
        return 0xFF000000 | rgb;
    }
}
