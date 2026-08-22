package com.uwdie.stafftools.client;

import com.uwdie.stafftools.client.config.ConfigManager;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.macro.MacroManager;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.ui.StafftoolsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class StafftoolsClient implements ClientModInitializer {

    private static MacroManager macroManager;
    private static ConfigManager configManager;

    private static KeyBinding openStaffToolsKey;

    @Override
    public void onInitializeClient() {

        configManager = new ConfigManager();
        configManager.load();

        Lang.setLanguage(
                configManager.getConfig().getLanguage()
        );

        com.uwdie.stafftools.client.ui.Theme.apply(
                configManager.getConfig().getThemeIndex()
        );

        PunishmentHistory.init(
                FabricLoader.getInstance()
                        .getConfigDir()
                        .resolve(
                                "stafftools_history.json"
                        )
        );

        macroManager = new MacroManager(
                configManager.getConfig(),
                configManager
        );
        macroManager.initialize();

        openStaffToolsKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.stafftools.open",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_RIGHT_SHIFT,
                        "category.stafftools"
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (openStaffToolsKey.wasPressed()) {

                if (client.currentScreen == null) {
                    client.setScreen(
                            new StafftoolsScreen()
                    );
                }
            }
        });
    }

    public static MacroManager getMacroManager() {
        return macroManager;
    }

    public static StaffToolsConfig getConfig() {
        return configManager.getConfig();
    }

    public static void saveConfig() {
        configManager.save();
    }
}
