package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class JumpReset extends Module {
    private SliderSetting chance;

    private ButtonSetting requireMouseDown;
    private ButtonSetting requireMovingForward;
    private ButtonSetting requireAim;

    private boolean setJump;
    private boolean ignoreNext;
    private boolean aiming;
    private int lastHurtTime;
    private double lastFallDistance;

    public JumpReset() {
        super("Jump Reset", category.combat);
        this.registerSetting(chance = new SliderSetting("Chance", "%", 80, 0, 100, 1));
        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(requireMovingForward = new ButtonSetting("Require moving forward", true));
        this.registerSetting(requireAim = new ButtonSetting("Require aim", true));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        int hurtTime = mc.thePlayer.hurtTime;
        boolean onGround = mc.thePlayer.onGround;

        if (onGround && lastFallDistance > 3 && !mc.thePlayer.capabilities.allowFlying) ignoreNext = true;

        if (hurtTime > lastHurtTime) {

            boolean mouseDown = Mouse.isButtonDown(0) || !requireMouseDown.isToggled();
            boolean aimingAt = aiming || !requireAim.isToggled();

            boolean forward = mc.gameSettings.keyBindForward.isKeyDown() || !requireMovingForward.isToggled();

            if (!ignoreNext && onGround && aimingAt && forward && mouseDown && Utils.randomizeDouble(0, 100) < chance.getInput() && !hasBadEffect()) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = true);
                KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
                if (Raven.debug) {
                    Utils.sendModuleMessage(this, "&7jumping enabled");
                }
            }
            ignoreNext = false;
        }

        lastHurtTime = hurtTime;
        lastFallDistance = mc.thePlayer.fallDistance;
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (setJump && !Utils.jumpDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = false);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7jumping disabled");
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook) {
            checkAim(((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getPitch());
        }
        else if (e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            checkAim(((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getPitch());
        }
    }

    private boolean hasBadEffect() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            String name = potionEffect.getEffectName();
            return name.equals("potion.jump") || name.equals("potion.poison") || name.equals("potion.wither");
        }
        return false;
    }

    private void checkAim(float yaw, float pitch) {
        MovingObjectPosition result = RotationUtils.rayTrace(5, ((IAccessorMinecraft) mc).getTimer().renderPartialTicks, new float[] { yaw, pitch }, null);
        aiming = result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && result.entityHit instanceof EntityOtherPlayerMP;
    }
}
