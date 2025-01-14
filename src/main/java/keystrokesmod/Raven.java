package keystrokesmod;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import keystrokesmod.keystroke.KeySrokeRenderer;
import keystrokesmod.keystroke.KeyStrokeConfigGui;
import keystrokesmod.keystroke.keystrokeCommand;
import keystrokesmod.module.Module;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.script.classes.Entity;
import keystrokesmod.script.classes.NetworkPlayer;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.script.ScriptManager;
import keystrokesmod.utility.*;
import keystrokesmod.utility.command.CommandManager;
import keystrokesmod.utility.profile.Profile;
import keystrokesmod.utility.profile.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Mod(
        modid = "keystrokes",
        name = "KeystrokesMod",
        version = "KMV5",
        acceptedMinecraftVersions = "[1.8.9]"
)
public class Raven {
    public static boolean debug = false;
    public static Minecraft mc = Minecraft.getMinecraft();
    private static KeySrokeRenderer keySrokeRenderer;
    private static boolean isKeyStrokeConfigGuiToggled;
    private static final ScheduledExecutorService ex = Executors.newScheduledThreadPool(2);
    public static ModuleManager moduleManager;
    public static ClickGui clickGui;
    public static ProfileManager profileManager;
    public static ScriptManager scriptManager;
    public static CommandManager commandManager;
    public static Profile currentProfile;
    public static PacketsHandler packetsHandler;
    private static boolean firstLoad;

    public Raven() {
        moduleManager = new ModuleManager();
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        Runtime.getRuntime().addShutdownHook(new Thread(ex::shutdown));
        ClientCommandHandler.instance.registerCommand(new keystrokeCommand());
        FMLCommonHandler.instance().bus().register(this);
        FMLCommonHandler.instance().bus().register(new DebugInfoRenderer());
        FMLCommonHandler.instance().bus().register(new CPSCalculator());
        FMLCommonHandler.instance().bus().register(new MovementFix(mc));
        FMLCommonHandler.instance().bus().register(new KeySrokeRenderer());
        FMLCommonHandler.instance().bus().register(new Ping());
        FMLCommonHandler.instance().bus().register(packetsHandler = new PacketsHandler());
        FMLCommonHandler.instance().bus().register(new ModuleUtils(mc));
        Reflection.getFields();
        Reflection.getMethods();
        moduleManager.register();
        scriptManager = new ScriptManager();
        keySrokeRenderer = new KeySrokeRenderer();
        clickGui = new ClickGui();
        profileManager = new ProfileManager();
        scriptManager.loadScripts();
        profileManager.loadProfiles();
        profileManager.loadProfile("default");
        Reflection.setKeyBindings();
        FMLCommonHandler.instance().bus().register(ModuleManager.scaffold);
        FMLCommonHandler.instance().bus().register(ModuleManager.tower);
        commandManager = new CommandManager();

    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            if (Utils.nullCheck()) {
                if (mc.thePlayer.ticksExisted % 6000 == 0) { // reset cache every 5 minutes
                    Entity.clearCache();
                    NetworkPlayer.clearCache();
                }
                if (Reflection.sendMessage) {
                    Utils.sendMessage("&cThere was an error, relaunch the game.");
                    Reflection.sendMessage = false;
                }
                for (Module module : getModuleManager().getModules()) {
                    if (mc.currentScreen == null && module.canBeEnabled()) {
                        module.keybind();
                    }
                    else if (mc.currentScreen instanceof ClickGui) {
                        module.guiUpdate();
                    }

                    if (module.isEnabled()) {
                        module.onUpdate();
                    }
                }
                if (mc.currentScreen == null) {
                    for (Profile profile : Raven.profileManager.profiles) {
                        profile.getModule().keybind();
                    }
                    for (Module module : Raven.scriptManager.scripts.values()) {
                        module.keybind();
                    }
                }
            }

            if (isKeyStrokeConfigGuiToggled) {
                isKeyStrokeConfigGuiToggled = false;
                mc.displayGuiScreen(new KeyStrokeConfigGui());
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            if (!firstLoad) {
                firstLoad = true;
                scriptManager.loadScripts();
            }
            Entity.clearCache();
            NetworkPlayer.clearCache();
        }
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static ScheduledExecutorService getExecutor() {
        return ex;
    }

    public static KeySrokeRenderer getKeyStrokeRenderer() {
        return keySrokeRenderer;
    }

    public static void toggleKeyStrokeConfigGui() {
        isKeyStrokeConfigGuiToggled = true;
    }
}
