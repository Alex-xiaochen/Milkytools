package com.milky.milkytools.mixin.masa;

import com.milky.milkytools.features.QuickShulkerSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Litematica 投影中键取方块后，如果身上没有对应物品，则尝试从快捷盒子打开潜影盒并取出。
 *
 * 使用字符串 targets，避免 Milkytools 在编译期/无 Litematica 环境下直接加载 Litematica 类。
 */
@Mixin(targets = "fi.dy.masa.litematica.util.InventoryUtils", remap = false)
public class MixinLitematicaInventoryUtils {
    @Inject(method = "schematicWorldPickBlock", at = @At("TAIL"), require = 0)
    private static void milkytools$schematicWorldPickBlock(ItemStack stack, BlockPos pos, Level schematicWorld, Minecraft mc, CallbackInfo ci) {
        QuickShulkerSupport.onLitematicaPickBlock(stack);
    }
}
