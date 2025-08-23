package keystrokesmod.utility;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayer;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

public class ReflectionUtils {

    public static Field button;
    public static Field buttonstate;
    public static Field buttons;

    public static HashMap<Class, Field> containerInventoryPlayer = new HashMap<>();

    private static List<Class> containerClasses = Arrays.asList(GuiFurnace.class, GuiBrewingStand.class, GuiEnchantment.class, ContainerHopper.class, GuiDispenser.class, ContainerWorkbench.class, ContainerMerchant.class, ContainerHorseInventory.class);

    public static boolean ERROR = false; // Set to true to alert the player when reflection fails

    public static Map<String, KeyBinding> keybinds = new HashMap<>();

    public static void setupFields() {
        try {
            button = MouseEvent.class.getDeclaredField("button");
            buttonstate = MouseEvent.class.getDeclaredField("buttonstate");
            buttons = Mouse.class.getDeclaredField("buttons");
            for (Class clazz : containerClasses) {
                for (Field field : clazz.getDeclaredFields()) {
                    addToMap(clazz, field);
                }
            }
        }
        catch (Exception var2) {
            System.out.println("There was an error, relaunch the game.");
            var2.printStackTrace();
            ERROR = true;
        }
    }

    public static void setKeyBindings() {
        for (KeyBinding keyBind : Minecraft.getMinecraft().gameSettings.keyBindings) {
            String keyName = keyBind.getKeyDescription().replaceFirst("key\\.", "");
            keybinds.put(keyName, keyBind);
        }
    }

    public static void setButton(int button, boolean state) {
        if (ReflectionUtils.button != null && buttonstate != null && buttons != null) {
            MouseEvent m = new MouseEvent();
            try {
                ReflectionUtils.button.setAccessible(true);
                ReflectionUtils.button.set(m, button);
                buttonstate.setAccessible(true);
                buttonstate.set(m, state);
                MinecraftForge.EVENT_BUS.post(m);
                buttons.setAccessible(true);
                ByteBuffer bf = (ByteBuffer) buttons.get(null);
                buttons.setAccessible(false);
                bf.put(button, (byte) (state ? 1 : 0));
            }
            catch (IllegalAccessException var4) {
            }
        }
    }

    private static void addToMap(Class clazz, Field field) {
        if (field == null || field.getType() != IInventory.class) {
            return;
        }
        field = ReflectionHelper.findField(clazz, field.getName());
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        containerInventoryPlayer.put(clazz, field);
    }

    public static boolean setItemInUse(boolean blocking) {
        if (!blocking && (ModuleManager.killAura.blockingClient || ModuleManager.noSlow.blockingClient)) {
            return blocking;
        }
        ((IAccessorEntityPlayer) Minecraft.getMinecraft().thePlayer).setItemInUseCount(blocking ? 999999999 : -1);
        return blocking;
    }

}