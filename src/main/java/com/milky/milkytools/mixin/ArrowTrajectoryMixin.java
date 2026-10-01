package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import com.milky.milkytools.features.TrajectoryGizmos;
import com.milky.milkytools.features.TrajectorySimulation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手持弓（按当前拉弓力度）或弩时，按箭矢物理预测其轨迹，并用 Gizmo 绘制连线与落点标记。
 * 物理（含随机散布与水中阻力）见 {@link TrajectorySimulation}。
 * 颜色与珍珠轨迹共用配置项“轨迹颜色”。
 */
@Mixin(DebugRenderer.class)
public class ArrowTrajectoryMixin {
    /** 香草 BowItem 的满蓄力初速度：{@code getPowerForTime(t) * 3.0F}。 */
    private static final double BOW_MAX_SPEED = 3.0;
    /** 香草 CrossbowItem 的箭矢初速度（ARROW_POWER）。 */
    private static final double CROSSBOW_ARROW_SPEED = 3.15;

    @Inject(method = "emitGizmos", at = @At("TAIL"))
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
            // 装填了烟花火箭的弩发射的是 FireworkRocketEntity，加速方式与箭矢完全不同，无法用这套物理预测。
            if (isFireworkLoaded(crossbowStack)) {
                return;
            }
            speed = CROSSBOW_ARROW_SPEED;
        }

        TrajectorySimulation.render(player, speed, TrajectorySimulation.Kind.ARROW, TrajectoryGizmos.colorOf());
    }

    /** 已上弦且装填的是烟花火箭。未上弦时返回 false，此时按上弦后的箭矢速度预览。 */
    private static boolean isFireworkLoaded(ItemStack crossbowStack) {
        ChargedProjectiles charged = crossbowStack.get(DataComponents.CHARGED_PROJECTILES);
        return charged != null && charged.contains(Items.FIREWORK_ROCKET);
    }
}
