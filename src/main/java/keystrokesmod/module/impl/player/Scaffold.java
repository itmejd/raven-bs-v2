package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private final SliderSetting motion;
    public SliderSetting rotation, fakeRotation;
    public SliderSetting sprint;
    private SliderSetting floatFirstJump;
    public SliderSetting fastScaffold;
    private SliderSetting multiPlace;
    public ButtonSetting multiPlaceOnlyNecessary;

    public ButtonSetting autoSwap;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    public ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;
    private ButtonSetting prioritizeSprintWithSpeed;

    private String[] rotationModes = new String[] { "§cDisabled", "Simple", "Offset", "Precise" };
    private String[] fakeRotationModes = new String[] { "§cDisabled", "None", "Strict", "Smooth", "Spin", "Precise" };
    private String[] sprintModes = new String[] { "§cDisabled", "Vanilla", "Float" };
    private String[] fastScaffoldModes = new String[] { "§cDisabled", "Jump A", "Jump B", "Jump B Low", "Jump E", "Keep-Y", "Keep-Y Low" };
    private String[] multiPlaceModes = new String[] { "§cDisabled", "1 extra", "2 extra", "3 extra", "4 extra" };

    //Highlight blocks
    public Map<BlockPos, Timer> highlight = new HashMap<>();
    public boolean canBlockFade;

    //Block count
    private ScaffoldBlockCount scaffoldBlockCount;

    //swapping related
    public AtomicInteger lastSlot = new AtomicInteger(-1);
    private int spoofSlot;
    public boolean hasSwapped;
    private int blockSlot = -1;

    //placements related
    public boolean hasPlaced;
    private boolean finishProcedure;
    private boolean stopUpdate;
    private boolean stopUpdate2;
    private PlaceData lastPlacement;
    private EnumFacing[] facings = { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
    private BlockPos[] offsets = { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };
    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 blockPos, hitVec, lookVec;

    //bypass related
    private boolean rotateForward;
    private double startYPos = -1;
    public static boolean fastScaffoldKeepY;
    public boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    public boolean lowhop;
    private boolean normal;
    private int rotationDelay;
    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;
    public int offsetDelay;
    public boolean placedVP;
    public boolean jump;
    private int floatTicks;
    public boolean blink;
    public boolean canSprint, canSprint2;
    private boolean idle;
    private int idleTicks;
    private boolean didJump;
    private int placeIdle;
    public static boolean firstJump;
    public static int fjDelay;
    private boolean didLastRotation, setYawOffset;
    private boolean reqrp;
    private boolean placedb;

    //disable checks
    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;

    //rotation related
    private boolean lockRotation;
    private boolean was451, was452;
    private float minPitch, minOffset, pOffset;
    private float edge;
    private long firstStroke, yawEdge, vlS, swDelay;
    private float lastEdge2, yawAngle, theYaw;
    private boolean enabledOffGround = false;
    private float[] blockRotations;
    private float[] rotations2;
    public float yaw, pitch, blockYaw, yawOffset, lastOffset;
    private boolean set2;
    private float maxOffset;
    private int sameMouse;
    private int randomF, yawChanges, dynamic;
    private boolean getVTR, resetm;
    private float VTRY;
    private float normalYaw, normalPitch;
    private int switchvl;
    private int dt;
    private float getSmooth, lastYawS, smoothedYaw;
    private boolean neg, wasForward;
    private float yawWithOffset;
    private int rt = 1, forwardTicks;
    //fake rotations
    private float fakeYaw, fakePitch;
    private float fakeYaw1, fakeYaw2;

    private boolean firstRotate;

    private int canSnap;
    private boolean began;
    private boolean cantRotate, startRotation;
    private int srt;

    public static int jumpDelayVal, airTickVal;

    float correct = -180F;

    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", "%", 100, 50, 150, 1));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(fakeRotation = new SliderSetting("Rotation (fake)", 0, fakeRotationModes));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", 0, sprintModes));
        this.registerSetting(prioritizeSprintWithSpeed = new ButtonSetting("Prioritize sprint with speed", false));
        this.registerSetting(floatFirstJump = new SliderSetting("§eFloat §rfirst jump speed", "%", 100, 50, 100, 1));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));

        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));
        //this.registerSetting(multiPlaceOnlyNecessary = new ButtonSetting("Multi-place only necessary", false));

        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));

        this.alwaysOn = true;
    }

    public void guiUpdate() {
        this.prioritizeSprintWithSpeed.setVisible(sprint.getInput() > 0, this);
        this.floatFirstJump.setVisible(sprint.getInput() == 2, this);
    }

    public void onDisable() {
        if (ModuleManager.tower.canTower() && (ModuleManager.tower.dCount == 0 || !Utils.isMoving())) {
            towerEdge = true;
        }
        disabledModule = true;
        moduleEnabled = false;

        if (!isEnabled) {
            scaffoldBlockCount.beginFade();
        }
    }

    public void onEnable() {
        dt = 0;
        isEnabled = true;
        moduleEnabled = true;
        ModuleUtils.fadeEdge = 0;
        edge = -999999929;
        minPitch = 80F;
        if (!mc.thePlayer.onGround) {
            rotationDelay = Utils.randomizeInt(2, 3);
            enabledOffGround = true;
        }
        lastEdge2 = mc.thePlayer.rotationYaw;

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(mc));
        lastSlot.set(-1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        if (e.button == 0 || e.button == 1) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        normalYaw = mc.thePlayer.rotationYaw;
        normalPitch = mc.thePlayer.rotationPitch;
        if (dt > 0) {
            return;
        }

        dynamic = 0;

        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            rotations2 = RotationUtils.getRotations(lookAt);
            correct = rotations2[0];
            //if (rotation.getInput() != 1) {
                targetBlock = null;
            //}
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
            if (yawEdge == 0) {
                randomF = Utils.randomizeInt(0, 9);
                yawEdge = Utils.time();
            }
            dynamic++;
        }
        randomF = 0;

        //Fake rotations
        if (fakeRotation.getInput() > 0) {
            if (fakeRotation.getInput() == 1) {
                fakeYaw = normalYaw;
                fakePitch = normalPitch;
            } else if (fakeRotation.getInput() == 2) {
                fakeYaw = fakeYaw1;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            } else if (fakeRotation.getInput() == 3) {
                fakeYaw2 = mc.thePlayer.rotationYaw - hardcodedYaw();
                float yawDifference = Utils.getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = 0.35f;
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
                if (blockRotations != null) {
                    fakePitch = blockRotations[1] + 5;
                } else {
                    fakePitch = 80f;
                }
            } else if (fakeRotation.getInput() == 4) {
                fakeYaw += 25.71428571428571F;
                fakePitch = 90F;
            } else if (fakeRotation.getInput() == 5) {
                if (blockRotations != null) {
                    fakeYaw2 = blockRotations[0];
                    fakePitch = blockRotations[1];
                } else {
                    fakeYaw2 = mc.thePlayer.rotationYaw - hardcodedYaw() - 180;
                    fakePitch = 88F;
                }
                float yawDifference = Utils.getAngleDifference(lastEdge2, fakeYaw2);
                float smoothingFactor = 0.35f;
                fakeYaw2 = (lastEdge2 + yawDifference * smoothingFactor);
                lastEdge2 = fakeYaw2;

                fakeYaw = fakeYaw2;
            }
            RotationUtils.setFakeRotations(fakeYaw, fakePitch);
        }
        else {
            if (canSprint2 && rotation.getInput() == 1) {
                RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw - hardcodedYaw() - offsetvv, pitch);
            }
            if ((fastScaffoldKeepY || ModuleManager.tower.hasT)) {
                /*if (wasForward && jumpFacingForward.isToggled()) {
                    RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw - (hardcodedYaw() - 180F), mc.thePlayer.rotationPitch);
                    wasForward = false;
                }
                else {
                    RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw - hardcodedYaw(), pitch);
                }*/
            }
        }
        if (!isEnabled) {
            dt++;
            return;
        }
        scaffoldTicks++;
        canBlockFade = true;
        if (Utils.keysDown() && usingFastScaffold() && mc.thePlayer.hurtTime == 0 && !ModuleManager.invmove.active() && fastScaffold.getInput() >= 1 && !Utils.jumpDown() && !LongJump.function && !Utils.hasJump()) { // jump mode
            fastScaffoldKeepY = true;
        }
        else if (fastScaffoldKeepY && (mc.thePlayer.onGround || mc.thePlayer.hurtTime > 0)) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
            if (!ModuleManager.tower.canTower()) {
                firstJump = false;
                fjDelay = 0;
            }
        }
        if (fastScaffoldKeepY) {
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                if (scaffoldTicks > 1) {
                    jump = !jumpFacingForward.isToggled();
                    jumpDelayVal = 4;
                    airTickVal = 5;
                    rotateForward(true);
                    if (startYPos == -1 || Math.abs(startYPos - mc.thePlayer.posY) > 2) {
                        startYPos = mc.thePlayer.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }

        if (sprint.getInput() == 1) {
            canSprint2 = (!usingFastScaffold() && !fastScaffoldKeepY && !Utils.jumpDown() && !LongJump.function && mc.thePlayer.onGround);
        }

        //Float
        if (sprint.getInput() == 2) {
            if (Utils.isMoving() && idle && idleTicks ++ > 4) {
                if (floatKeepY) {
                    startYPos = -1;
                }
                floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
                floatTicks = rt = 0;
                canSprint2 = false;

                offsetDelay = 0;

                idle = false;
                idleTicks = 0;
                ModuleUtils.groundTicks = 9;
            }
            if (!usingFastScaffold() && !fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function) {
                if (ModuleUtils.stillTicks > 2 && mc.thePlayer.onGround) {
                    idle = true;
                    idleTicks = 0;
                }

                floatWasEnabled = true;
                if (!floatStarted && offsetDelay == 0) {
                    if (ModuleUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                        canSprint2 = true;
                        floatKeepY = true;
                        startYPos = e.posY;
                        rotateForward(true);
                        mc.thePlayer.jump();
                        if (Utils.isMoving()) {
                            double fvl = (getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001)) * (floatFirstJump.getInput() / 100);
                            Utils.setSpeed(fvl);
                        }
                        floatJumped = true;
                    } else if (ModuleUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                        floatStarted = true;
                    }
                    if (floatJumped && !mc.thePlayer.onGround) {
                        floatStarted = true;
                    }
                }

                if (floatStarted && mc.thePlayer.onGround) {
                    floatKeepY = false;
                    startYPos = -1;
                    if (moduleEnabled && mc.thePlayer.posY % 1 == 0) {
                        ++floatTicks;
                        rotateForward = false;
                        rotationDelay = 0;
                        ModuleManager.tower.delay = false;
                        canSprint2 = true;
                        if (didJump && !mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                        }
                        else if (!idle) {
                            switch (floatTicks) {
                                case 1:
                                case 4:
                                case 6:
                                    ModuleManager.tower.delay = true;
                                    ModuleManager.tower.delayTicks = 0;
                                    e.setPosY(e.getPosY() + 1e-3);
                                    break;
                                case 8:
                                    floatTicks = 0;
                                    break;
                            }
                            if (Utils.isMoving() && !ModuleManager.invmove.active()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                        }
                        ModuleUtils.groundTicks = 0;
                        offsetDelay = 2;
                    }
                }
            } else if (floatWasEnabled && moduleEnabled) {
                if (mc.thePlayer.onGround) {
                    Utils.setSpeed(Utils.getHorizontalSpeed() / 2);
                }
                if (floatKeepY) {
                    startYPos = -1;
                }
                floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
                floatTicks = rt = 0;
                canSprint2 = false;

                idle = false;
                idleTicks = 0;
            }
            didJump = false;
            if (ModuleManager.tower.delay && mc.gameSettings.keyBindJump.isKeyDown()) {
                didJump = true;
                canSprint2 = true;
            }
        }

        if (blockRotations != null) {
            if (mc.thePlayer.rotationYaw == lastOffset) {
                sameMouse++;
            }
            else {
                sameMouse = 0;
                yawChanges++;
            }
            if (sameMouse > 2) {
                yawChanges = 0;
            }
            lastOffset = mc.thePlayer.rotationYaw;
            if (yawChanges > 15) {
                randomF = 1;
                yawEdge = Utils.time();
            }
            if (yawEdge > 0 && (Utils.time() - yawEdge) > 500) {
                yawEdge = 0;
            }
        }
        else {
            fakeYaw1 = mc.thePlayer.rotationYaw - hardcodedYaw();
        }
    }

    private int rnj;
    private int frd, rnv, rtd;
    private float rnf = 20, bv = 20;

    private boolean canForward, over45;
    private int jumpDelay;


    private float uForward, uStrafe;

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        canSprint = false;
        t7 = false;
        if (!isEnabled) {
            getSmooth = lastYawS = (mc.thePlayer.rotationYaw - hardcodedYaw());
            return;
        }

        placeIdle++;

        float ny = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw);
        if (mc.thePlayer.onGround || ModuleUtils.inAirTicks >= 3) {
            if (ny >= 161.5 && ny <= 180) {
                normal = false;
            } else {
                normal = true;
            }
        }

        switch ((int) rotation.getInput()) {
            case 1:
                simpleRots(e);
                break;
            case 2:
                offsetRots(e);
                break;
            case 3:
                preciseRots(e);
                break;
        }
        if (edge != 1) {
            switchvl++;
            edge = 1;
        }
        if (mc.thePlayer.onGround) {
            enabledOffGround = false;
        }

        //jump facing forward
        if (rotationDelay > 0) {
            --rotationDelay;
        }
        if (ModuleUtils.inAirTicks >= 1 || rotation.getInput() != 1 && !mc.thePlayer.onGround) {
            rotateForward = false;
            jumpDelay = 0;
            forwardTicks = 0;
            lockRotation = false;
        }
        if (rotateForward && jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (back == 0) {
                    rnj++;
                    /*if (rnj == 1) {
                        rnf = (float) Utils.randomizeInt(2, 11);
                    } else if (rnj >= 2) {
                        rnf = (rnf - (rnf * 2));
                        rnj = 0;
                    }*/rnf = 0;
                }
                //if (firstJump) {
                float relativeYaw = mc.thePlayer.rotationYaw - hardcodedYaw() - 180F;
                float normalizedYaw = (relativeYaw % 360 + 360) % 360;
                float quad = normalizedYaw % 90;
                float by = rotation.getInput() != 1 ? 180F : (normal ? 135 : -135);
                    float forwardYaw = mc.thePlayer.rotationYaw - (hardcodedYaw() - by);
                    if (mc.thePlayer.onGround) {
                        getSmooth = lastYawS = (mc.thePlayer.rotationYaw - hardcodedYaw());
                        jump = true;
                        rt++;
                        rotationDelay = 3;
                        over45 = quad > 45;
                    }
                    e.setYaw(forwardYaw);
                    lockRotation = true;
                    wasForward = true;
                    rotatingForward = true;
                    canSprint = true;
                    blockRotations = null;
                    theYaw = forwardYaw;
                    back = 2;
                    bvs = false;
                    b1t = 0;
                //}
                firstJump = true;

                /*if (ModuleUtils.inAirTicks == 1 && Utils.scaffoldDiagonal(false)) {
                    rotationDelay = airTickVal;
                }*/

            }
        }
        else {
            rotatingForward = false;
        }

        if (jump && mc.thePlayer.onGround) {
            mc.thePlayer.setSprinting(true);
        }

        if (!Settings.movementFix.isToggled() && mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionZ == 0.0D) {
            if (blockRotations != null) {
                e.setYaw(blockRotations[0]);
            }
        }

        if (ModuleManager.tower.isVerticalTowering()) {
            if (blockRotations != null && (!getVTR || ModuleManager.tower.ebDelay <= 1 || !ModuleManager.tower.firstVTP)) {
                VTRY = blockRotations[0];
                getVTR = true;
            }
            if (getVTR) {
                e.setYaw(VTRY);
                back = 2;
            }
            if (ModuleManager.tower.yaw != 0) {
                e.setYaw(ModuleManager.tower.yaw);
                back = 2;
            }
            if (ModuleManager.tower.pitch != 0) {
                e.setPitch(ModuleManager.tower.pitch);
            }
        }
        else {
            getVTR = false;
        }

        if (back > 0) {
            back--;
        }

        lastey = theYaw;
    }

    private int back;
    public int b1t;
    private float lrnf, offsetvv = 45F;
    private boolean bvs;
    private int snapDelay;
    private int btm;
    private int scycle, sv;
    private long vdl;
    private boolean t7;

    private void simpleRots(ClientRotationEvent e) {
        /*if ((fastScaffoldKeepY || ModuleManager.tower.canTower()) && Utils.scaffoldDiagonal(false)) {
            yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
            if (back > 0 && blockRotations != null) {
                //yaw = blockRotations[0];
            }
        }
        else {*/
            float relativeYaw = mc.thePlayer.rotationYaw - hardcodedYaw() - 180F;
            float normalizedYaw = (relativeYaw % 360 + 360) % 360;
            float quad = normalizedYaw % 90;
            float rotOffset = 0;
            float ofv = -45;
            float d = correct - (mc.thePlayer.rotationYaw - hardcodedYaw());
            float f = 85;
            if ((fastScaffoldKeepY || ModuleManager.tower.hasT) && jumpFacingForward.isToggled()) {
                //if (over45) { 11
                float oval = 18.5f;
                if (!hasPlaced) {
                    placedb = true;
                }
                if (normal) {
                    RotationHelper.get().yawOffset = oval;
                    if (ModuleUtils.inAirTicks <= 2) {
                        ofv = -oval;
                    } else {
                        ofv = oval;
                    }
                }
                else {
                    RotationHelper.get().yawOffset = -oval;
                    if (ModuleUtils.inAirTicks <= 2) {
                        ofv = oval;
                    } else {
                        ofv = -oval;
                    }
                }
                    /*Utils.print(">45");
                }
                else {
                    RotationHelper.get().yawOffset = -16.3F;
                    if (ModuleUtils.inAirTicks <= 2) {
                        ofv = 12;
                    }
                    Utils.print("<45");
                }*/
                setYawOffset = true;
                offsetvv = ofv;
            }
            else {
                if (setYawOffset) {
                    RotationHelper.get().yawOffset = 0F;
                    setYawOffset = false;
                }
                rt = 1;

                /*if (dynamic > 0 && vdl == 0 || !hasPlaced) {
                    if (quad >= 6 && quad <= 84 || !hasPlaced && !mc.thePlayer.onGround || mc.thePlayer.hurtTime > 0) {
                        */offsetvv = 0;
                        /*vdl = Utils.time();
                    }
                    else {
                        Utils.print(d);
                        if (d <= 0) {
                            if (offsetvv != -ofv) {
                                offsetvv = -ofv;
                                vdl = Utils.time();
                            }
                        } else {
                            if (offsetvv != ofv) {
                                offsetvv = ofv;
                                vdl = Utils.time();
                            }
                        }
                    }
                }*/
            }
            //if (dynamic > 0 || !reqrp || blockRotations == null) {
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - offsetvv;
                reqrp = false;

                if ((fastScaffoldKeepY || ModuleManager.tower.hasT) && jumpFacingForward.isToggled()) {
                    getSmooth = (mc.thePlayer.rotationYaw - hardcodedYaw());
                    float yawDifference = Utils.getAngleDifference(lastYawS, getSmooth);
                    float smoothingFactor = 0.025f;
                    getSmooth = (lastYawS + yawDifference * smoothingFactor);
                    lastYawS = getSmooth;
                    smoothedYaw = getSmooth;
                    yaw = smoothedYaw - offsetvv;
                }
            //}
            /*if (blockRotations != null && (!(fastScaffoldKeepY || ModuleManager.tower.hasT) && (blockRotations[1] < 60F || !mc.thePlayer.onGround && scaffoldTicks <= 5) || mc.thePlayer.hurtTime > 0 || !ModuleManager.tower.canTower() && (mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionZ == 0.0D))) {
                yaw = blockRotations[0];
                reqrp = true;
            }*/
        //}

        if (vdl > 0 && (Utils.time() - vdl) > 250) {
            vdl = 0;
        }


        if (canSprint2) {
            b1t++;
            if (b1t == 1) {
                began = blink = true;
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - offsetvv;
            }
            else if (b1t <= 2) {
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - 135;
            }
            else {
                b1t = 0;
            }

            if (mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionY == -0.0784000015258789 && mc.thePlayer.motionZ == 0.0D || placeIdle > 8) {
                btm++;
                if (btm >= 19) {
                    began = blink = false;
                }

            }
            else {
                btm = 0;
            }
        }
        else {
            if (began) {
                began = blink = false;
            }
        }


        if (blockRotations != null) {
            pitch = blockRotations[1];
        } else {
            /*if (rotationDelay <= 1) {
                pitch = 79F;
            }
            else if (rotationDelay == 2) {
                pitch = 77F;
            }
            else if (rotationDelay == 3) {
                pitch = 75F;
            }
            else {
                pitch = 73F;
            }*/
            pitch = 78F;
        }
        if (!cantRotate) {
            e.setRotations(yaw, pitch);
            theYaw = yaw;
        }
        if (b1t == 2) {
            mc.thePlayer.setSprinting(false);
            BlinkHandler.release();
        }
    }

    Vec3 getBestFacing(Vec3 playerVec, Vec3 blockPos) {
        double dx = blockPos.xCoord + 0.5 - playerVec.xCoord;
        double dz = blockPos.zCoord + 0.5 - playerVec.zCoord;

        if (Math.abs(dx) > Math.abs(dz)) {
            if (dx > 0) {
                return new Vec3(-1, 0, 0);
            }
            else {
                return new Vec3(1, 0, 0);
            }
        }
        else {
            if (dz > 0) {
                return new Vec3(0, 0, -1);
            }
            else {
                return new Vec3(0, 0, 1);
            }
        }
    }

    private float lastey;
    private void offsetRots(ClientRotationEvent e) {
        float moveAngle = (float) getMovementAngle();
        float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
        float normalizedYaw = (relativeYaw % 360 + 360) % 360;
        float quad = normalizedYaw % 90;

        float side = MathHelper.wrapAngleTo180_float(getMotionYaw() - yaw);
        float yawBackwards = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - blockYaw);

        long strokeDelay = 250;

        float first = 77F;
        float sec = first;

        if (quad <= 5 || quad >= 85) {
            yawAngle = 121.525F;
            minOffset = 11;
            minPitch = first;
        }
        if (quad > 5 && quad <= 15 || quad >= 75 && quad < 85) {
            yawAngle = 123.425F;
            minOffset = 9;
            minPitch = first;
        }
        if (quad > 15 && quad <= 25 || quad >= 65 && quad < 75) {
            yawAngle = 127.425F;
            minOffset = 8;
            minPitch = first;
        }
        if (quad > 25 && quad <= 32 || quad >= 58 && quad < 65) {
            yawAngle = 131.325F;
            minOffset = 7;
            minPitch = sec;
        }
        if (quad > 32 && quad <= 38 || quad >= 52 && quad < 58) {
            yawAngle = 133.525F;
            minOffset = 6;
            minPitch = sec;
        }
        if (quad > 38 && quad <= 42 || quad >= 48 && quad < 52) {
            yawAngle = 135.825F;
            minOffset = 4;
            minPitch = sec;
        }
        if (quad > 42 && quad <= 45 || quad >= 45 && quad < 48) {
            yawAngle = 138.625F;
            minOffset = 3;
            minPitch = sec;
        }
        //float offsetAmountD = ((((float) offsetAmount.getInput() / 10) - 10) * -2) - (((float) offsetAmount.getInput() / 10) - 10);
        //yawAngle += offsetAmountD;

        float offset = yawAngle;//(!Utils.scaffoldDiagonal(false)) ? 125.500F : 143.500F;


        float nigger = 0;

        if (quad > 45) {
            nigger = 10;
        }
        else {
            nigger = -10;
        }
        if (switchvl > 0) {
            /*if (vlS > 0 && (Utils.time() - vlS) > strokeDelay && firstStroke == 0) {
                switchvl = 0;
                vlS = 0;
            }*/
            //if (switchvl > 0) {
                firstStroke = Utils.time();
                switchvl = 0;
                vlS = 0;
                resetm = true;
            //}
        }
        else {
            vlS = Utils.time();
        }
        if (firstStroke > 0 && (Utils.time() - firstStroke) > strokeDelay) {
            firstStroke = 0;
        }
        if (Utils.fallDist() <= 2 && Utils.getHorizontalSpeed() > 0.1) {
            enabledOffGround = false;
        }
        if (enabledOffGround) {
            if (blockRotations != null) {
                yaw = blockRotations[0];
                pitch = blockRotations[1];
            } else {
                yaw = mc.thePlayer.rotationYaw - hardcodedYaw() - nigger;
                pitch = minPitch;
            }
            e.setRotations(yaw, pitch);
            return;
        }

        if (blockRotations != null) {
            blockYaw = blockRotations[0];
            pitch = blockRotations[1];
            yawOffset = blockYawOffset;
            if (pitch < minPitch) {
                pitch = minPitch;
            }
        } else {
            pitch = minPitch;
            if (edge == 1 && ((quad <= 3 || quad >= 87) && !Utils.scaffoldDiagonal(false))) {
                firstStroke = Utils.time();
            }
            yawOffset = 5;
            dynamic = 2;
        }

        if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
            e.setRotations(theYaw, pitch);
            return;
        }

        float motionYaw = getMotionYaw();

        float newYaw = motionYaw - offset * Math.signum(
                MathHelper.wrapAngleTo180_float(motionYaw - yaw)
        );
        yaw = MathHelper.wrapAngleTo180_float(newYaw);

        if (quad > 3 && quad < 87 && dynamic > 0) {
            if (quad < 45F) {
                if (firstStroke == 0) {
                    if (side >= 0) {
                        set2 = false;
                    } else {
                        set2 = true;
                    }
                }
                if (was452) {
                    switchvl++;
                }
                was451 = true;
                was452 = false;
            } else {
                if (firstStroke == 0) {
                    if (side >= 0) {
                        set2 = true;
                    } else {
                        set2 = false;
                    }
                }
                if (was451) {
                    switchvl++;
                }
                was452 = true;
                was451 = false;
            }
        }
        double minSwitch = (!Utils.scaffoldDiagonal(false)) ? 9 : 15;
        if (side >= 0) {
            if (yawOffset <= -minSwitch && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (set2) {
                        switchvl++;
                    }
                    set2 = false;
                }
            } else if (yawOffset >= 0 && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (yawOffset >= minSwitch) {
                        if (!set2) {
                            switchvl++;
                        }
                        set2 = true;
                    }
                }
            }
            if (set2) {
                if (yawOffset <= -0) yawOffset = -0;
                if (yawOffset >= minOffset) yawOffset = minOffset;
                theYaw = (yaw + offset * 2) - yawOffset;
                e.setRotations(theYaw, pitch);
                return;
            }
        } else if (side <= -0) {
            if (yawOffset >= minSwitch && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (set2) {
                        switchvl++;
                    }
                    set2 = false;
                }
            } else if (yawOffset <= 0 && firstStroke == 0 && dynamic > 0) {
                if (quad <= 3 || quad >= 87) {
                    if (yawOffset <= -minSwitch) {
                        if (!set2) {
                            switchvl++;
                        }
                        set2 = true;
                    }
                }
            }
            if (set2) {
                if (yawOffset >= 0) yawOffset = 0;
                if (yawOffset <= -minOffset) yawOffset = -minOffset;
                theYaw = (yaw - offset * 2) - yawOffset;
                e.setRotations(theYaw, pitch);
                return;
            }
        }

        if (side >= 0) {
            if (yawOffset >= 0) yawOffset = 0;
            if (yawOffset <= -minOffset) yawOffset = -minOffset;
        } else if (side <= -0) {
            if (yawOffset <= -0) yawOffset = -0;
            if (yawOffset >= minOffset) yawOffset = minOffset;
        }
        theYaw = yaw - yawOffset;
        e.setRotations(theYaw, pitch);
    }
    private void preciseRots(ClientRotationEvent e) {
        if (blockRotations != null) {
            yaw = blockRotations[0];
            pitch = blockRotations[1];
        }
        else {
            yaw = mc.thePlayer.rotationYaw - hardcodedYaw();
            pitch = 80F;
        }
        e.setRotations(yaw, pitch);
        theYaw = yaw;
    }

    private boolean canJump() {
        return !ModuleManager.tower.canTower() && !ModuleManager.tower.delay && Utils.jumpDown() || ModuleManager.tower.towerMove.getInput() > 0 && ModuleManager.tower.canTower() && Utils.isMoving() && ModuleManager.tower.disableDiag();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (fastScaffoldKeepY && !floatKeepY && (Settings.movementFix.isToggled() && !jump) && !ModuleManager.tower.delay || canJump()) {
            return;
        }
        mc.thePlayer.movementInput.jump = false;
        if (jump && mc.thePlayer.onGround) {
            canSprint = true;
            if (Settings.movementFix.isToggled()) {
                mc.thePlayer.movementInput.jump = true;
            }
            else {
                mc.thePlayer.jump();
            }
            if (!Settings.movementFix.isToggled()) {
                Utils.setSpeed((getSpeed(getSpeedLevel()) * ModuleUtils.applyFrictionMulti()));
            }
            if (fastScaffold.getInput() == 6 || fastScaffold.getInput() == 3 && firstKeepYPlace) {
                lowhop = true;
            }
        }
        jump = false;
    }

    @SubscribeEvent
    public void onSlotUpdate(SlotUpdateEvent e) {
        if (isEnabled && autoSwap.isToggled()) {
            lastSlot.set(e.slot);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent e) {
        stopUpdate = stopUpdate2 = false;
        if (!isEnabled) {
            stopUpdate2 = true;
        }
        if (LongJump.function) {
            startYPos = -1;
        }
        if (LongJump.stopModules) {
            stopUpdate2 = true;
        }
        if (!stopUpdate2) {
            ModuleUtils.swapTick = 2;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            if (holdingBlocks() && setSlot()) {
                hasSwapped = true;
                /*if (moduleEnabled && !finishProcedure) {
                    if (Utils.distanceToGround(mc.thePlayer) < 2) {
                        finishProcedure = true;
                    }
                    if (Utils.distanceToGround(mc.thePlayer) > 6) {
                        if (hasPlaced) {
                            finishProcedure = true;
                        }
                    } else if (!finishProcedure) {
                        stopUpdate = true;
                    }
                }*/
                if (!stopUpdate) {

                    int mode = (int) fastScaffold.getInput();
                    if (!ModuleManager.tower.placeExtraBlock) {
                        if (rotation.getInput() == 0 || rotationDelay == 0) {
                            placeBlock(0, 0);
                        }
                    } else if ((ModuleManager.tower.ebDelay == 0 || !ModuleManager.tower.firstVTP)) {
                        placeBlock(0, 0);
                        placedVP = true;
                    }
                    if (ModuleManager.tower.placeExtraBlock) {
                        placeBlock(0, -1);
                    }

                    if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                        ++keepYTicks;
                        if ((int) mc.thePlayer.posY > (int) startYPos) {
                            switch (mode) {
                                case 1:
                                    if (!firstKeepYPlace && keepYTicks == 3) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 2:
                                    if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 3:
                                    if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                                case 4:
                                    if (!firstKeepYPlace && keepYTicks == 7) {
                                        placeBlock(1, 0);
                                        firstKeepYPlace = true;
                                    }
                                    break;
                            }
                        }
                        if (mc.thePlayer.onGround) keepYTicks = 0;
                        if ((int) mc.thePlayer.posY == (int) startYPos) firstKeepYPlace = false;
                    }
                    handleMotion();
                }
            }
        }

        if (disabledModule) {
            if (hasPlaced && (towerEdge || floatStarted && Utils.isMoving())) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
            }
            if (!dontDisable) {
                isEnabled = false;
            }


            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;

                if (ModuleManager.tower.speed) {
                    Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
                }

                if (lastSlot.get() != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot.get();
                    lastSlot.set(-1);
                }
                blockSlot = -1;
                if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
                }
                if (offsetDelay > 0) {
                    ModuleManager.sprint.requireJump = false;
                }
                scaffoldBlockCount.beginFade();
                hasSwapped = hasPlaced = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = floatStarted = floatJumped = floatWasEnabled = towerEdge =
                        was451 = was452 = enabledOffGround = finishProcedure = jump = blink = canSprint = canSprint2 = idle = didJump = firstRotate = bvs = began = cantRotate =
                                startRotation = lockRotation = firstJump = didLastRotation = reqrp = placedb = false;
                rotationDelay = keepYTicks = scaffoldTicks = floatTicks = idleTicks = frd = back = b1t = canSnap = btm = snapDelay = srt = placeIdle = fjDelay = 0;
                rt = 1;
                forwardTicks = 0;
                wasForward = false;
                jumpDelay = 0;
                canForward = false;
                firstStroke = vlS = 0;
                startYPos = -1;
                lookVec = null;
                lastPlacement = null;
                if (setYawOffset) {
                    RotationHelper.get().yawOffset = 0F;
                    setYawOffset = false;
                }
                correct = -180F;
            }
        }
    }

    @Override
    public String getInfo() {
        String s;
        if (sprint.getInput() > 0) {
            s = sprintModes[(int) sprint.getInput()];
        }
        else {
            s = rotationModes[(int) rotation.getInput()];
        }
        String info;
        if (fastOnRMB.isToggled()) {
            info = fastOnRMB() ? fastScaffoldModes[(int) fastScaffold.getInput()] : s;
        }
        else {
            info = fastScaffold.getInput() > 0 ? fastScaffoldModes[(int) fastScaffold.getInput()] : s;
        }
        return info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    public boolean sprint() {
        return isEnabled && (canSprint || canSprint2);
    }

    public void rotateForward(boolean delay) {
        if (jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (!rotatingForward) {
                    if (delay) {
                        rtd = 5;
                    }
                    else {
                        rtd = 0;
                    }
                }
                rotateForward = true;
            }
        }
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    private boolean usingFloat() {
        return sprint.getInput() == 2 && Utils.isMoving() && !usingFastScaffold();
    }

    private boolean sprintScaf() {
        return sprint.getInput() > 0 && Utils.isMoving() && mc.thePlayer.onGround && !usingFastScaffold() && !ModuleManager.tower.canTower();
    }

    public boolean usingFastScaffold() {
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || fastOnRMB() && Utils.tabbedIn()) && !prioritizeSprint();
    }

    public boolean fastOnRMB() {
        return fastOnRMB.isToggled() && Utils.tabbedIn() && (Mouse.isButtonDown(1) || ModuleManager.bhop.isEnabled() || defPS());
    }

    private boolean defPS() {
        return prioritizeSprintWithSpeed.isToggled() && (sprint.getInput() == 0 || getSpeedLevel() == 0);
    }

    private boolean prioritizeSprint() {
        return prioritizeSprintWithSpeed.isToggled() && sprint.getInput() > 0 && getSpeedLevel() > 0 && !fastOnRMB();
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled();
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData block) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                if (holdingBlocks()) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            if (ModuleManager.tower.placeExtraBlock) {
                ModuleManager.tower.firstVTP = true;
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
            hasPlaced = true;
            placeIdle = 0;
        }
    }

    public boolean canSafewalk() {
        if (!safeWalk.isToggled()) {
            return false;
        }
        if (usingFastScaffold()) {
            return false;
        }
        if (ModuleManager.tower.canTower()) {
            return false;
        }
        if (!isEnabled) {
            return false;
        }
        return true;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private void placeBlock(int yOffset, int xOffset) {
        locateAndPlaceBlock(yOffset, xOffset);
        int input = (int) multiPlace.getInput();
        /*if (multiPlaceOnlyNecessary.isToggled()) {
            if (!(mc.thePlayer.hurtTime > 0 || scaffoldTicks <= 2)) {
                return;
            }
        }*/
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
                if (input >= 3) {
                    locateAndPlaceBlock(yOffset, xOffset);
                    if (input >= 4) {
                        locateAndPlaceBlock(yOffset, xOffset);
                    }
                }
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        lastPlacement = blockInfo;
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        // Sort by distance to player's feet
        BlockPos playerFeet = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        blocksInfo.sort(Comparator.comparingDouble(b -> b.blockPos.distanceSq(playerFeet)));

        double sumX = 0, sumY = 0, sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1) break;
            sumX += blockssInfo.blockPos.getX();
            sumY += blockssInfo.blockPos.getY();
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = sumY / index;
        double avgZ = sumZ / index;

        targetBlock = new Vec3(avgX, avgY, avgZ);

        PlaceData blockInfo2 = blocksInfo.get(0);
        int blockX = blockInfo2.blockPos.getX();
        int blockY = blockInfo2.blockPos.getY();
        int blockZ = blockInfo2.blockPos.getZ();
        EnumFacing blockFacing = blockInfo2.enumFacing;
        blockInfo = blockInfo2;

        double hitX = (blockX + 0.5D) + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double hitY = (blockY + 0.5D) + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double hitZ = (blockZ + 0.5D) + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D,
                0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D,
                0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
        hitVec = new Vec3(hitX, hitY, hitZ);
        blockInfo.hitVec = hitVec;
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x": return (facing == EnumFacing.WEST) ? -0.5 : (facing == EnumFacing.EAST) ? 0.5 : 0;
            case "y": return (facing == EnumFacing.DOWN) ? -0.5 : (facing == EnumFacing.UP) ? 0.5 : 0;
            case "z": return (facing == EnumFacing.NORTH) ? -0.5 : (facing == EnumFacing.SOUTH) ? 0.5 : 0;
        }
        return 0;
    }

    private static double distance = 3.5;

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        distance = 3.5;
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : mc.thePlayer.posY) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        BlockPos base = new BlockPos(x, y - 1, z);

        if (!BlockUtils.replaceable(base)) {
            return null;
        }

        EnumFacing[] allFacings = getFacingsSorted();
        List<EnumFacing> validFacings = new ArrayList<>(5);
        for (EnumFacing facing : allFacings) {
            if (facing != EnumFacing.UP && placeConditions(facing, yOffset, xOffset)) {
                validFacings.add(facing);
            }
        }

        List<PlaceData> possibleBlocks = new ArrayList<>();

        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        Map<BlockPos, Integer> distances = new HashMap<>();

        queue.offer(base);
        visited.add(base);
        distances.put(base, 0);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int currentDistance = distances.get(current);

            if (currentDistance >= distance) {
                continue;
            }

            for (EnumFacing facing : validFacings) {
                BlockPos neighbor = current.offset(facing);

                if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                    possibleBlocks.add(new PlaceData(neighbor, facing.getOpposite()));
                } else if (BlockUtils.replaceable(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    distances.put(neighbor, currentDistance + 1);
                    queue.offer(neighbor);
                }
            }
        }

        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private EnumFacing[] getFacingsSorted() {
        EnumFacing lastFacing = EnumFacing.getHorizontal(MathHelper.floor_double((((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() * 4.0F / 360.0F) + 0.5D) & 3);

        EnumFacing perpClockwise = lastFacing.rotateY();
        EnumFacing perpCounterClockwise = lastFacing.rotateYCCW();

        EnumFacing opposite = lastFacing.getOpposite();

        float yaw = ((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() % 360;
        if (yaw > 180) {
            yaw -= 360;
        }
        else if (yaw < -180) {
            yaw += 360;
        }

        // Calculates the difference from the last placed angle and gets the closest one
        float diffClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpClockwise)));
        float diffCounterClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - getFacingAngle(perpCounterClockwise)));

        EnumFacing firstPerp, secondPerp;
        if (diffClockwise <= diffCounterClockwise) {
            firstPerp = perpClockwise;
            secondPerp = perpCounterClockwise;
        }
        else {
            firstPerp = perpCounterClockwise;
            secondPerp = perpClockwise;
        }

        return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
    }

    private float getFacingAngle(EnumFacing facing) {
        switch (facing) {
            case WEST:
                return 90;
            case NORTH:
                return 180;
            case EAST:
                return -90;
            default:
                return 0;
        }
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            if (!ModuleManager.tower.placeExtraBlock) {
                return enumFacing == EnumFacing.EAST;
            }
            else {
                return enumFacing == EnumFacing.DOWN;
            }
        }
        else if (ModuleManager.tower.placeExtraBlock) {
            return enumFacing == EnumFacing.WEST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }
        return true;
    }

    float applyGcd(float value) {
        float gcd = 0.2F * 0.2F * 0.2F * 8.0F;
        return (float) ((double) value - (double) value % ((double) gcd * 0.15D));
    }

    public float getMotionYaw() {
        return (float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F;
    }

    public int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    public int getJumpLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.jump")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    public double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.27, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        double min = 0;
        double value = 0;
        double input = (motion.getInput() / 100);
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        value = floatSpeedLevels[speedLevel] - min;
        if (speedLevel == 2) {
            value = (Utils.scaffoldDiagonal(false) ? 0.255 : 0.265) - min;
        }
        value *= input;
        return value;
    }

    private void handleMotion() {
        if (usingFastScaffold() || usingFloat() || ModuleManager.tower.canTower() || motion.getInput() == 100 || !mc.thePlayer.onGround) {
            return;
        }
        double input = (motion.getInput() / 100);
        mc.thePlayer.motionX *= input;
        mc.thePlayer.motionZ *= input;
    }

    private float finalhYaw;
    public float hardcodedYaw() {
        float simpleYaw = 0;
        boolean w = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
        boolean s = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
        boolean a = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
        boolean d = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());

        boolean dupe = a & d;

        //if (!lockRotation) {
            if (w) {
                simpleYaw -= 180;
                if (!dupe) {
                    if (a) simpleYaw += 45;
                    if (d) simpleYaw -= 45;
                }
            } else if (!s) {
                simpleYaw -= 180;
                if (!dupe) {
                    if (a) simpleYaw += 90;
                    if (d) simpleYaw -= 90;
                }
            } else if (!w) {
                if (!dupe) {
                    if (a) simpleYaw -= 45;
                    if (d) simpleYaw += 45;
                }
            }
            finalhYaw = simpleYaw;
        //}
        return finalhYaw;
    }

    public boolean holdingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
    }

    private double getMovementAngle() {
        double angle = Settings.movementFix.isToggled() ? Math.toDegrees(Math.atan2(-Settings.fixedStrafe, Settings.fixedForward)) : Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0 ? 0 : angle;
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
                if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
                    continue;
                }
                if (itemStack.stackSize > highestStack) {
                    highestStack = itemStack.stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public static boolean bypassRots() {
        return (ModuleManager.scaffold.rotation.getInput() == 2 || ModuleManager.scaffold.rotation.getInput() == 0);
    }

    public boolean setSlot() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        int slot = getSlot();
        if (slot == -1) {
            return false;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
                mc.thePlayer.inventory.currentItem = slot;
                spoofSlot = slot;
            }
            else {
                if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem()) || mc.thePlayer.getHeldItem().stackSize <= ModuleManager.autoSwap.swapAt.getInput()) {
                    mc.thePlayer.inventory.currentItem = slot;
                    spoofSlot = slot;
                }
            }
        }

        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return false;
        }
        return true;
    }


    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;
        Vec3 hitVec;

        PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }

        public PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}