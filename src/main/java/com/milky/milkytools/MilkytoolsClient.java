package com.milky.milkytools;

import com.milky.milkytools.config.ConfigUi;
import com.milky.milkytools.config.Configs;
import com.milky.milkytools.config.HotkeysCallback;
import com.milky.milkytools.config.InputHandler;
import com.milky.milkytools.features.NameTags;
import com.milky.milkytools.features.PearlNameTags;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class MilkytoolsClient implements ClientModInitializer {
    public static final String MOD_ID = "milkytools";
    public static final String MOD_NAME = "Milkytools";
    public static final Minecraft CLIENT = Minecraft.getInstance();

    @Override
    public void onInitializeClient() {
        register();
    }

    private void register() {
        Configs.INSTANCE.load();
        Configs.INSTANCE.initValueChangeCallbacks();
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.INSTANCE);

        ConfigUi.initMalilibConfig();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());

        HotkeysCallback.init();

        // 26.1.2 起 HudElement 的回调是 extractRenderState(GuiGraphicsExtractor, DeltaTracker)，
        // 1.21.11 上是 render(GuiGraphics, DeltaTracker)，因此入口方法按版本二选一。
        //? if >=26.1.2 {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "nametags"),
                NameTags::onHudExtract
        );
        //?} else {
        /*HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "nametags"),
                NameTags::onHudRender
        );*/
        //?}

        // 珍珠名牌。注册在玩家名牌之后，两者重叠时画在玩家名牌上面。
        //? if >=26.1.2 {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "pearl_nametags"),
                PearlNameTags::onHudExtract
        );
        //?} else {
        /*HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "pearl_nametags"),
                PearlNameTags::onHudRender
        );*/
        //?}

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.MotionCamera.onTick());

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.SpawnerBoxes.onClientTick());

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.AmethystBoxes.onClientTick());

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.ObsidianBoxes.onClientTick());

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.CoordinateBeacon.onClientTick());

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.ItemBlacklist.onClientTick());
    }
}
