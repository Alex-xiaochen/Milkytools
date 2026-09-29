package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称手持物品模型调整（ViewModel）：在绘制手持物品的 PoseStack 上叠加平移/缩放/旋转，
 * 只作用于第一人称手持上下文。
 * <p>
 * 26.3 把第一人称手持渲染重做了：{@code ItemInHandRenderer} 更名为
 * {@code FirstPersonHandsAndItemsRenderer}，逐物品的 {@code renderItem} 变成私有的逐手方法
 * {@code submitArmWithItem}（每只手各调用一次，方法内部自带 pushPose/popPose）。
 * 注入它的 HEAD 与旧版注入 {@code renderItem} 的效果一致 —— 只影响对应的那只手。
 * 该方法没有 display context 参数，但只有第一人称渲染器会调用它，过滤条件因此可以省略。
 */
//? if <26.3 {
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
//?} else {
/*@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
*///?}
public class HeldModelMixin {

    //? if <26.3 {
    @Inject(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), require = 1)
    private void milkytools$adjustHeldModel(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                            PoseStack poseStack, SubmitNodeCollector bufferSource, int combinedLight,
                                            CallbackInfo ci) {
        if (displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }
        milkytools$apply(poseStack);
    }
    //?} else {
    /*@Inject(
            method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;"
                    + "Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;"
                    + "FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;F"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), require = 1)
    private void milkytools$adjustHeldModel(
            net.minecraft.client.renderer.state.level.PlayerRenderState playerState,
            net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handsState,
            float f0, float f1, net.minecraft.world.InteractionHand hand, float f2, ItemStack stack, float f3,
            PoseStack poseStack, SubmitNodeCollector bufferSource, int combinedLight, CallbackInfo ci) {
        milkytools$apply(poseStack);
    }*/
    //?}

    /** 按配置在 PoseStack 上叠加平移/缩放/旋转。 */
    @Unique
    private static void milkytools$apply(PoseStack poseStack) {
        if (!Configs.HELD_MODEL_ENABLED.getBooleanValue()) {
            return;
        }

        double scale = Configs.HELD_MODEL_SCALE.getDoubleValue();
        double px = Configs.HELD_MODEL_POS_X.getDoubleValue();
        double py = Configs.HELD_MODEL_POS_Y.getDoubleValue();
        double pz = Configs.HELD_MODEL_POS_Z.getDoubleValue();
        double rx = Math.toRadians(Configs.HELD_MODEL_ROT_X.getDoubleValue());
        double ry = Math.toRadians(Configs.HELD_MODEL_ROT_Y.getDoubleValue());
        double rz = Math.toRadians(Configs.HELD_MODEL_ROT_Z.getDoubleValue());

        if (scale != 1.0) {
            poseStack.scale((float) scale, (float) scale, (float) scale);
        }
        if (rx != 0.0 || ry != 0.0 || rz != 0.0) {
            Quaternionf quaternion = new Quaternionf();
            if (rx != 0.0) quaternion.rotateX((float) rx);
            if (ry != 0.0) quaternion.rotateY((float) ry);
            if (rz != 0.0) quaternion.rotateZ((float) rz);
            // 26.3 移除了 mulPose(Quaternionfc) 重载；Matrix4f 形式在四个版本上都存在，故统一用它。
            poseStack.mulPose(new Matrix4f().rotation(quaternion));
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            poseStack.translate(px, py, pz);
        }
    }
}
