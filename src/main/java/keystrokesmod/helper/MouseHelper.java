package keystrokesmod.helper;

import keystrokesmod.Raven;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class MouseHelper {
    private static Minecraft mc = Minecraft.getMinecraft();

    private static List<Long> a = new ArrayList();
    private static List<Long> b = new ArrayList();

    public static long LL = 0L;
    public static long LR = 0L;

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!e.buttonstate) {
            return;
        }
        if (e.button == 0) {
            aL();
            if (Raven.debug && mc.objectMouseOver != null) {
                Entity en = mc.objectMouseOver.entityHit;
                if (en == null || !(en instanceof EntityLivingBase)) {
                    return;
                }

                Utils.printInfo((EntityLivingBase) en);
            }
        } else if (e.button == 1) {
            aR();
        }
        else if (e.button == 2 && Settings.middleClickFriends.isToggled()) {
            EntityLivingBase g = Utils.raytrace(200);
            if (g != null && !AntiBot.isBot(g) && !Utils.addFriend(g.getName())) {
                Utils.removeFriend(g.getName());
            }
        }
    }

    public static void aL() {
        a.add(LL = System.currentTimeMillis());
    }

    public static void aR() {
        b.add(LR = System.currentTimeMillis());
    }

    public static int f() {
        a.removeIf(o -> o < System.currentTimeMillis() - 1000L);
        return a.size();
    }

    public static int i() {
        b.removeIf(o -> o < System.currentTimeMillis() - 1000L);
        return b.size();
    }
}
