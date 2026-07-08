package com.milky.milkytools.config;

import com.milky.milkytools.MilkytoolsClient;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;

// 注册 MaLiLib 热键。没被 addKeybindToMap 的热键，按下时不会被识别。
public class InputHandler implements IKeybindProvider, IKeyboardInputHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Configs.KEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(MilkytoolsClient.MOD_ID, "按下式", Configs.KEY_LIST);
    }

    public static InputHandler getInstance() {
        return INSTANCE;
    }
}
