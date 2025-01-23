package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.ModuleUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import org.lwjgl.input.Keyboard;

public class Velocity extends Module {
    private SliderSetting velocityModes;
    private SliderSetting horizontal;
    private SliderSetting vertical;
    private SliderSetting chance;
    private ButtonSetting onlyWhileAttacking;
    private ButtonSetting onlyWhileTargeting;
    private ButtonSetting disableS;
    private ButtonSetting zzWhileNotTargeting;
    private ButtonSetting disableExplosions;
    private ButtonSetting allowSelfFireball;
    private boolean stopFBvelo;
    public boolean disableVelo;

    private String[] velocityModesString = new String[] { "Normal", "Hypixel" };


    public Velocity() {
        super("Velocity", category.combat);
        this.registerSetting(velocityModes = new SliderSetting("Mode", 0, velocityModesString));
        this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
        this.registerSetting(chance = new SliderSetting("Chance", "%", 100.0D, 0.0D, 100.0D, 1.0D));
        this.registerSetting(onlyWhileAttacking = new ButtonSetting("Only while attacking", false));
        this.registerSetting(onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
        this.registerSetting(disableS = new ButtonSetting("Disable while holding S", false));
        this.registerSetting(zzWhileNotTargeting = new ButtonSetting("00 while not targeting", false));
        this.registerSetting(disableExplosions = new ButtonSetting("Disable explosions", false));
        this.registerSetting(allowSelfFireball = new ButtonSetting("Allow self fireball", false));
    }

    public void guiUpdate() {
        this.onlyWhileAttacking.setVisible(velocityModes.getInput() == 0, this);
        this.onlyWhileTargeting.setVisible(velocityModes.getInput() == 0, this);
        this.disableS.setVisible(velocityModes.getInput() == 0, this);

        this.allowSelfFireball.setVisible(velocityModes.getInput() == 1, this);
        this.disableExplosions.setVisible(velocityModes.getInput() == 1, this);
        this.zzWhileNotTargeting.setVisible(velocityModes.getInput() == 1, this);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (velocityModes.getInput() == 1) {
            if (!Utils.nullCheck() || LongJump.stopVelocity || e.isCanceled() || ModuleManager.bedAura.cancelKnockback()) {
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
                if (!dontEditMotion() && !disableVelo && !disableExplosions.isToggled() && !ModuleManager.bedAura.cancelKnockback()) {
                    if (horizontal.getInput() == 0 && vertical.getInput() > 0) {
                        mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * vertical.getInput() / 100.0;
                    } else if (horizontal.getInput() > 0 && vertical.getInput() == 0) {
                        mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * horizontal.getInput() / 100.0;
                        mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * horizontal.getInput() / 100.0;
                    } else if (horizontal.getInput() > 0 && vertical.getInput() > 0) {
                        mc.thePlayer.motionX += s27PacketExplosion.func_149149_c() * horizontal.getInput() / 100.0;
                        mc.thePlayer.motionY += s27PacketExplosion.func_149144_d() * vertical.getInput() / 100.0;
                        mc.thePlayer.motionZ += s27PacketExplosion.func_149147_e() * horizontal.getInput() / 100.0;
                    }
                }
                if (disableExplosions.isToggled()) stopFBvelo = true;
                e.setCanceled(true);
                disableVelo = false;
            }
            if (e.getPacket() instanceof S12PacketEntityVelocity) {
                if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                    S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity) e.getPacket();

                    if (!dontEditMotion() && !disableVelo && !stopFBvelo && !ModuleManager.bedAura.cancelKnockback()) {
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

    @Override
    public int getInfoType() {
        return 1;
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
        if (zzWhileNotTargeting.isToggled() && !ModuleManager.killAura.isTargeting) {
            return true;
        }

        return false;
    }

}