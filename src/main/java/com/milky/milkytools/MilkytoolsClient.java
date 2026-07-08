package com.milky.milkytools;

import com.milky.milkytools.config.ConfigUi;
import com.milky.milkytools.config.Configs;
import com.milky.milkytools.config.HotkeysCallback;
import com.milky.milkytools.config.InputHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

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

        fi.dy.masa.malilib.event.TickHandler.getInstance()
                .registerClientTickHandler(minecraft -> com.milky.milkytools.features.MotionCamera.onTick());
    }
}
