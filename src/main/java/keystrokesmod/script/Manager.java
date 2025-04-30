package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.Image;
import keystrokesmod.script.model.NetworkPlayer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import org.lwjgl.Sys;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Manager extends Module {
    public static ButtonSetting enableHttpRequests;
    public static ButtonSetting enableWebSockets;

    public final String DOCUMENTATION_URL = "https://blowsy.gitbook.io/raven";
    private final String CONFIG_DIR = mc.mcDataDir + File.separator + "keystrokes" + File.separator + "settings.txt";
    private final String SEPARATOR = ":";
    private final String SEPARATOR_FULL = SEPARATOR + " ";

    private long lastLoad = 0;

    public Manager() {
        super("Manager", category.scripts);
        this.registerSetting(new ButtonSetting("Load scripts", () -> {
            if (Raven.scriptManager.compiler == null) {
                Utils.sendMessage("&cCompiler error, JDK not found");
            }
            else {
                final long currentTimeMillis = System.currentTimeMillis();
                if (Utils.timeBetween(this.lastLoad, currentTimeMillis) > 1500) {
                    this.lastLoad = currentTimeMillis;
                    Raven.scriptManager.loadScripts();
                    if (Raven.scriptManager.scripts.isEmpty()) {
                        Utils.sendMessage("&7No scripts found.");
                    }
                    else {
                        double timeTaken = Utils.round((System.currentTimeMillis() - currentTimeMillis) / 1000.0, 1);
                        Utils.sendMessage("&7Loaded &b" + Raven.scriptManager.scripts.size() + " &7script" + ((Raven.scriptManager.scripts.size() == 1) ? "" : "s") + " in &b" + Utils.asWholeNum(timeTaken) + "&7s.");
                    }
                    Entity.clearCache();
                    NetworkPlayer.clearCache();
                    Image.clearCache();
                    ScriptDefaults.reloadModules();
                    if (Raven.currentProfile != null && Raven.currentProfile.getModule() != null) {
                        ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                    }
                }
                else {
                    Utils.sendMessage("&cYou are on cooldown.");
                }
            }
        }));
        this.registerSetting(new ButtonSetting("Open folder", () -> {
            try {
                Desktop.getDesktop().open(Raven.scriptManager.directory);
            }
            catch (IOException ex) {
                Raven.scriptManager.directory.mkdirs();
                Utils.sendMessage("&cError locating folder, recreated.");
            }
        }));
        this.registerSetting(new ButtonSetting("View documentation", () -> {
            try {
                Desktop.getDesktop().browse(new URI(DOCUMENTATION_URL));
            }
            catch (Throwable t) {
                Sys.openURL(DOCUMENTATION_URL);
            }
        }));
        this.registerSetting(new DescriptionSetting("Privacy"));
        this.registerSetting(enableHttpRequests = new ButtonSetting("Enable http requests", true));
        this.registerSetting(enableWebSockets = new ButtonSetting("Enable websockets", true));
        this.canBeEnabled = false;
        this.ignoreOnSave = true;

        retrieveSettings();
    }

    @Override
    public void guiButtonToggled(ButtonSetting s) {
        updateSettingFile();
    }

    private boolean updateSettingFile() {
        return set("enable-http-requests", String.valueOf(enableHttpRequests.isToggled())) & set("enable-websockets", String.valueOf(enableWebSockets.isToggled()));
    }

    private void ensureConfigFileExists() throws IOException {
        final Path configPath = Paths.get(CONFIG_DIR);
        if (Files.notExists(configPath)) {
            Files.createDirectories(configPath.getParent());
            Files.createFile(configPath);
        }
    }

    private boolean set(String key, String value) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        key = key.replace(SEPARATOR, "");
        final String entry = key + SEPARATOR_FULL + value;
        try {
            ensureConfigFileExists();
            final Path configPath = new File(CONFIG_DIR).toPath();
            final List<String> lines = new ArrayList<>(Files.readAllLines(configPath));
            boolean keyExists = false;
            for (int i = 0; i < lines.size(); ++i) {
                final String line = lines.get(i);
                if (line.startsWith(key + SEPARATOR_FULL)) {
                    lines.set(i, entry);
                    keyExists = true;
                    break;
                }
            }
            if (!keyExists) {
                lines.add(entry);
            }
            Files.write(configPath, lines);
            return true;
        }
        catch (IOException ex) {
            return false;
        }
    }

    private void retrieveSettings() {
        String requestState = retrieveSetting("enable-http-requests");
        String webSocketsState = retrieveSetting("enable-websockets");
        if (requestState != null) {
            enableHttpRequests.setEnabled(parseBoolean(requestState, true));
        }
        if (webSocketsState != null) {
            enableWebSockets.setEnabled(parseBoolean(webSocketsState, true));
        }
    }

    private boolean parseBoolean(String parse, boolean defaultVal) {
        try {
            return Boolean.parseBoolean(parse);
        }
        catch (Exception e) {
            return defaultVal;
        }
    }

    private String retrieveSetting(String key) {
        try {
            ensureConfigFileExists();
            final Path configPath = new File(CONFIG_DIR).toPath();
            final List<String> lines = Files.readAllLines(configPath);
            for (final String line : lines) {
                if (line.startsWith(key + SEPARATOR_FULL)) {
                    return line.substring((key + SEPARATOR_FULL).length());
                }
            }
        }
        catch (IOException ex) {}
        return null;
    }
}