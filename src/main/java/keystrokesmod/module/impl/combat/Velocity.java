package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceiveAllPacketsEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.ModuleUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ConcurrentLinkedQueue;

public class Velocity extends Module {
    public SliderSetting velocityModes;
    public static SliderSetting vertical, horizontal, reverseHorizontal, explosionsHorizontal, explosionsVertical;
    public static SliderSetting minExtraSpeed, extraSpeedBoost;
    private SliderSetting chance;
    private ButtonSetting onlyWhileAttacking;
    private ButtonSetting onlyWhileTargeting;
    private ButtonSetting disableS;
    private ButtonSetting zzWhileNotTargeting, delayPacket;
    public ButtonSetting allowSelfFireball;
    public static ButtonSetting reverseDebug;
    private KeySetting switchToReverse, switchToPacket;
    private boolean stopFBvelo;
    public boolean disableVelo;
    private long delay;
    private boolean buttonDown, pDown, rDown;

    private String[] velocityModesString = new String[] { "Normal", "Packet", "Reverse" };


    public Velocity() {
        super("Velocity", category.combat);
        this.registerSetting(velocityModes = new SliderSetting("Mode", 0, velocityModesString));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));

        this.registerSetting(reverseHorizontal = new SliderSetting("-Horizontal", 0.0, 0.0, 100.0, 1.0));

        this.registerSetting(explosionsHorizontal = new SliderSetting("Horizontal (Explosions)", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(explosionsVertical = new SliderSetting("Vertical (Explosions)", 0.0, 0.0, 100.0, 1.0));

        this.registerSetting(minExtraSpeed = new SliderSetting("Maximum speed for extra boost", 0, 0, 0.7, 0.01));
        this.registerSetting(extraSpeedBoost = new SliderSetting("Extra speed boost multiplier", "%", 0, 0, 100, 1));


        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(onlyWhileAttacking = new ButtonSetting("Only while attacking", false));
        this.registerSetting(onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(disableS = new ButtonSetting("Disable while holding S", false));
        this.registerSetting(zzWhileNotTargeting = new ButtonSetting("00 while not targeting", false));
        this.registerSetting(allowSelfFireball = new ButtonSetting("Allow self fireball", false));

        this.registerSetting(switchToReverse = new KeySetting("Switch to reverse", Keyboard.KEY_SPACE));
        this.registerSetting(switchToPacket = new KeySetting("Switch to packet", Keyboard.KEY_SPACE));

        this.registerSetting(reverseDebug = new ButtonSetting("Show reverse debug messages", false));
    }

    public void guiUpdate() {
        this.onlyWhileAttacking.setVisible(velocityModes.getInput() == 0, this);
        this.onlyWhileTargeting.setVisible(velocityModes.getInput() == 0, this);
        this.disableS.setVisible(velocityModes.getInput() == 0, this);

        this.allowSelfFireball.setVisible(velocityModes.getInput() == 1, this);
        this.zzWhileNotTargeting.setVisible(velocityModes.getInput() == 1, this);

        this.switchToReverse.setVisible(velocityModes.getInput() == 1, this);
        this.switchToPacket.setVisible(velocityModes.getInput() == 2, this);



        this.horizontal.setVisible(velocityModes.getInput() != 2, this);
        this.vertical.setVisible(velocityModes.getInput() != 2, this);
        this.chance.setVisible(velocityModes.getInput() != 2, this);
        this.reverseHorizontal.setVisible(velocityModes.getInput() == 2, this);

        this.explosionsHorizontal.setVisible(velocityModes.getInput() != 0, this);
        this.explosionsVertical.setVisible(velocityModes.getInput() != 0, this);

        this.minExtraSpeed.setVisible(velocityModes.getInput() == 2, this);
        this.extraSpeedBoost.setVisible(velocityModes.getInput() == 2, this);
        this.reverseDebug.setVisible(velocityModes.getInput() == 2, this);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (Utils.tabbedIn()) {
            if (switchToReverse.isPressed() && velocityModes.getInput() == 1 && !buttonDown) {
                velocityModes.setValue(2);
                buttonDown = true;
                Utils.print(Utils.formatColor("&7[&dR&7]&7 Switched to &bReverse&7 Velocity mode"));
            }
            if (switchToPacket.isPressed() && velocityModes.getInput() == 2 && !buttonDown) {
                velocityModes.setValue(1);
                buttonDown = true;
                Utils.print(Utils.formatColor("&7[&dR&7]&7 Switched to &bPacket&7 Velocity mode"));
            }
        }
        if (switchToReverse.isPressed() || switchToPacket.isPressed()) {
            buttonDown = true;
        }
        else {
            buttonDown = false;
        }
    }

    @SubscribeEvent
    public void onReceivePacketAll(ReceiveAllPacketsEvent e) {
        if (velocityModes.getInput() >= 1) {
            if (!Utils.nullCheck() || LongJump.stopVelocity || e.isCanceled() || velocityModes.getInput() == 2 && ModuleUtils.firstDamage || ModuleManager.bhop.isEnabled() && ModuleManager.bhop.damageBoost.isToggled() && ModuleUtils.firstDamage && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
                return;
            }
            if (e.getPacket() instanceof S27PacketExplosion) {
                Packet packet = e.getPacket();
                S27PacketExplosion s27PacketExplosion = (S27PacketExplosion) e.getPacket();
                S27PacketExplosion s27 = (S27PacketExplosion) packet;

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

                stopFBvelo = true;
                e.setCanceled(true);
                disableVelo = false;
            }
            if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                    S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();

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
                    }
                    else {
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

                    stopFBvelo = false;
                    if (!disableVelo) {
                        e.setCanceled(true);
                    }
                }
            }
        }
    }

    @Override
    public String getInfo() {
        return (int) horizontal.getInput() + "%" + " " + (int) vertical.getInput() + "%";
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent ev) {
        if (velocityModes.getInput() == 0) {
            if (Utils.nullCheck() && !LongJump.stopVelocity && !ModuleManager.bedAura.cancelKnockback()) {
                if (mc.thePlayer.maxHurtTime <= 0 || mc.thePlayer.hurtTime != mc.thePlayer.maxHurtTime) {
                    return;
                }
                if (onlyWhileAttacking.isToggled() && !ModuleUtils.isAttacking) {
                    return;
                }
                if (dontEditMotion()) {
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

    private boolean dontEditMotion() {
        if (velocityModes.getInput() == 1 && zzWhileNotTargeting.isToggled() && KillAura.attackingEntity != null || ModuleManager.noFall.start || ModuleManager.blink.isEnabled() && ModuleManager.blink.cancelKnockback.isToggled() || ModuleManager.bedAura.cancelKnockback() || ModuleManager.tower.cancelKnockback()) {
            return true;
        }
        return false;
    }

    private boolean blinkModules() {
        if (ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blinking.get()) {
            return true;
        }
        if (ModuleManager.blink.isEnabled() && ModuleManager.blink.started) {
            return true;
        }
        if (ModuleManager.antiVoid.isEnabled() && ModuleManager.antiVoid.started) {
            return true;
        }
        return false;
    }

}