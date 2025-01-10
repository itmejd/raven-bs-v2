package keystrokesmod.utility;

import keystrokesmod.Raven;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.client.Minecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.Iterator;
import java.util.Map;

import static net.minecraft.util.EnumFacing.DOWN;

public class ModuleUtils {
    private final Minecraft mc;

    public ModuleUtils(Minecraft mc) {
        this.mc = mc;
    }

    public static boolean isBreaking;
    public static boolean threwFireball;
    private int isBreakingTick;
    public static long MAX_EXPLOSION_DIST_SQ = 9;
    private long FIREBALL_TIMEOUT = 750L, fireballTime = 0;

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C07PacketPlayerDigging) {
            isBreaking = true;
        }

        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball()) {
            if (Utils.keybinds.isMouseDown(1)) {
                fireballTime = System.currentTimeMillis();
                threwFireball = true;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
            LongJump.stopKillAura = false;
            LongJump.stopScaffold = false;
            LongJump.slotResetTicks = 0;
            LongJump.slotReset = false;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            threwFireball = false;
            fireballTime = 0;
            ModuleManager.velocity.disableVelo = false;
        }

        if (isBreaking && ++isBreakingTick >= 1) {
            isBreaking = false;
            isBreakingTick = 0;
        }

        if (ModuleManager.killAura.fixStates) {
            if (!ModuleManager.killAura.isTargeting && ModuleManager.killAura.lag && !ModuleManager.scaffold.isEnabled) {
                if (!Utils.holdingSword() && ModuleManager.killAura.swapped) {
                    PacketUtils.sendPacketNoEvent(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                    ModuleManager.killAura.swapped = false;
                } else {
                    if (Utils.holdingSword()) {
                        PacketUtils.sendPacketNoEvent(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
                        ModuleManager.killAura.swapped = false;
                    }
                }
            }
            else {
                ModuleManager.killAura.swapped = false;
            }
            ModuleManager.killAura.fixStates = false;
            ModuleManager.killAura.lag = false;
        }

        if (ModuleManager.killAura.justUnTargeted) {
            if (++ModuleManager.killAura.unTargetTicks >= 2) {
                ModuleManager.killAura.unTargetTicks = 0;
                ModuleManager.killAura.justUnTargeted = false;
            }
        }


    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        // 7 tick needs to always finish the motion or itll lag back
        if (!ModuleManager.bhop.isEnabled() && ModuleManager.bhop.mode.getInput() == 3 && ModuleManager.bhop.didMove) {
            int simpleY = (int) Math.round((e.posY % 1) * 10000);

            if (mc.thePlayer.hurtTime == 0) {
                switch (simpleY) {
                    case 4200:
                        mc.thePlayer.motionY = 0.39;
                        break;
                    case 1138:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                        ModuleManager.bhop.lowhop = false;
                        ModuleManager.bhop.didMove = false;
                        Utils.print("7 tick");
                        break;
                    /*case 2031:
                        mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                        didMove = false;
                        break;*/
                }
            }
        }

        //Bhop rotate yaw handling
        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= 0 && Utils.isMoving() && ModuleManager.bhop.isEnabled()) {
                ModuleManager.bhop.setRotation = true;
            } else {
                ModuleManager.bhop.setRotation = false;
            }
        }
        if (ModuleManager.bhop.rotateYawOption.isToggled()) {
            if (ModuleManager.bhop.setRotation) {
                if (!ModuleManager.killAura.isTargeting && !Utils.noSlowingBackWithBow()) {
                    float playerYaw = mc.thePlayer.rotationYaw;
                    e.setYaw(playerYaw -= 55);
                }
            }
        }



    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !ModuleManager.scaffold.highlightBlocks.isToggled() || ModuleManager.scaffold.highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }


}