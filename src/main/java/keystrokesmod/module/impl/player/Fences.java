package keystrokesmod.module.impl.player;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Fences extends Module {
    public static SliderSetting mode;
    public String[] modes = new String[]{"Hurt-time", "Sumo"};

    public Fences() {
        super("Fences", category.player, 0);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @Override
    public void onDisable() {
    }

    public static boolean canFence() {
        if (ModuleManager.fences != null && ModuleManager.fences.isEnabled()) {
            if (mode.getInput() == 1 && isSumo() && Utils.distanceToGround(mc.thePlayer) > 3) {
                return true;
            }
        }
        return false;
    }


    private static boolean isSumo() {
        if (Utils.isHypixel()) {
            for (String l : Utils.gsl()) {
                String s = Utils.stripColor(l);
                if (s.startsWith("Map:")) {
                    //if (this.maps.contains(s.substring(5))) {
                    return true;
                    //}
                } else if (s.equals("Mode: Sumo Duel")) {
                    return true;
                }
            }
        }

        return false;
    }
}