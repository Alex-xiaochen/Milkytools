package com.milky.milkytools;

import com.milky.milkytools.config.ConfigUi;
import com.milky.milkytools.config.Configs;
import com.milky.milkytools.config.HotkeysCallback;
import com.milky.milkytools.config.InputHandler;
import com.milky.milkytools.features.NameTags;
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
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.INSTANCE);

        ConfigUi.initMalilibConfig();

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());

        HotkeysCallback.init();

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "nametags"),
                NameTags::onHudExtract
        );

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
    }
}
