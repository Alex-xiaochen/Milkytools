package com.milky.milkytools.config;

import com.milky.milkytools.MilkytoolsClient;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import java.util.List;

public class ConfigUi extends GuiConfigsBase {
    private static ConfigGuiTab currentTab = ConfigGuiTab.HOTKEYS;

    public ConfigUi() {
        super(10, 50, MilkytoolsClient.MOD_ID, null, MilkytoolsClient.MOD_NAME, MilkytoolsClient.MOD_NAME);
    }

    public static void initMalilibConfig() {
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(MilkytoolsClient.MOD_ID, MilkytoolsClient.MOD_NAME, ConfigUi::new)
        );
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab);
        }
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.displayName);
        button.setEnabled(ConfigUi.currentTab != tab);
        this.addButton(button, (buttonBase, mouseButton) -> {
            ConfigUi.currentTab = tab;
            this.setActiveKeybindButton(null);
            this.initGui();
        });

        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;

        if (ConfigUi.currentTab == ConfigGuiTab.HOTKEYS) {
            configs = Configs.KEY_LIST;
        } else {
            configs = Configs.ALL_CONFIGS;
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    protected void onSettingsChanged() {
        super.onSettingsChanged();
        Configs.INSTANCE.save();
    }

    @Override
    public void removed() {
        Configs.INSTANCE.save();
        super.removed();
    }

    private enum ConfigGuiTab {
        HOTKEYS("工具热键"),
        ALL("全部工具");

        private final String displayName;

        ConfigGuiTab(String displayName) {
            this.displayName = displayName;
        }
    }
}
