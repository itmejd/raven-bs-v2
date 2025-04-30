package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;

public class WTap extends Module {
    private SliderSetting delay;
    private SliderSetting hurttime;
    private SliderSetting chance;
    private ButtonSetting playersOnly;

    private final HashMap<Integer, Long> hits = new HashMap<>();
    public static boolean stopSprint = false;

    public WTap() {
        super("WTap", category.combat);
        this.registerSetting(delay = new SliderSetting("Delay", "ms", 200, 0, 1000, 50));
        this.registerSetting(hurttime = new SliderSetting("Hurttime", 0, 0, 10, 1));
        this.registerSetting(chance = new SliderSetting("Chance", "%", 100, 0, 100, 1));
        this.registerSetting(playersOnly = new ButtonSetting("Players only", true));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!Utils.nullCheck() || event.entityPlayer != mc.thePlayer || !mc.thePlayer.isSprinting()) {
            return;
        }
        if (chance.getInput() == 0) {
            return;
        }
        if (playersOnly.isToggled()) {
            if (!(event.target instanceof EntityPlayer)) {
                return;
            }
            if (AntiBot.isBot(event.target)) {
                return;
            }
        }
        else if (!(event.target instanceof EntityLivingBase)) {
            return;
        }
        if (((EntityLivingBase)event.target).deathTime != 0) {
            return;
        }
        if (((EntityLivingBase) event.target).hurtTime > hurttime.getInput()) {
            return;
        }
        long currentMs = System.currentTimeMillis();
        Long lastHit = this.hits.get(event.target.getEntityId());
        if (lastHit != null && Utils.timeBetween(lastHit, currentMs) <= (long) delay.getInput()) {
            return;
        }
        if (chance.getInput() != 100.0D) {
            double ch = Math.random();
            if (ch >= chance.getInput() / 100.0D) {
                return;
            }
        }
        this.hits.put(event.target.getEntityId(), currentMs);
        stopSprint = true;
    }

    public void onDisable() {
        stopSprint = false;
        this.hits.clear();
    }
}