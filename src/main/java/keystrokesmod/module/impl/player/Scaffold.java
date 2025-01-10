package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixins.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.other.ViewPackets;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private static SliderSetting motion;
    public static SliderSetting rotation;
    private static SliderSetting sprint;
    private static SliderSetting fastScaffold;
    private static SliderSetting multiPlace;
    public static ButtonSetting autoSwap;
    private static ButtonSetting cancelKnockBack;
    private static ButtonSetting fastOnRMB;
    public static ButtonSetting highlightBlocks;
    private static ButtonSetting jumpFacingForward;
    public static ButtonSetting safeWalk;
    public static ButtonSetting showBlockCount;
    private static ButtonSetting silentSwing;
    public static ButtonSetting tower;

    private static String[] rotationModes = new String[] { "None", "Simple", "Offset", "Precise" };
    private static String[] sprintModes = new String[] { "None", "Vanilla", "Float" };
    private static String[] fastScaffoldModes = new String[] { "None", "Jump B", "Jump B Low", "Jump E", "Keep-Y", "Keep-Y Low" };
    private String[] multiPlaceModes = new String[] { "Disabled", "1 extra", "2 extra" };

    public Map<BlockPos, Timer> highlight = new HashMap<>();

    private ScaffoldBlockCount scaffoldBlockCount;

    public AtomicInteger lastSlot = new AtomicInteger(-1);

    public boolean hasSwapped;

    private boolean rotateForward;
    private int keepYDelay;
    private int onGroundTicks;
    private int keepYPlaceTicks;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    private boolean lowhop;
    private int rotationDelay;
    private int blockSlot = -1;
    private int inAirTicks;
    private boolean modifyPitch;

    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;

    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 blockPos, hitVec, lookVec;
    private float[] blockRotations;
    private long lastPlaceTime, rotationTimeout = 250L;
    private float getPitch;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private float lastBlockYaw;

    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable;
    private int disableTicks;

    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", 0, sprintModes));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));
        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));
        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(cancelKnockBack = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.alwaysOn = true;
    }

    public void onDisable() {
        disabledModule = true;
        moduleEnabled = false;
    }

    public void onEnable() {
        isEnabled = true;
        moduleEnabled = true;

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(mc));
        lastSlot.set(-1);
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        if (e.button >= 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        inAirTicks = mc.thePlayer.onGround ? 0 : ++inAirTicks;
        onGroundTicks = !mc.thePlayer.onGround ? 0 : ++onGroundTicks;
        if (!isEnabled || !holdingBlocks()) {
            return;
        }
        int simpleY = (int) Math.round((e.posY % 1) * 10000);
        if (Utils.keysDown() && usingFastScaffold() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !ModuleManager.LongJump.isEnabled()) { // jump mode
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                rotateForward = true;
                if (++keepYDelay >= 2 || keepYPlaceTicks > 0 || onGroundTicks > 1) {
                    mc.thePlayer.jump();
                    Utils.setSpeed(getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.001, 0.0001));
                    if (fastScaffold.getInput() == 5 || fastScaffold.getInput() == 2 && firstKeepYPlace) {
                        lowhop = true;
                    }
                    //Utils.print("Keepy");
                    if (startYPos == -1 || Math.abs(startYPos - e.posY) > 5) {
                        startYPos = e.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYDelay = keepYTicks = keepYPlaceTicks = 0;
        }
        if (lowhop) {
            switch (simpleY) {
                case 4200:
                    mc.thePlayer.motionY = 0.39;
                    break;
                case 1138:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                    break;
                case 2031:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                    lowhop = false;
                    break;
            }
        }

        //Float
        if (sprint.getInput() == 2 && !usingFastScaffold() && !ModuleManager.tower.canTower() && !ModuleManager.LongJump.isEnabled()) {
            floatWasEnabled = true;
            if (!floatStarted) {
                if (onGroundTicks > 8 && mc.thePlayer.onGround) {
                    floatKeepY = true;
                    startYPos = e.posY;
                    mc.thePlayer.jump();
                    floatJumped = true;
                } else if (onGroundTicks <= 8 && mc.thePlayer.onGround) {
                    floatStarted = true;
                }
                if (floatJumped && !mc.thePlayer.onGround) {
                    floatStarted = true;
                }
            }

            if (floatStarted && mc.thePlayer.onGround) {
                floatKeepY = false;
                startYPos = -1;
                if (moduleEnabled) e.setPosY(e.getPosY() + 1E-10);
                if (Utils.isMoving()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
            }
        } else if (floatWasEnabled) {
            if (floatKeepY) {
                startYPos = -1;
            }
            floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
        }


        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            targetBlock = null;
        }

        getPitch = 82;
        if (inAirTicks >= 2) {
            rotateForward = false;
        }
        if (rotation.getInput() > 0 && (!rotateForward || !jumpFacingForward.isToggled())) {
            rotatingForward = false;
            if (rotation.getInput() > 0) {
                if (blockRotations != null) {
                    e.setYaw(blockRotations[0]);
                    e.setPitch(blockRotations[1]);

                    if (rotation.getInput() == 2) {
                        e.setYaw(offsetRotation());
                        if (modifyPitch) {
                            //e.setPitch(e.getPitch() + 5);
                            //Utils.print("Modifying pitch");
                        }
                        if (e.getPitch() >= 60 && !ModuleManager.tower.canTower() && mc.thePlayer.motionY <= 0.42F) {
                            e.setPitch(e.getPitch() + 9);
                        }
                    }

                    if (rotation.getInput() == 1) {
                        e.setYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw());
                    }

                    if (e.getPitch() > 89) {
                        e.setPitch(89);
                    }
                    lastYaw = e.getYaw();
                    lastPitch = e.getPitch();
                    if (lastPlaceTime > 0 && (System.currentTimeMillis() - lastPlaceTime) > rotationTimeout) blockRotations = null;
                }
                else {
                    if (rotation.getInput() == 2) {
                        e.setYaw(offsetRotation());
                    }
                    else {
                        e.setYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw());
                    }
                    e.setPitch(getPitch);
                }
            }
        }
        else {
            if (rotation.getInput() > 0) {
                e.setYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw() - 180 - (float)  Utils.randomizeDouble(-5, 5));
                e.setPitch(10 - (float) Utils.randomizeDouble(1, 5));
            }
            if (!rotatingForward) {
                rotationDelay = 2;
                rotatingForward = true;
            }
        }
        if (rotationDelay > 0) --rotationDelay;
    }

    private float yaw;
    float newYaw;
    float mainOffset;

    float lastNigger = 0;
    int difference = 0;

    private float offsetRotation() {
        float yawBackwards = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        float motionYaw = getMotionYaw();
        float main = MathHelper.wrapAngleTo180_float(motionYaw - yaw);
        if (blockRotations != null) {
            mainOffset = MathHelper.wrapAngleTo180_float(yawBackwards - lastBlockYaw);
            lastBlockYaw = MathHelper.wrapAngleTo180_float(blockRotations[0]);

            if (main >= 0) {
                if (mainOffset >= 10) {
                    modifyPitch = true;
                }
                else modifyPitch = false;
                //Utils.print("Main1");
                if (mainOffset >= 0) mainOffset = 0;
                if (mainOffset <= -30) mainOffset = -30;
            }
            if (main <= -0) {
                if (mainOffset <= -10) {
                    modifyPitch = true;
                }
                else modifyPitch = false;
                //Utils.print("Main2");
                if (mainOffset <= -0) mainOffset = -0;
                if (mainOffset >= 30) mainOffset = 30;
            }

            //Utils.print("" + mainOffset);

            //Utils.print("" + difference);
        }
        else {
            lastBlockYaw = yaw;
            if (main >= 0) {
                if (mainOffset >= 0) mainOffset = -10;
            }
            if (main <= -0) {
                if (mainOffset <= -0) mainOffset = 10;
            }
            //Utils.print("No offset");
            difference = 0;
        }
        newYaw = motionYaw - (!Utils.scaffoldDiagonal(false) ? 122.625F : 142.625F) * Math.signum(
                main
        );
        yaw = applyGcd(MathHelper.wrapAngleTo180_float(newYaw) - mainOffset);

        //yaw += 180 = opposite yaw

        /*double min = 25;
        if (lastNigger > min) {
            yaw += 180;
            Utils.print("(Positive) Yaw switched " + lastNigger);
        }
        else if (lastNigger < -min) {
            yaw -= 180;
            Utils.print("(Negative) Yaw switched " + lastNigger);
        }*/

        lastNigger = MathHelper.wrapAngleTo180_float(lastBlockYaw - yawBackwards);
        return yaw;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!isEnabled) {
            return;
        }
        if (LongJump.stopScaffold) {
            //Utils.print("Stopped scaffold due to longjump swapping");
            return;
        }
        if (ModuleManager.killAura.isTargeting || ModuleManager.killAura.justUnTargeted) {
            //Utils.print("Stopped scaffold due to ka untargeting");
            return;
        }
        if (holdingBlocks() && setSlot()) {
            hasSwapped = true;
            int mode = (int) fastScaffold.getInput();
            if (rotation.getInput() == 0 || rotation.getInput() == 2 || (rotation.getInput() > 0 && (rotationDelay == 0 && !rotateForward || !jumpFacingForward.isToggled()))) {
                placeBlock(0, 0);
            }
            if (ModuleManager.tower.placeExtraBlock) {
                placeBlock(0, -1);
            }
            if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                ++keepYTicks;
                if ((int) mc.thePlayer.posY > (int) startYPos) {
                    switch (mode) {
                        case 1:
                            if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 2:
                            if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 3:
                            if (!firstKeepYPlace && keepYTicks == 8 || Utils.scaffoldDiagonal(false) && keepYTicks == 11) {
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

        if (disabledModule) {
            if (ModuleManager.tower.canTower() && ModuleManager.tower.dCount == 0 || floatStarted) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
                //Utils.print("Extra tick");
            }
            if (!dontDisable) {
                isEnabled = false;
            }


            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;
                //Utils.print("Disabled");

                if (ModuleManager.tower.speed) {
                    Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 2);
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
                scaffoldBlockCount.beginFade();
                hasSwapped = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                startYPos = -1;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = lowhop = floatStarted = floatJumped = floatWasEnabled = false;
                inAirTicks = keepYDelay = rotationDelay = keepYTicks = keepYPlaceTicks = 0;
                startYPos = -1;
                lookVec = null;
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!isEnabled) {
            return;
        }
        if (!Utils.nullCheck() || !cancelKnockBack.isToggled()) {
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

    @Override
    public String getInfo() {
        return fastScaffoldModes[handleFastScaffolds()];
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public boolean sprint() {
        if (isEnabled) {
            return handleFastScaffolds() > 0 || !holdingBlocks();
        }
        return false;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return Mouse.isButtonDown(1) && Utils.tabbedIn() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        else {
            return (int) fastScaffold.getInput();
        }
    }

    private boolean usingFastScaffold() {
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1) && Utils.tabbedIn());
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
                if (!(autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled())) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
        }
    }

    public boolean canSafewalk() {
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

    private boolean canGetBlock() {
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
                    //Utils.print("Got block");
                    return true;
                }
            }
        }
        return false;
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
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        double sumX = 0, sumY = !mc.thePlayer.onGround ? 0 : blocksInfo.get(0).blockPos.getY(), sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || (!Utils.isDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += blockssInfo.blockPos.getY();
            }
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
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
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
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

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        List<PlaceData> possibleBlocks = new ArrayList<>();
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : (mc.thePlayer.posY)) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        if (BlockUtils.replaceable(new BlockPos(x, y - 1, z))) {
            for (EnumFacing enumFacing : EnumFacing.values()) {
                if (enumFacing != EnumFacing.UP && placeConditions(enumFacing, yOffset, xOffset)) {
                    BlockPos offsetPos = new BlockPos(x, y - 1, z).offset(enumFacing);
                    if (!BlockUtils.replaceable(offsetPos)) {
                        possibleBlocks.add(new PlaceData(offsetPos, enumFacing.getOpposite()));
                    }
                }
            }
            for (EnumFacing enumFacing2 : EnumFacing.values()) {
                if (enumFacing2 != EnumFacing.UP && placeConditions(enumFacing2, yOffset, xOffset)) {
                    BlockPos offsetPos2 = new BlockPos(x, y - 1, z).offset(enumFacing2);
                    if (BlockUtils.replaceable(offsetPos2)) {
                        for (EnumFacing enumFacing3 : EnumFacing.values()) {
                            if (enumFacing3 != EnumFacing.UP && placeConditions(enumFacing3, yOffset, xOffset)) {
                                BlockPos offsetPos3 = offsetPos2.offset(enumFacing3);
                                if (!BlockUtils.replaceable(offsetPos3)) {
                                    possibleBlocks.add(new PlaceData(offsetPos3, enumFacing3.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
            if (mc.thePlayer.motionY > -0.0784) {
                for (EnumFacing enumFacing5 : EnumFacing.values()) {
                    if (enumFacing5 != EnumFacing.UP && placeConditions(enumFacing5, yOffset, xOffset)) {
                        BlockPos offsetPos5 = new BlockPos(x, y - 2, z).offset(enumFacing5);
                        if (BlockUtils.replaceable(offsetPos5)) {
                            for (EnumFacing enumFacing6 : EnumFacing.values()) {
                                if (enumFacing6 != EnumFacing.UP && placeConditions(enumFacing6, yOffset, xOffset)) {
                                    BlockPos offsetPos6 = offsetPos5.offset(enumFacing6);
                                    if (!BlockUtils.replaceable(offsetPos6)) {
                                        possibleBlocks.add(new PlaceData(offsetPos6, enumFacing6.getOpposite()));
                                    }
                                }
                            }
                        }
                    }
                }
                for (EnumFacing enumFacing7 : EnumFacing.values()) {
                    if (enumFacing7 != EnumFacing.UP && placeConditions(enumFacing7, yOffset, xOffset)) {
                        BlockPos offsetPos7 = new BlockPos(x, y - 3, z).offset(enumFacing7);
                        if (BlockUtils.replaceable(offsetPos7)) {
                            for (EnumFacing enumFacing8 : EnumFacing.values()) {
                                if (enumFacing8 != EnumFacing.UP && placeConditions(enumFacing8, yOffset, xOffset)) {
                                    BlockPos offsetPos8 = offsetPos7.offset(enumFacing8);
                                    if (!BlockUtils.replaceable(offsetPos8)) {
                                        possibleBlocks.add(new PlaceData(offsetPos8, enumFacing8.getOpposite()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            return null;
        }
        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            return enumFacing == EnumFacing.EAST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }

        return true;
    }

    /*private boolean allowedFaces(EnumFacing enumFacing) {
        if (yaw >= 0 && yaw < 90) {
            Utils.print("1");
            //west south
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.WEST || enumFacing == EnumFacing.SOUTH;
        }
        else if (yaw >= 90 && yaw < 180) {
            Utils.print("2");
            //north west
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.NORTH || enumFacing == EnumFacing.WEST;
        }
        else if (yaw == 180 || yaw >= -180 && yaw < -90) {
            Utils.print("3");
            //north east
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.NORTH || enumFacing == EnumFacing.EAST;
        }
        else if (yaw >= -90 && yaw <= 0) {
            Utils.print("4");
            //east south
            return enumFacing == EnumFacing.DOWN || enumFacing == EnumFacing.EAST || enumFacing == EnumFacing.SOUTH;
        }

        return false;
    }*/

    float applyGcd(float value) {
        float gcd = 0.2F * 0.2F * 0.2F * 8.0F;
        return (float) ((double) value - (double) value % ((double) gcd * 0.15D));
    }

    float getMotionYaw() {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
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

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return floatSpeedLevels[speedLevel];
        }
        return floatSpeedLevels[0];
    }

    private void handleMotion() {
        if (usingFastScaffold()) {
            return;
        }
        mc.thePlayer.motionX *= motion.getInput();
        mc.thePlayer.motionZ *= motion.getInput();
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float f = 0.8F;

        if (mc.thePlayer.moveForward >= f) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 45;
        }
        else if (mc.thePlayer.moveForward == 0) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 90;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 90;
        }
        else if (mc.thePlayer.moveForward <= -f) {
            if (mc.thePlayer.moveStrafing >= f) simpleYaw -= 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw += 45;
        }
        return simpleYaw;
    }

    public boolean holdingBlocks() {
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
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

    public boolean setSlot() {
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
            }
            else {
                mc.thePlayer.inventory.currentItem = blockSlot;
            }
            //Utils.print("set slot?");
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
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
    }
}