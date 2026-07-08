package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 客户端侧"快捷盒子"状态与集成桥。 */
public class QuickShulkerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger("Milkytools/QuickShulkerSupport");
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final String MOD_ID = "quickshulker";

    private static boolean switchingItem = false;
    private static Class<?> utilClass;
    private static Class<?> clientUtilClass;
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;

    public static boolean isSwitchingItem() { return switchingItem && isAvailable(); }
    public static void switchFromOpenedShulker() { switchingItem = false; }
    public static void reset() { switchingItem = false; }

    public static void onLitematicaPickBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isAvailable() || !Configs.QUICK_SHULKER.getBooleanValue()) return;
        int slot = findShulkerSlot(stack);
        if (slot >= 0) openShulker(slot);
        else LOGGER.debug("No suitable shulker found for item: {}", stack.getDisplayName().getString());
    }

    private static boolean isAvailable() {
        if (reflectionFailed) return false;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) { LOGGER.debug("QuickShulker mod not loaded"); return false; }
        initializeReflection();
        return !reflectionFailed;
    }

    private static synchronized void initializeReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try { utilClass = Class.forName("net.kyrptonaught.quickshulker.api.Util"); LOGGER.debug("Found QuickShulker Util class: {}", utilClass.getName()); }
        catch (ClassNotFoundException e) { LOGGER.warn("QuickShulker Util class not found, API may have changed: {}", e.getMessage()); reflectionFailed = true; return; }
        try { clientUtilClass = Class.forName("net.kyrptonaught.quickshulker.client.ClientUtil"); LOGGER.debug("Found QuickShulker ClientUtil class: {}", clientUtilClass.getName()); }
        catch (ClassNotFoundException e) { LOGGER.warn("QuickShulker ClientUtil class not found: {}", e.getMessage()); }
    }

    private static int findShulkerSlot(ItemStack target) {
        var inventory = CLIENT.player != null ? CLIENT.player.getInventory() : null;
        if (inventory == null) return -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !isOpenableItem(stack)) continue;
            if (containsItem(stack, target)) { LOGGER.debug("Found matching shulker at slot {}: {}", slot, stack.getDisplayName().getString()); return slot; }
        }
        return -1;
    }

    private static boolean containsItem(ItemStack shulker, ItemStack target) {
        try {
            Object container = callUtil("getQuickItemInventory", new Class[]{net.minecraft.world.entity.player.Player.class, ItemStack.class}, CLIENT.player, shulker);
            if (container instanceof net.minecraft.world.Container c) {
                for (int i = 0; i < c.getContainerSize(); i++) if (itemsEqualExactly(c.getItem(i), target)) return true;
            }
        } catch (Throwable t) { LOGGER.warn("Failed to check shulker contents via API: {}", t.getMessage()); }
        return false;
    }

    private static boolean isOpenableItem(ItemStack stack) {
        Object result = callUtil("isOpenableItem", new Class[]{ItemStack.class}, stack);
        if (result instanceof Boolean) return (Boolean) result;
        if (result == null) LOGGER.trace("isOpenableItem returned null for stack: {}", stack.getItem());
        return false;
    }

    private static boolean itemsEqualExactly(ItemStack a, ItemStack b) {
        Object result = callUtil("areItemsEqualExactly", new Class[]{ItemStack.class, ItemStack.class}, a, b);
        if (result instanceof Boolean) return (Boolean) result;
        if (result == null) LOGGER.trace("itemsEqualExactly returned null for stacks: {} vs {}", a.getItem(), b.getItem());
        return false;
    }

    private static void openShulker(int slot) {
        if (clientUtilClass == null) { LOGGER.warn("ClientUtil class not available, cannot open shulker"); return; }
        try {
            try { invoke(clientUtilClass, "openItem", new Class[]{net.minecraft.world.entity.player.Player.class, int.class}, null, CLIENT.player, slot); LOGGER.debug("Opened shulker at slot {} via openItem", slot); return; }
            catch (NoSuchMethodException ignored) {} catch (Throwable t) { LOGGER.warn("openItem call failed: {}", t.getMessage()); }
            invoke(clientUtilClass, "openShulker", new Class[]{int.class}, null, slot);
            LOGGER.debug("Opened shulker at slot {} via openShulker", slot);
        } catch (Throwable t) { LOGGER.error("Failed to open shulker at slot {}: {}", slot, t.getMessage(), t); }
    }

    private static Object callUtil(String method, Class<?>[] types, Object... args) {
        if (utilClass == null) return null;
        try { return utilClass.getMethod(method, types).invoke(null, args); }
        catch (NoSuchMethodException e) { LOGGER.warn("Util method not found: {} - API may have changed", method); return null; }
        catch (Throwable t) { LOGGER.warn("Util method '{}' call failed: {}", method, t.getMessage()); return null; }
    }

    private static void invoke(Class<?> clazz, String method, Class<?>[] types, Object instance, Object... args) throws NoSuchMethodException {
        try { clazz.getMethod(method, types).invoke(instance, args); }
        catch (NoSuchMethodException e) { throw e; } catch (Throwable t) { LOGGER.warn("Failed to invoke {}.{}: {}", clazz.getSimpleName(), method, t.getMessage()); }
    }
}
