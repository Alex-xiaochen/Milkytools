package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
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
 * 手持弓（按当前拉弓力度）或已上弦的弩时，按箭矢物理预测其轨迹，并用 Gizmo 绘制连线与落点标记。
 * 物理与香草 AbstractArrow 一致：初速度由拉弓力度/弩决定，重力 0.05，空气阻力每 tick 0.99。
 * 颜色与珍珠轨迹共用配置项“轨迹颜色”。
 */
@Mixin(DebugRenderer.class)
public class ArrowTrajectoryMixin {
    private static final double GRAVITY = 0.05;
    private static final double AIR_DRAG = 0.99;
    private static final double BOW_MAX_SPEED = 3.0;
    private static final double CROSSBOW_SPEED = 3.15;
    private static final double CROSSBOW_UNCHARGED_SPEED = 1.6;
    private static final int MAX_TICKS = 200;

    @Inject(method = "emitGizmos", at = @At("TAIL"), require = 0)
    private void milkytools$drawArrowTrajectory(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
        if (!Configs.ARROW_TRAJECTORY.getBooleanValue()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean bow = mainHand.getItem() instanceof BowItem || offHand.getItem() instanceof BowItem;
        boolean crossbow = mainHand.getItem() instanceof CrossbowItem || offHand.getItem() instanceof CrossbowItem;
        if (!bow && !crossbow) {
            return;
        }

        double speed;
        if (bow) {
            float power = 1.0F;
            if (player.isUsingItem()) {
                ItemStack using = player.getUseItem();
                if (using.getItem() instanceof BowItem) {
                    power = BowItem.getPowerForTime(player.getTicksUsingItem());
                }
            }
            speed = power * BOW_MAX_SPEED;
        } else {
            ItemStack crossbowStack = mainHand.getItem() instanceof CrossbowItem ? mainHand : offHand;
            speed = CrossbowItem.isCharged(crossbowStack) ? CROSSBOW_SPEED : CROSSBOW_UNCHARGED_SPEED;
        }

        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        int color = colorOf();

        Vec3 position = eye;
        Vec3 velocity = look.scale(speed);
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
            velocity = velocity.subtract(0.0, GRAVITY, 0.0).scale(AIR_DRAG);
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
        int rgb = (color.ri << 16) | (color.gi << 8) | color.bi;
        return 0xFF000000 | rgb;
    }
}
