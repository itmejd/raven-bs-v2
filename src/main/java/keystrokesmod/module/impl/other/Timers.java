package keystrokesmod.module.impl.other;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Timers extends Module {

    private ButtonSetting consumables;

    public boolean isEnabled;
    private int a, f1, f2;
    private boolean d1, d2;
    private int consumeTicks = -1, bot = -1;
    private int consumeOffset, bo;

    public Timers() {
        super("Timers", Module.category.other);

        this.registerSetting(consumables = new ButtonSetting("Consumables", true));

        this.alwaysOn = true;
    }


    public void onEnable() {
        isEnabled = true;
    }

    public void onDisable() {
        isEnabled = false;
    }

    @SubscribeEvent
    public void onPacketSent(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && isConsumable()) {
            consumeTicks = 34;
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.thePlayer.isUsingItem() && isConsumable()) {
            if (f1 == 0) {
                add();
                f1 = 1;
                consumeOffset = handleX();
            }
            if (consumeTicks > 1 && mc.thePlayer.ticksExisted % 2 == 0) {
                consumeTicks = consumeTicks - 2;
            }
            d1 = true;
        }
        else if (d1) {
            consumeTicks = -1;
            resetA();
            f1 = 0;
            d1 = false;
        }

        /*if (ModuleManager.bhop.isEnabled()) {
            if (f2 == 0) {
                add();
                f2 = 1;
                bo = handleX();
            }
            if (bot <= 0) {
                bot = 400;
            }
            if (bot > 1 && mc.thePlayer.ticksExisted % 2 == 0) {
                bot = bot - 2;
            }
            d2 = true;
        }
        else if (d2) {
            bot = -1;
            resetA();
            f2 = 0;
            d2 = false;
        }*/
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !isEnabled) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        if (consumeTicks > -1 && consumables.isToggled()) {
            handleTimer(consumeTicks, "6", consumeOffset);
        }
        if (bot > -1) {
            handleTimer(bot, "c", bo);
        }
    }

    //(heldItem instanceof ItemBow && Utils.hasArrows(itemStack)) bow

    private boolean isConsumable() {
        if (mc.thePlayer.getHeldItem() == null) {
            return false;
        }
        Item heldItem = mc.thePlayer.getHeldItem().getItem();
        if (heldItem == null) {
            return false;
        }
        if (heldItem instanceof ItemFood || heldItem instanceof ItemBucketMilk || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()))) {
            return true;
        }
        return false;
    }

    private void add() {
        a++;
    }

    private int handleX() {
        int value = 0;
        if (a == 1) {
            value = 12;
        }
        else if (a == 2) {
            value = 38;
        }
        return value;
    }

    private void resetA() {
        f1 = f2 = 0;
        a = 0;
    }

    private void handleTimer(int ticks, String colorcode, int wo) {
        int color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        double s;
        s = (double) ticks / 20;
        int eo = (s >= 10 && wo == 12) ? 4 : 0;
        int widthOffset = wo;
        String text = ("§" + colorcode + s + "s");
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset + eo, display[1] / 2 + 8, color, true);
    }

}