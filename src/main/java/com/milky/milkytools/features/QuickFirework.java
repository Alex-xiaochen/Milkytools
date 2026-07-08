package com.milky.milkytools.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;

public class QuickFirework {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    public static boolean accelerated() {
        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            return false;
        }

        // 只在鞘翅飞行时触发。没有飞行时不要吞掉按键，
        // 否则把热键设成右键后会导致无法吃东西/使用物品。
        if (!player.isFallFlying()) {
            return false;
        }

        AbstractContainerMenu menu = player.containerMenu;
        NonNullList<Slot> slots = menu.slots;

        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            ItemStack stack = slots.get(slotIndex).getItem();
            if (!isSafeFirework(stack)) {
                continue;
            }

            return swapToOffhandUseAndSwapBack(menu, slotIndex);
        }

        // 飞行中但没找到安全烟花，也不要吞掉按键。
        return false;
    }

    private static boolean isSafeFirework(ItemStack stack) {
        if (!(stack.getItem() instanceof FireworkRocketItem)) {
            return false;
        }

        // 普通烟花通常没有 explosions；带爆炸效果的烟花会伤人，所以跳过。
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks == null || fireworks.explosions().isEmpty();
    }
    private static boolean swapToOffhandUseAndSwapBack(AbstractContainerMenu menu, int slotIndex) {
        if (CLIENT.player == null || CLIENT.gameMode == null) {
            return false;
        }

        // 40 是副手槽。通过 gameMode 发包点击，避免只改本地菜单状态。
        // Minecraft 26.x 将旧版 ClickType 改成了 ContainerInput，并将方法名改为 handleContainerInput。
        // 这里使用反射，避免静态引用不存在的类导致 NoClassDefFoundError。
        if (!swapWithOffhand(menu.containerId, slotIndex)) {
            return false;
        }

        CLIENT.gameMode.useItem(CLIENT.player, InteractionHand.OFF_HAND);

        swapWithOffhand(menu.containerId, slotIndex);
        return true;
    }

    private static boolean swapWithOffhand(int containerId, int slotIndex) {
        return invoke26ContainerInputSwap(containerId, slotIndex)
                || invokeLegacyClickTypeSwap(containerId, slotIndex);
    }

    private static boolean invoke26ContainerInputSwap(int containerId, int slotIndex) {
        try {
            Class<?> inputClass = Class.forName("net.minecraft.world.inventory.ContainerInput");
            Object swapInput = Enum.valueOf((Class<Enum>) inputClass.asSubclass(Enum.class), "SWAP");

            CLIENT.gameMode.getClass()
                    .getMethod("handleContainerInput", int.class, int.class, int.class, inputClass,
                            net.minecraft.world.entity.player.Player.class)
                    .invoke(CLIENT.gameMode, containerId, slotIndex, 40, swapInput, CLIENT.player);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean invokeLegacyClickTypeSwap(int containerId, int slotIndex) {
        try {
            Class<?> clickTypeClass = Class.forName("net.minecraft.world.inventory.ClickType");
            Object swapClick = Enum.valueOf((Class<Enum>) clickTypeClass.asSubclass(Enum.class), "SWAP");

            CLIENT.gameMode.getClass()
                    .getMethod("handleInventoryMouseClick", int.class, int.class, int.class, clickTypeClass,
                            net.minecraft.world.entity.player.Player.class)
                    .invoke(CLIENT.gameMode, containerId, slotIndex, 40, swapClick, CLIENT.player);
            return true;
        } catch (ReflectiveOperationException | IllegalArgumentException ignored) {
            return false;
        }
    }
}

