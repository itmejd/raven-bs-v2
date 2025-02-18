package keystrokesmod.module.impl.player;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tower extends Module {
    final public SliderSetting towerMove;
    private SliderSetting speedSetting;
    final public SliderSetting verticalTower;
    final private SliderSetting slowedSpeed;
    final private SliderSetting slowedTicks;
    final private ButtonSetting disableWhileHurt;
    private GroupSetting cancelKnockbackGroup;
    private final ButtonSetting cancelKnockback;
    private ButtonSetting cancelVelocityRequired;

    final private String[] towerMoveModes = new String[]{"None", "Vanilla", "Low", "Edge", "2.5 tick", "1.5 tick", "1 tick"};
    final private String[] verticalTowerModes = new String[]{"None", "Vanilla", "Extra block"};
    private int slowTicks;
    private boolean wasTowering;
    private int towerTicks;
    public boolean towering;
    private boolean hasTowered, startedTowerInAir, setLowMotion, firstJump;
    private int cMotionTicks, placeTicks;
    public int dCount;
    public float yaw;

    public float pitch;

    public boolean finishedTower;

    //vertical tower
    private boolean aligning, aligned, placed;
    private int blockX;
    private double firstX, firstY, firstZ;
    public boolean placeExtraBlock;

    public boolean speed;

    private int grounds;

    public Tower() {
        super("Tower", category.player);
        this.registerSetting(towerMove = new SliderSetting("Tower Move", 0, towerMoveModes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 3.0, 0.5, 8.0, 0.1));
        this.registerSetting(verticalTower = new SliderSetting("Vertical Tower", 0, verticalTowerModes));
        this.registerSetting(slowedSpeed = new SliderSetting("Slowed speed", "%", 0, 0, 100, 1));
        this.registerSetting(slowedTicks = new SliderSetting("Slowed ticks", 1, 0, 20, 1));
        this.registerSetting(disableWhileHurt = new ButtonSetting("Disable while hurt", false));
        this.registerSetting(cancelKnockbackGroup = new GroupSetting("Cancel knockback"));
        this.registerSetting(cancelKnockback = new ButtonSetting(cancelKnockbackGroup, "Enable Cancel knockback", false));
        this.registerSetting(cancelVelocityRequired = new ButtonSetting(cancelKnockbackGroup, "Require velocity enabled", false));

        this.canBeEnabled = false;
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || !cancelKnockback()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (canTower() && Utils.keysDown()) {
            if (disableWhileHurt.isToggled() && ModuleUtils.damage) {
                return;
            }
            switch ((int) towerMove.getInput()) {
                case 1:

                    break;
                case 2:

                    break;
                case 3:

                    break;
                case 4:
                    if (towering) {
                        if (towerTicks == 6) {
                            e.setPosY(e.getPosY() + 0.000383527);
                            ModuleManager.scaffold.rotateForward();
                        }
                    }
                    break;
                case 5:

                    break;
                case 6:

                    break;
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        int simpleY = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
        if (canTower() && Utils.keysDown()) {
            speed = false;
            wasTowering = hasTowered = true;
            if (disableWhileHurt.isToggled() && ModuleUtils.damage) {
                towerTicks = 0;
                towering = false;
                return;
            }
            switch ((int) towerMove.getInput()) {
                case 1:
                    mc.thePlayer.motionY = 0.41965;
                    switch (towerTicks) {
                        case 1:
                            mc.thePlayer.motionY = 0.33;
                            break;
                        case 2:
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                            break;
                    }
                    if (towerTicks >= 3) {
                        towerTicks = 0;
                    }
                case 2:
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.4196;
                    }
                    else {
                        switch (towerTicks) {
                            case 3:
                            case 4:
                                mc.thePlayer.motionY = 0;
                                break;
                            case 5:
                                mc.thePlayer.motionY = 0.4191;
                                break;
                            case 6:
                                mc.thePlayer.motionY = 0.3275;
                                break;
                            case 11:
                                mc.thePlayer.motionY = - 0.5;

                        }
                    }
                    break;
                case 3:
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround && !setLowMotion) {
                        towering = true;
                    }
                    if (towering) {
                        if (valY == 0) {
                            mc.thePlayer.motionY = 0.42f;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                            startedTowerInAir = false;
                        }
                        else if (valY > 4000 && valY < 4300) {
                            mc.thePlayer.motionY = 0.33f;
                            Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                            speed = true;
                        }
                        else if (valY > 7000) {
                            if (setLowMotion) {
                                towering = false;
                            }
                            mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                        }
                    }
                    else if (setLowMotion) {
                        ++cMotionTicks;
                        if (cMotionTicks == 1) {
                            mc.thePlayer.motionY = 0.08F;
                            Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                        }
                        else if (cMotionTicks == 4) {
                            cMotionTicks = 0;
                            setLowMotion = false;
                            towering = true;
                            Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.02);
                        }
                    }
                    break;
                case 4:
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                        towering = true;
                    }
                    if (towering) {
                        towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                        switch (simpleY) {
                            case 0:
                                mc.thePlayer.motionY = 0.42f;
                                if (towerTicks == 6) {
                                    mc.thePlayer.motionY = -0.078400001525879;
                                }
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                                break;
                            case 42:
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                                break;
                            case 75:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                                break;
                        }
                    }
                    break;
                case 5:
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                        towering = true;
                    }
                    if (towering) {
                        towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                        switch (towerTicks) {
                            case 0:
                                mc.thePlayer.motionY = 0.42f;
                                Utils.setSpeed(get15tickspeed(getSpeedLevel())); // Speed + Strafe tick
                                speed = true;
                                break;
                            case 1:
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                break;
                            case 2:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                                break;
                            case 3:
                                mc.thePlayer.motionY = 0.42f;
                                Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                break;
                            case 4:
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                break;
                            case 5:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f + 0.0000001;
                                break;
                            case 6:
                                mc.thePlayer.motionY = -0.01;
                                break;
                        }
                    }
                    break;
                case 6:
                    if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                        grounds++;
                    }
                    if (mc.thePlayer.posY % 1 == 0) {
                        towering = true;
                    }
                    if (towering) {
                        towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                        switch (towerTicks) {
                            case 0:
                                mc.thePlayer.motionY = 0.42f;
                                Utils.setSpeed(get15tickspeed(getSpeedLevel())); // Speed + Strafe tick
                                speed = true;
                                break;
                            case 1:
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                break;
                            case 2:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f;
                                break;
                            case 3:
                                mc.thePlayer.motionY = 0.005;
                                break;
                        }
                    }
                    break;
            }
        }
        else {
            if (finishedTower) {
                finishedTower = false;
            }
            if (hasTowered) {
                finishedTower = true;
            }
            if (wasTowering && modulesEnabled()) {
                if (slowedTicks.getInput() > 0 && slowedTicks.getInput() != 100 && slowTicks++ < slowedTicks.getInput()) {
                    mc.thePlayer.motionX *= slowedSpeed.getInput() / 100;
                    mc.thePlayer.motionZ *= slowedSpeed.getInput() / 100;
                }
                else {
                    ModuleUtils.handleSlow();
                }
                if (slowTicks >= slowedTicks.getInput()) {
                    slowTicks = 0;
                    wasTowering = false;
                }
            }
            else {
                if (wasTowering) {
                    wasTowering = false;
                }
                slowTicks = 0;
            }
            if (speed || hasTowered && mc.thePlayer.onGround) {
                Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
            }
            hasTowered = towering = firstJump = startedTowerInAir = setLowMotion = speed = false;
            cMotionTicks = placeTicks = towerTicks = grounds = 0;
            reset();
        }
        if (canTower() && !Utils.keysDown()) {
            wasTowering = true;
            switch ((int) verticalTower.getInput()) {
                case 1:
                    mc.thePlayer.motionY = 0.42f;
                    break;
                case 2:
                    if (!aligned) {
                        if (mc.thePlayer.onGround) {
                            if (!aligning) {
                                blockX = (int) mc.thePlayer.posX;

                                firstX = mc.thePlayer.posX - 10;
                                firstY = mc.thePlayer.posY;
                                firstZ = mc.thePlayer.posZ;
                            }
                            mc.thePlayer.motionX = 0.22;
                            aligning = true;
                        }
                        if (aligning && (int) mc.thePlayer.posX > blockX) {
                            aligned = true;
                        }
                        yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                        pitch = 0;
                    }
                    if (aligned) {
                        if (placed) {
                            yaw = 0;
                            pitch = 89.9F;
                        }
                        else {
                            yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                            pitch = 0;
                        }
                        placeExtraBlock = true;
                        mc.thePlayer.motionX = 0;
                        mc.thePlayer.motionY = verticalTowerValue();
                        mc.thePlayer.motionZ = 0;
                    }
                    break;
            }
        } else {
            yaw = pitch = 0;
            aligning = aligned = placed = false;
            firstX = 0;
            placeExtraBlock = false;
        }
    }

    public boolean isVerticalTowering() {
        return canTower() && !Utils.keysDown() && verticalTower.getInput() == 2;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMoveInput(PrePlayerInputEvent e) {
        if (!ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (canTower() && Utils.keysDown() && towerMove.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
            if (!firstJump) {
                if (!mc.thePlayer.onGround) {
                    if (!startedTowerInAir) {
                        //Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.04);
                    }
                    startedTowerInAir = true;
                }
                else if (mc.thePlayer.onGround) {
                    //Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                    firstJump = true;
                }
            }
        }
        if (canTower() && !Utils.keysDown() && verticalTower.getInput() > 0) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (canTower() && Utils.isMoving()) {
                ++placeTicks;
                if (((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection() == 1 && placeTicks > 5 && hasTowered) {
                    dCount++;
                    if (dCount >= 2) {
                        //Utils.sendMessage("Hey");
                        setLowMotion = true;
                    }
                }
                else {
                    dCount = 0;
                }
            }
            else {
                placeTicks = 0;
            }

            if (aligned) {
                placed = true;
            }
            //Utils.print("" + ((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection());
        }
    }

    private void reset() {
        towerTicks = 0;
        towering = false;
        placeTicks = 0;
        setLowMotion = false;
    }

    public boolean cancelKnockback() {
        if (!canTower()) {
            return false;
        }
        if (cancelVelocityRequired.isToggled() && !ModuleManager.velocity.isEnabled()) {
            return false;
        }
        if (cancelKnockback.isToggled()) {
            return true;
        }
        return false;
    }

    public boolean canTower() {
        if (!Utils.nullCheck() || !Utils.jumpDown() || !Utils.tabbedIn()) {
            return false;
        }
        else if (mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        else if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava())) {
            return false;
        }
        else if (modulesEnabled()) {
            return true;
        }
        return false;
    }

    private boolean modulesEnabled() {
        return (ModuleManager.scaffold.moduleEnabled && ModuleManager.scaffold.holdingBlocks() && ModuleManager.scaffold.hasSwapped && !ModuleManager.LongJump.function);
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

    private double verticalTowerValue() {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        double value = 0;
        if (valY == 0) {
            value = 0.42f;
        } else if (valY > 4000 && valY < 4300) {
            value = 0.33f;
        } else if (valY > 7000) {
            value = 1 - mc.thePlayer.posY % 1f;
        }
        return value;
    }

    private double getTowerSpeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) + 0.04;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10) + 0.08;
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.12;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.12;
        }
        return (speedSetting.getInput() / 10);
    }

    private double getTowerGroundSpeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10) - 0.08;
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) - 0.05;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.05;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.10;
        }
        return (speedSetting.getInput() / 10) - 0.08;
    }

    private double get15tickspeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) + 0.04;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10) + 0.08;
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.12;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.13;
        }
        return (speedSetting.getInput() / 10);
    }

}