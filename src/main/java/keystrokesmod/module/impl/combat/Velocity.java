package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;

public class Velocity extends Module {
    public SliderSetting mode;
    public static SliderSetting vertical, horizontal, reverseHorizontal, explosionsHorizontal, explosionsVertical, verticalM;
    public static SliderSetting minExtraSpeed, extraSpeedBoost;
    private SliderSetting chance;
    private ButtonSetting onlyWhileAttacking;
    private ButtonSetting onlyWhileTargeting;
    private ButtonSetting onlyWhileSwinging;
    private ButtonSetting disableS;
    private ButtonSetting zzWhileNotTargeting, delayPacket;
    public ButtonSetting allowSelfFireball;
    public static ButtonSetting reverseDebug;
    private KeySetting switchToReverse, switchToPacket;
    private ButtonSetting requireMouseDown;
    private ButtonSetting requireMovingForward;
    private ButtonSetting requireAim;
    private ButtonSetting disableLobby;
    private boolean stopFBvelo;
    public boolean disableVelo;
    private boolean buttonDown, pDown, rDown;
    private boolean setJump;
    private boolean ignoreNext;
    private boolean aiming;
    private int lastHurtTime;
    private double lastFallDistance;

    public boolean blink;
    private int delayTicks;

    private boolean exp, canJump;

    private int db, ovd;
    private boolean t1;
    private int t2;
    private boolean fb;

    //delay velo
    private List<Map<String, Object>> packets = new ArrayList<>();
    private SliderSetting delay;
    private ButtonSetting optimizeAB;
    private boolean delaying, conditionals;
    public boolean optimize;

    private String[] modes = new String[] { "Normal", "Packet", "Reverse", "Jump", "Delay", "Buffer" };


    public Velocity() {
        super("Velocity", category.combat);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));


        this.registerSetting(delay = new SliderSetting("Maximuim Delay", "ms", 400, 0, 1000, 50));
        this.registerSetting(optimizeAB = new ButtonSetting("Optimize autoblock", false));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(verticalM = new SliderSetting("Vertical Motion Limit", 1.0, -1.0, 1, 0.1));
        this.registerSetting(reverseHorizontal = new SliderSetting("-Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(explosionsHorizontal = new SliderSetting("Horizontal (Explosions)", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(explosionsVertical = new SliderSetting("Vertical (Explosions)", 0.0, 0.0, 100.0, 1.0));



        this.registerSetting(minExtraSpeed = new SliderSetting("Maximum speed for extra boost", 0, 0, 0.7, 0.01));
        this.registerSetting(extraSpeedBoost = new SliderSetting("Extra speed boost multiplier", "%", 0, 0, 100, 1));
        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(onlyWhileAttacking = new ButtonSetting("Only while attacking", false));
        this.registerSetting(onlyWhileSwinging = new ButtonSetting("Only while swinging", false));
        this.registerSetting(onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(disableS = new ButtonSetting("Disable while holding S", false));
        this.registerSetting(zzWhileNotTargeting = new ButtonSetting("00 while not targeting", false));
        this.registerSetting(allowSelfFireball = new ButtonSetting("Allow self fireball", false));
        this.registerSetting(switchToReverse = new KeySetting("Switch to reverse", Keyboard.KEY_SPACE));
        this.registerSetting(switchToPacket = new KeySetting("Switch to packet", Keyboard.KEY_SPACE));
        this.registerSetting(reverseDebug = new ButtonSetting("Show reverse debug messages", false));
        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(requireMovingForward = new ButtonSetting("Require moving forward", false));
        this.registerSetting(requireAim = new ButtonSetting("Require aim", false));
        this.registerSetting(disableLobby = new ButtonSetting("Disable in lobby", false));
    }

    public void guiUpdate() {
        this.delay.setVisible(mode.getInput() == 4 || mode.getInput() == 5, this);
        this.optimizeAB.setVisible(mode.getInput() == 4 || mode.getInput() == 5, this);
        this.onlyWhileAttacking.setVisible(mode.getInput() == 0, this);
        this.onlyWhileSwinging.setVisible(mode.getInput() == 0, this);
        this.onlyWhileTargeting.setVisible(mode.getInput() == 0, this);
        this.disableS.setVisible(mode.getInput() == 0, this);

        this.allowSelfFireball.setVisible(mode.getInput() == 1, this);
        this.zzWhileNotTargeting.setVisible(mode.getInput() == 1, this);

        this.switchToReverse.setVisible(mode.getInput() == 1, this);
        this.switchToPacket.setVisible(mode.getInput() == 2, this);



        this.horizontal.setVisible(mode.getInput() != 2 && mode.getInput() != 3 && mode.getInput() != 4 && mode.getInput() != 5, this);
        this.vertical.setVisible(mode.getInput() != 2 && mode.getInput() != 3 && mode.getInput() != 4 && mode.getInput() != 5, this);
        this.verticalM.setVisible(mode.getInput() == 1, this);
        this.chance.setVisible(mode.getInput() != 2 && mode.getInput() != 4 && mode.getInput() != 5, this);
        this.reverseHorizontal.setVisible(mode.getInput() == 2, this);

        this.explosionsHorizontal.setVisible(mode.getInput() != 0 && mode.getInput() != 3 && mode.getInput() != 4 && mode.getInput() != 5, this);
        this.explosionsVertical.setVisible(mode.getInput() != 0 && mode.getInput() != 3 && mode.getInput() != 4 && mode.getInput() != 5, this);

        this.minExtraSpeed.setVisible(mode.getInput() == 2, this);
        this.extraSpeedBoost.setVisible(mode.getInput() == 2, this);
        this.reverseDebug.setVisible(mode.getInput() == 2, this);

        this.requireMouseDown.setVisible(mode.getInput() == 3, this);
        this.requireMovingForward.setVisible(mode.getInput() == 3, this);
        this.requireAim.setVisible(mode.getInput() == 3, this);
    }

    @Override
    public String getInfo() {
        String name = "";
        if (mode.getInput() == 0 || mode.getInput() == 1) {
            name = (int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%";
        }
        if (mode.getInput() == 2) {
            name = "-" + (int) reverseHorizontal.getInput()+ "%";
        }
        if (mode.getInput() == 3 || mode.getInput() == 4 || mode.getInput() == 5) {
            name = modes[(int) mode.getInput()];
        }
        return name;
    }

    @Override
    public void onDisable() {
        blink = false;
        delayTicks = 0;
        stopFBvelo = disableVelo = false;
        buttonDown = pDown = rDown = false;
        setJump = ignoreNext = aiming = false;
        lastHurtTime = 0;
        lastFallDistance = 0;
        db = 0;
        exp = false;
        canJump = false;
        flushAll();
        t1 = false;
        if (t2 != 0) {
            Utils.resetTimer();
        }
        t2 = 0;
        fb = false;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {

        if (Utils.tabbedIn()) {
            if (switchToReverse.isPressed() && mode.getInput() == 1 && !buttonDown) {
                mode.setValue(2);
                buttonDown = true;
                Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bReverse&7 Velocity mode"));
            }
            if (switchToPacket.isPressed() && mode.getInput() == 2 && !buttonDown) {
                mode.setValue(1);
                buttonDown = true;
                Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bPacket&7 Velocity mode"));
            }
        }
        if (switchToReverse.isPressed() || switchToPacket.isPressed()) {
            buttonDown = true;
        }
        else {
            buttonDown = false;
        }

        if (db > 0) {
            db--;
        }

        int hurtTime = mc.thePlayer.hurtTime;
        boolean onGround = mc.thePlayer.onGround;

        if (mc.thePlayer.hurtTime > 0 && mode.getInput() == 4) {
            KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
        }

        if (onGround && lastFallDistance > 3 && !mc.thePlayer.capabilities.allowFlying) ignoreNext = true;
        if (hurtTime > lastHurtTime) {

            boolean mouseDown = Mouse.isButtonDown(0) || !requireMouseDown.isToggled();
            boolean aimingAt = aiming || !requireAim.isToggled();

            boolean forward = mc.gameSettings.keyBindForward.isKeyDown() || !requireMovingForward.isToggled();

            handlejr(onGround, aimingAt, forward, mouseDown);
            ignoreNext = false;
        }

        lastHurtTime = hurtTime;
        lastFallDistance = mc.thePlayer.fallDistance;
    }

    private void handlejr(boolean onGround, boolean aimingAt, boolean forward, boolean mouseDown) {
        if (mode.getInput() != 3) {
            return;
        }
        if (disableLobby.isToggled() && Utils.isLobby()) {
            return;
        }
        if (db > 0) {
            return;
        }
        if (!ignoreNext && onGround && aimingAt && forward && mouseDown && Utils.randomizeDouble(0, 100) < chance.getInput() && !hasBadEffect()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = true);
            KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7jumping enabled");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostMotion(PostMotionEvent e) {

        if (setJump && !Utils.jumpDown()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), setJump = false);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7jumping disabled");
            }
        }

        conditionals = conditionals();


        if (delaying && !packets.isEmpty() && (mode.getInput() == 4 || mode.getInput() == 5)) {
            long now = Utils.time();
            long delayv = (long) delay.getInput();

            int v3v = 300; //ms
            if (++ovd >= (v3v / 50)) {
                optimize = (optimizeAB.isToggled());
            }

            if (mode.getInput() == 5) {
                if (ovd >= (delay.getInput() / 50)) {
                    flushAll();
                }
            }

            if (mode.getInput() == 4) {
                while (!packets.isEmpty()) {
                    long timestamp = (Long) packets.get(0).get("time");
                    if (now - timestamp >= delayv) {
                        flushOne();
                    } else {
                        break;
                    }
                }
            }

            if (fb || !conditionals || mc.thePlayer.onGround || Utils.overVoid() || !containsVelocity() || !Utils.nullCheck()/* || Raven.packetsHandler.C02.sentCurrentTick.get()*/) {
                flushAll();
            }
        }
        fb = false;
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (canJump && mc.thePlayer.onGround) {
            //mc.thePlayer.movementInput.jump = true;
        }
        canJump = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof S27PacketExplosion) {
            db = 10;
        }
        if (LongJump.stopVelocity || disableLobby.isToggled() && Utils.isLobby()) {
            return;
        }
        if (mode.getInput() == 1 || mode.getInput() == 4 || mode.getInput() == 5) {
            if (ModuleManager.bhop.isEnabled() && ModuleManager.bhop.damageBoost.isToggled() && ModuleUtils.firstDamage && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
                return;
            }
            if (e.getPacket() instanceof S27PacketExplosion) {
                Packet packet = e.getPacket();
                S27PacketExplosion s27PacketExplosion = (S27PacketExplosion) e.getPacket();
                S27PacketExplosion s27 = (S27PacketExplosion) packet;

                if (mode.getInput() == 1) {
                    if (allowSelfFireball.isToggled() && ModuleUtils.threwFireball) {
                        if ((mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) <= ModuleUtils.MAX_EXPLOSION_DIST_SQ) || disableVelo) {
                            disableVelo = true;
                            ModuleUtils.threwFireball = false;
                            e.setCanceled(false);
                            return;
                        }
                    }
                    if (!dontEditMotion() && !disableVelo) {
                        if (explosionsHorizontal.getInput() == 0 && explosionsVertical.getInput() > 0) {
                            mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                        } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() == 0) {
                            mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                            mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                        } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() > 0) {
                            mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                            mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                            mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                        }
                    }
                }

                stopFBvelo = true;
                fb = true;
                if (mode.getInput() == 1) {
                    e.setCanceled(true);
                    disableVelo = false;
                }
            }
            if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                    S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();

                    if (mode.getInput() == 4 || mode.getInput() == 5) {
                        if (s12PacketEntityVelocity.getEntityID() == mc.thePlayer.getEntityId() && conditionals && !stopFBvelo) {
                            /*if (mode.getInput() == 5 && dontEditMotion()) {
                                e.setCanceled(true);
                            }
                            else {*/
                                delaying = true;
                            //}
                        }
                    }

                    if (mode.getInput() == 1) {
                        if (!stopFBvelo) {
                            if (!dontEditMotion() && !disableVelo) {
                                if (horizontal.getInput() == 0 && vertical.getInput() > 0) {
                                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                                } else if (horizontal.getInput() > 0 && vertical.getInput() == 0) {
                                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                                } else if (horizontal.getInput() > 0 && vertical.getInput() > 0) {
                                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * horizontal.getInput() / 100.0;
                                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * vertical.getInput() / 100.0;
                                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * horizontal.getInput() / 100.0;
                                }
                            }
                        } else {
                            if (!dontEditMotion() && !disableVelo) {
                                if (explosionsHorizontal.getInput() == 0 && explosionsVertical.getInput() > 0) {
                                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * explosionsVertical.getInput() / 100.0;
                                } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() == 0) {
                                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                } else if (explosionsHorizontal.getInput() > 0 && explosionsVertical.getInput() > 0) {
                                    mc.thePlayer.motionX = ((double) s12PacketEntityVelocity.getMotionX() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                    mc.thePlayer.motionY = ((double) s12PacketEntityVelocity.getMotionY() / 8000) * explosionsVertical.getInput() / 100.0;
                                    mc.thePlayer.motionZ = ((double) s12PacketEntityVelocity.getMotionZ() / 8000) * explosionsHorizontal.getInput() / 100.0;
                                }
                            }
                        }
                    }

                    stopFBvelo = false;
                    if (mode.getInput() == 1) {
                        if (!disableVelo) {
                            e.setCanceled(true);
                        }
                    }
                }
            }
        }


        Packet p = e.getPacket();
        if (mode.getInput() == 5 && (p instanceof S0BPacketAnimation || p instanceof S25PacketBlockBreakAnim || p instanceof S24PacketBlockAction || p instanceof S2APacketParticles || p instanceof S04PacketEntityEquipment || p instanceof S0DPacketCollectItem || p instanceof S19PacketEntityHeadLook || p instanceof S02PacketChat)) {
            return;
        }
        if (!delaying) return;
        Map<String, Object> entry = new HashMap<>();
        entry.put("packet", e.getPacket());
        entry.put("time", Utils.time());
        synchronized (packets) {
            packets.add(entry);
        }
        e.setCanceled(true);
    }

    boolean conditionals() {
        if (disableLobby.isToggled() && Utils.isLobby()) return false;
        if (mc.thePlayer.isCollidedHorizontally) return false;
        //if (mc.thePlayer.onGround) return false;
        if (mc.thePlayer.capabilities.isFlying) return false;
        if (ModuleManager.bedAura.delaying) return false;
        return true;
    }

    void flushOne() {
        synchronized (packets) {
            Map<String, Object> entry = packets.remove(0);
            PacketUtils.receivePacketNoEvent((Packet) entry.get("packet"));
        }
    }

    void flushAll() {
        while (!packets.isEmpty()) {
            flushOne();
        }
        delaying = false;
        exp = false;
        optimize = false;
        ovd = 0;
    }

    boolean containsVelocity() {
        synchronized(packets) {
            int id = mc.thePlayer.getEntityId();
            for (Map<String, Object> entry : packets) {
                Packet p = (Packet) entry.get("packet");
                if (p instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) p).getEntityID() == id) return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent ev) {
        if (mode.getInput() == 0) {
            if (Utils.nullCheck() && !LongJump.stopVelocity) {
                if (mc.thePlayer.maxHurtTime <= 0 || mc.thePlayer.hurtTime != mc.thePlayer.maxHurtTime) {
                    return;
                }
                if (onlyWhileAttacking.isToggled() && !ModuleUtils.isAttacking) {
                    return;
                }
                if (onlyWhileSwinging.isToggled() && !ModuleUtils.isSwinging) {
                    return;
                }
                if (onlyWhileTargeting.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
                    return;
                }
                if (disableS.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
                    return;
                }
                if (chance.getInput() == 0) {
                    return;
                }
                if (disableLobby.isToggled() && Utils.isLobby()) {
                    return;
                }
                if (chance.getInput() != 100) {
                    double ch = Math.random();
                    if (ch >= chance.getInput() / 100.0D) {
                        return;
                    }
                }
                if (horizontal.getInput() != 100.0D) {
                    mc.thePlayer.motionX *= horizontal.getInput() / 100;
                    mc.thePlayer.motionZ *= horizontal.getInput() / 100;
                }
                if (vertical.getInput() != 100.0D) {
                    mc.thePlayer.motionY *= vertical.getInput() / 100;
                }
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook) {
            checkAim(((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C05PacketPlayerLook) e.getPacket()).getPitch());
        }
        else if (e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            checkAim(((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getYaw(), ((C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket()).getPitch());
        }
    }

    public boolean dontEditMotion() {
        if (mc.thePlayer.motionY >= verticalM.getInput() && !mc.thePlayer.onGround || mode.getInput() == 1 && zzWhileNotTargeting.isToggled() && KillAura.attackingEntity == null) {
            return true;
        }
        return false;
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