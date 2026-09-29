package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if >=26.1.2 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;*/
//?}
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 物品黑名单：背包中出现名单上的物品时自动丢弃。
 * 通过客户端容器点击（THROW）发包，效果与服务端一致。
 */
public class ItemBlacklist {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    // 扫描间隔（tick）。每隔一段时间扫描一次，避免每 tick 发包。
    private static final int SCAN_INTERVAL = 5;

    private static int tickCounter = 0;

    public static void onClientTick() {
        if (!Configs.ITEM_BLACKLIST_ENABLED.getBooleanValue()) {
            return;
        }

        LocalPlayer player = CLIENT.player;
        if (player == null || CLIENT.gameMode == null) {
            return;
        }

        // 打开其他容器时菜单槽位不是玩家背包，跳过以免丢错物品。
        if (player.containerMenu != player.inventoryMenu) {
            return;
        }

        if (++tickCounter < SCAN_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Set<String> blacklist = getBlacklist();
        if (blacklist.isEmpty()) {
            return;
        }

        AbstractContainerMenu menu = player.inventoryMenu;

        // InventoryMenu 中 9 起为背包/快捷栏/副手槽，0~8 是合成与护甲槽。
        // 一次扫描把所有命中的槽位全部丢出，而不是每次只丢一组。
        for (int slot = InventoryMenu.INV_SLOT_START; slot < menu.slots.size(); slot++) {
            Slot menuSlot = menu.slots.get(slot);
            ItemStack stack = menuSlot.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && blacklist.contains(id.toString())) {
                dropSlot(menu.containerId, slot);
            }
        }
    }

    private static void dropSlot(int containerId, int slot) {
        if (CLIENT.player == null || CLIENT.gameMode == null) {
            return;
        }

        // 按钮 1 表示丢弃整组，0 表示只丢一个。
        // 26.1.2 起 ClickType 更名为 ContainerInput，方法名也随之改变。
        //? if >=26.1.2 {
        CLIENT.gameMode.handleContainerInput(containerId, slot, 1, ContainerInput.THROW, CLIENT.player);
        //?} else {
        /*CLIENT.gameMode.handleInventoryMouseClick(containerId, slot, 1, ClickType.THROW, CLIENT.player);*/
        //?}
    }

    private static Set<String> getBlacklist() {
        List<String> configured = Configs.ITEM_BLACKLIST_ITEMS.getStrings();
        Set<String> ids = new HashSet<>();

        for (String raw : configured) {
            if (raw == null) {
                continue;
            }

            String value = raw.trim();
            if (value.isEmpty()) {
                continue;
            }

            // 允许省略命名空间，默认补 minecraft:。
            if (!value.contains(":")) {
                value = "minecraft:" + value;
            }

            ids.add(value);
        }

        return ids;
    }
}
