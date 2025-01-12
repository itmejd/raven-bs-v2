package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class LongJump extends Module {
    private SliderSetting boostSetting;
    private SliderSetting motionTicks;
    private SliderSetting verticalMotion;
    private SliderSetting motionDecay;

    private ButtonSetting allowStrafe;
    private ButtonSetting invertYaw;
    private ButtonSetting stopMovement;
    private ButtonSetting hideExplosion;

    private KeySetting temporaryFlightKey;
    private SliderSetting pitchVal;

    private float yaw;
    private float pitch;

    private boolean notMoving;
    private boolean enabled;

    private int boostTicks;
    private int lastSlot = -1;
    private int stopTime;
    private int rotateTick;
    private int motionDecayVal;

    private long fireballTime;
    private long MAX_EXPLOSION_DIST_SQ = 9;
    private long FIREBALL_TIMEOUT = 750L;

    public static boolean stopVelocity;
    public static boolean stopKillAura;
    public static boolean stopScaffold;
    public static boolean slotReset;
    public static int slotResetTicks;

    private String[] modes = new String[]{"Fireball", "Fireball Auto"};
    public LongJump() {
        super("Long Jump", category.movement);
        this.registerSetting(boostSetting = new SliderSetting("Horizontal boost", 1.7, 0.0, 2.0, 0.05));
        this.registerSetting(verticalMotion = new SliderSetting("Vertical motion", 0, 0.4, 0.9, 0.01));
        this.registerSetting(motionDecay = new SliderSetting("Motion decay", 17, 1, 40, 1));
        this.registerSetting(allowStrafe = new ButtonSetting("Allow strafe", false));
        this.registerSetting(invertYaw = new ButtonSetting("Invert yaw", true));
        this.registerSetting(stopMovement = new ButtonSetting("Stop movement", false));
        this.registerSetting(hideExplosion = new ButtonSetting("Hide explosion", false));

        this.registerSetting(temporaryFlightKey = new KeySetting("Vertical key", Keyboard.KEY_SPACE));

        //this.registerSetting(new DescriptionSetting("Dev:"));
        //this.registerSetting(pitchVal = new SliderSetting("Stop movement Pitch", 55, 55, 80, 0.1));
    }

    public void onEnable() {
        slotReset = false;
        slotResetTicks = 0;
        enabled = true;
        ModuleManager.bhop.disable();
        stopKillAura = true;
        stopScaffold = true;
    }

    public void onDisable() {
        fireballTime = rotateTick = stopTime = 0;
        boostTicks = -1;
        resetSlot();
        enabled = notMoving = stopVelocity = false;
    }

    /*public boolean onChat(String chatMessage) {
        String msg = util.strip(chatMessage);

        if (msg.equals("Build height limit reached!")) {
            client.print("fb fly build height");
            modules.disable(scriptName);
            return false;
        }
        return true;
    }*/

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (enabled) {
            if (!Utils.isMoving()) notMoving = true;
            if (boostSetting.getInput() == 0 && verticalMotion.getInput() == 0) {
                Utils.print("&cValues are set to 0!");
                disable();
                return;
            }
            int fireballSlot = setupFireballSlot(true);
            if (fireballSlot != -1) {
                lastSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = fireballSlot;
                //("Set fireball slot");
                rotateTick = 1;
                if (stopMovement.isToggled()) {
                    stopTime = 1;
                }
            } // auto disables if -1
            enabled = false;
        }

        if (notMoving) {
            motionDecayVal = 21;
        } else {
            motionDecayVal = (int) motionDecay.getInput();
        }
        if (stopTime == -1 && ++boostTicks > (!temporaryFlightKey() ? 33/*flat motion ticks*/ : (!notMoving ? 32/*normal motion ticks*/ : 33/*vertical motion ticks*/))) {
            disable();
            return;
        }

        if (fireballTime > 0 && (System.currentTimeMillis() - fireballTime) > FIREBALL_TIMEOUT) {
            Utils.print("&cFireball timed out.");
            disable();
            return;
        }
        if (boostTicks > 0) {
            modifyVertical(); // has to be onPreUpdate
            //Utils.print("Modifying vertical");
            if (allowStrafe.isToggled()) {
                Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
                //Utils.print("Speed");
            }
        }

        if (stopMovement.isToggled() && !notMoving) {
            if (stopTime > 0) {
                ++stopTime;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (rotateTick > 0) {
            if ((invertYaw.isToggled() || stopMovement.isToggled()) && !notMoving) {
                if (!stopMovement.isToggled()) {
                    if (rotateTick == 1) {
                        yaw = mc.thePlayer.rotationYaw - 180f;
                        pitch = 90f;
                    }
                } else {
                    if (rotateTick == 1) {
                        yaw = mc.thePlayer.rotationYaw - 180f;
                        pitch = 66.3f;//(float) pitchVal.getInput();
                    }
                }
            } else {
                if (rotateTick == 1) {
                    yaw = mc.thePlayer.rotationYaw;
                    pitch = 90f;
                }
            }
            if (rotateTick > 1) {
                yaw = mc.thePlayer.rotationYaw;
                pitch = mc.thePlayer.rotationPitch;
            }
            e.setYaw(yaw);
            e.setPitch(pitch);
        }
        if (rotateTick > 0 && ++rotateTick >= 3) {
            rotateTick = 0;
            int fireballSlot = setupFireballSlot(false);
            if (fireballSlot != -1) {
                mc.thePlayer.inventory.currentItem = fireballSlot; // we are probably already on the slot but make sure
                fireballTime = System.currentTimeMillis();
                Reflection.rightClick();
                stopVelocity = true;
                //Utils.print("Right click");
            }
        }
        if (boostTicks == 1) {
            if (invertYaw.isToggled()) {
                //client.setMotion(client.getMotion().x, client.getMotion().y + 0.035d, client.getMotion().z);
            }
            modifyHorizontal();
            stopVelocity = false;
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent e) {
        mc.thePlayer.movementInput.jump = false;
        if (rotateTick > 0 || fireballTime > 0) {
            if (Utils.isMoving()) e.setForward(1);
            e.setStrafe(0);
        }
        if (notMoving && boostTicks < 3) {
            e.setForward(0);
            e.setStrafe(0);
            Utils.setSpeed(0);
        }
        if (stopMovement.isToggled() && !notMoving && stopTime >= 1) {
            e.setForward(0);
            e.setStrafe(0);
            Utils.setSpeed(0);
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        Packet packet = e.getPacket();
        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion s27 = (S27PacketExplosion) packet;
            if (fireballTime == 0 || mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) > MAX_EXPLOSION_DIST_SQ) {
                e.setCanceled(true);
                //Utils.print("0 fb time / out of dist");
            }

            stopTime = -1;
            fireballTime = 0;
            resetSlot();
            boostTicks = 0; // +1 on next pre update
            //Utils.print("set start vals");

            //client.print(client.getPlayer().getTicksExisted() + " s27 " + boostTicks + " " + client.getPlayer().getHurtTime() + " " + client.getPlayer().getSpeed());
        } else if (packet instanceof S08PacketPlayerPosLook) {
            Utils.print("&cReceived setback, disabling.");
            disable();
        }

        if (hideExplosion.isToggled() && fireballTime != 0 && (packet instanceof S0EPacketSpawnObject || packet instanceof S2APacketParticles || packet instanceof S29PacketSoundEffect)) {
            e.setCanceled(true);
        }
    }

    private int getFireballSlot() {
        int n = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
            if (getStackInSlot != null && getStackInSlot.getItem() == Items.fire_charge) {
                n = i;
                break;
            }
        }
        return n;
    }

    private int setupFireballSlot(boolean pre) {
        // only cancel bad packet right click on the tick we are sending it
        int fireballSlot = getFireballSlot();
        if (fireballSlot == -1) {
            Utils.print("&cFireball not found.");
            disable();
        } else if (ModuleManager.scaffold.isEnabled || (pre && Utils.distanceToGround(mc.thePlayer) > 3)/* || (!pre && !PacketUtil.canRightClickItem())*/) { //needs porting
            Utils.print("&cCan't throw fireball right now.");
            disable();
            fireballSlot = -1;
        }
        return fireballSlot;
    }

    private void resetSlot() {
        if (lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot;
            lastSlot = -1;
        }
        slotReset = true;
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    // only apply horizontal boost once
    void modifyHorizontal() {
        if (boostSetting.getInput() != 0) {
            //client.print("&7horizontal &b" + boostTicks + " " + client.getPlayer().getHurtTime());

            double speed = boostSetting.getInput() - Utils.randomizeDouble(0.0001, 0);
            if (Utils.isMoving()) {
                Utils.setSpeed(speed);
                //Utils.print("og speed");
            }
        }
    }

    private void modifyVertical() {
        if (verticalMotion.getInput() != 0) {
            double ver = ((!notMoving ? verticalMotion.getInput() : 1.16 /*vertical*/) * (1.0 / (1.0 + (0.05 * getSpeedLevel())))) + Utils.randomizeDouble(0.0001, 0.1);
            double decay = motionDecay.getInput() / 1000;
            if (boostTicks > 1 && !temporaryFlightKey()) {
                if (boostTicks > 1 || boostTicks <= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/)) {
                    mc.thePlayer.motionY = Utils.randomizeDouble(0.0101, 0.01);
                }
            } else {
                if (boostTicks >= 1 && boostTicks <= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/)) {
                    mc.thePlayer.motionY = ver - boostTicks * decay;
                } else if (boostTicks >= (!notMoving ? 32/*horizontal motion ticks*/ : 33/*vertical motion ticks*/) + 3) {
                    mc.thePlayer.motionY = mc.thePlayer.motionY + 0.028;
                    Utils.print("?");
                }
            }
        }
    }

    private boolean temporaryFlightKey() {
        if (notMoving) return true;
        return temporaryFlightKey.isPressed();
    }

    private int getKeyCode(String keyName) {
        switch (keyName) {
            case "0": return 11;
            case "1": return 2;
            case "2": return 3;
            case "3": return 4;
            case "4": return 5;
            case "5": return 6;
            case "6": return 7;
            case "7": return 8;
            case "8": return 9;
            case "9": return 10;
            case "A": return 30;
            case "B": return 48;
            case "C": return 46;
            case "D": return 32;
            case "E": return 18;
            case "F": return 33;
            case "G": return 34;
            case "H": return 35;
            case "I": return 23;
            case "J": return 36;
            case "K": return 37;
            case "L": return 38;
            case "M": return 50;
            case "N": return 49;
            case "O": return 24;
            case "P": return 25;
            case "Q": return 16;
            case "R": return 19;
            case "S": return 31;
            case "T": return 20;
            case "U": return 22;
            case "V": return 47;
            case "W": return 17;
            case "X": return 45;
            case "Y": return 21;
            case "Z": return 44;
            case "BACK": return 14;
            case "CAPITAL": return 58;
            case "COMMA": return 51;
            case "DELETE": return 211;
            case "DOWN": return 208;
            case "END": return 207;
            case "ESCAPE": return 1;
            case "F1": return 59;
            case "F2": return 60;
            case "F3": return 61;
            case "F4": return 62;
            case "F5": return 63;
            case "F6": return 64;
            case "F7": return 65;
            case "HOME": return 199;
            case "INSERT": return 210;
            case "LBRACKET": return 26;
            case "LCONTROL": return 29;
            case "LMENU": return 56;
            case "LMETA": return 219;
            case "LSHIFT": return 42;
            case "MINUS": return 12;
            case "NUMPAD0": return 82;
            case "NUMPAD1": return 79;
            case "NUMPAD2": return 80;
            case "NUMPAD3": return 81;
            case "NUMPAD4": return 75;
            case "NUMPAD5": return 76;
            case "NUMPAD6": return 77;
            case "NUMPAD7": return 71;
            case "NUMPAD8": return 72;
            case "NUMPAD9": return 73;
            case "PERIOD": return 52;
            case "RETURN": return 28;
            case "RCONTROL": return 157;
            case "RSHIFT": return 54;
            case "RBRACKET": return 27;
            case "SEMICOLON": return 39;
            case "SLASH": return 53;
            case "SPACE": return 57;
            case "TAB": return 15;
            case "GRAVE": return 41;
            default: return -1;
        }
    }
}
