package keystrokesmod.module.impl.minigames;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WoolWars extends Module {
    public SliderSetting breakSpeed;
    public SliderSetting range;
    public SliderSetting breakDelay;
    public SliderSetting placeDelay;
    public ButtonSetting onlyMiddleClick;
    public ButtonSetting onlyVisible;

    private final int middlePositionColors = new Color(255, 153, 204).getRGB();
    private final int miningColor = new Color(200, 100, 255).getRGB();
    private final int placeColor = new Color(150, 70, 255).getRGB();

    private BlockPos middlePos;
    private BlockPos miningPos;
    private MovingObjectPosition placeMop;

    private float curBlockDamageMP;
    private int delay;
    private int swapBack = -1;
    private double lastRange;
    private double rangeSq;
    private float placingYaw;
    private float placingPitch;
    private boolean fakeSwing;

    public WoolWars() {
        super("WoolWars", category.minigames, 0);

        this.registerSetting(new DescriptionSetting("Nukes and places at control point."));
        this.registerSetting(breakSpeed = new SliderSetting("Break speed", 0.2, 0.0, 0.8, 0.05));
        this.registerSetting(breakDelay = new SliderSetting("Delay after breaking", 3.0, 1.0, 10.0, 1.0));
        this.registerSetting(placeDelay = new SliderSetting("Delay after placing", 1.0, 1.0, 10.0, 1.0));
        this.registerSetting(range = new SliderSetting("Range", 5.0, 1.0, 8.0, 0.5));
        this.registerSetting(onlyVisible = new ButtonSetting("Only visible", true));
        this.registerSetting(onlyMiddleClick = new ButtonSetting("Only while middle clicking", true));
    }

    @Override
    public void guiUpdate() {
        if (lastRange != range.getInput()) {
            lastRange = range.getInput();
            rangeSq = Math.pow(lastRange + 2.0, 2.0);
        }
    }

    @Override
    public void onDisable() {
        swapBack();
        reset();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent event) {
        if (!isWoolWars()) {
            reset();
            return;
        }
        if (middlePos == null) {
            middlePos = getMiddlePos();
        }
        else if (!mc.thePlayer.capabilities.allowFlying && mc.thePlayer.getDistanceSq(middlePos) < rangeSq && isActiveRound() && (!onlyMiddleClick.isToggled() || Mouse.isButtonDown(2))) {
            if (swapBack == -1) {
                swapBack = mc.thePlayer.inventory.currentItem;
            }
            if (delay > 0 && --delay > 0) {
                if (fakeSwing) {
                    mc.thePlayer.swingItem();
                }
                return;
            }
            if (placeMop != null) {
                return;
            }
            if (miningPos == null) {
                List<BlockPos> posList = getPossiblePos(middlePos, true);
                if (!posList.isEmpty()) {
                    BlockPos closestPos = getClosestPos(posList, true);
                    if (closestPos != null) {
                        int blockSlot = getBlockSlot();
                        if (blockSlot == -1) {
                            return;
                        }
                        Utils.switchSlot(blockSlot, true);
                        search:
                        for (int i = 0; i < 360; i += 10) {
                            float yaw = (float) (mc.thePlayer.rotationYaw + i + randomRotationOffset());
                            int j = 20;
                            while (j < 90) {
                                float pitch = RotationUtils.clampPitch((float) (j + randomRotationOffset()));
                                MovingObjectPosition mop = Utils.getTarget(lastRange, yaw, pitch);
                                if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && BlockUtils.isBlockPosEqual(BlockUtils.offsetPos(mop), closestPos)) {
                                    placeMop = mop;
                                    placingYaw = yaw;
                                    placingPitch = pitch;
                                    break search;
                                }
                                else {
                                    j += 5;
                                }
                            }
                        }
                        return;
                    }
                }
                posList = getPossiblePos(middlePos, false);
                if (posList.isEmpty()) {
                    middlePos = null;
                    swapBack();
                    return;
                }
                BlockPos closestPos = getClosestPos(posList, false);
                if (closestPos == null) {
                    return;
                }
                miningPos = closestPos;
                switchToSlot(Utils.getTool(BlockUtils.getBlock(closestPos)));
                miningPos = closestPos;
                mc.thePlayer.swingItem();
                startBreak(miningPos);
            }
            else if (!Utils.isPossibleToReach(miningPos, lastRange)) {
                abortBreak(miningPos);
                miningPos = null;
                curBlockDamageMP = (delay = 0);
                return;
            }
            curBlockDamageMP += BlockUtils.getBlockHardness(BlockUtils.getBlock(miningPos), mc.thePlayer.getHeldItem(), false, false);
            if (curBlockDamageMP < breakSpeed.getInput()) {
                curBlockDamageMP = (float) breakSpeed.getInput();
            }
            if (curBlockDamageMP >= 1.0f) {
                stopBreak(miningPos);
                mc.playerController.onPlayerDestroyBlock(miningPos, EnumFacing.UP);
                miningPos = null;
                curBlockDamageMP = 0.0f;
                delay = (int) breakDelay.getInput();
                fakeSwing = true;
            }
            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), miningPos, (int) (curBlockDamageMP * 10.0f) - 1);
            mc.thePlayer.swingItem();
        }
        else if (miningPos != null) {
            abortBreak(miningPos);
            miningPos = null;
            curBlockDamageMP = (delay = 0);
            swapBack();
        }
        else if (swapBack != -1) {
            swapBack();
        }
    }

    public double randomRotationOffset() {
        return Math.random() - 0.5;
    }

    public boolean switchToSlot(int slot) {
        if (slot == -1) {
            return false;
        }
        mc.thePlayer.inventory.currentItem = slot;
        return true;
    }

    private BlockPos getMiddlePos() {
        BlockPos middlePos = null;
        int y;
        int startY;
        for (startY = (y = (int) Math.floor(mc.thePlayer.posY + 20.0)); y > -1; --y) {
            BlockPos pos = BlockUtils.pos(0.0, y, 0.0);
            if (BlockUtils.getBlock(pos.add(0, 0, 2)) instanceof BlockStairs || isControlPointBlock(pos, false)) {
                middlePos = pos;
                break;
            }
        }
        if (middlePos == null) {
            for (y = startY; y > -1; --y) {
                BlockPos pos = BlockUtils.pos(0.0, y, 6.0);
                if (BlockUtils.getBlock(pos.add(0, 0, 2)) instanceof BlockStairs || isControlPointBlock(pos, false)) {
                    middlePos = pos;
                    break;
                }
            }
        }
        return middlePos;
    }

    private List<BlockPos> getPossiblePos(BlockPos middlePos, boolean airOnly) {
        List<BlockPos> posList = new ArrayList<>();
        for (int zOffset = -1; zOffset <= 1; ++zOffset) {
            for (int xOffset = -1; xOffset <= 1; ++xOffset) {
                BlockPos pos = new BlockPos(middlePos.getX() + xOffset, middlePos.getY(), middlePos.getZ() + zOffset);
                if (airOnly) {
                    if (!(BlockUtils.getBlock(pos) instanceof BlockAir)) {
                        continue;
                    }
                } else if (!isControlPointBlock(pos, true)) {
                    continue;
                }
                posList.add(pos);
            }
        }
        return posList;
    }

    private BlockPos getClosestPos(List<BlockPos> posList, boolean down) {
        BlockPos closestPos = null;
        double leastDistSq = rangeSq + 1.0;
        for (BlockPos pos : posList) {
            if (!Utils.isPossibleToReach(down ? pos.down() : pos, lastRange)) {
                continue;
            }
            if (onlyVisible.isToggled() && !BlockUtils.canBlockBeSeen(pos)) {
                continue;
            }
            double distSq = mc.thePlayer.getDistanceSq(pos);
            if (distSq >= leastDistSq) {
                continue;
            }
            leastDistSq = distSq;
            closestPos = pos;
        }
        return closestPos;
    }

    private boolean isControlPointBlock(BlockPos pos, boolean verifyWoolColor) {
        Block block = BlockUtils.getBlock(pos);
        if (block != Blocks.wool) {
            return block == Blocks.snow || block == Blocks.quartz_block;
        }
        if (!verifyWoolColor) {
            return true;
        }
        EnumDyeColor teamColor = null;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() == Blocks.wool) {
                teamColor = EnumDyeColor.byMetadata(stack.getMetadata());
                break;
            }
        }
        return BlockUtils.getWoolColor(BlockUtils.getBlockState(pos)) != teamColor;
    }

    private boolean isActiveRound() {
        for (String line : Utils.getSidebarLines()) {
            String strip = Utils.stripString(line);
            if (strip.contains("State: Active Round")) {
                return true;
            }
        }
        return false;
    }

    public int getBlockSlot() {
        for (int slot = 0; slot < InventoryPlayer.getHotbarSize(); ++slot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null) {
                if (stack.getItem() instanceof ItemBlock) {
                    Block block = ((ItemBlock) stack.getItem()).getBlock();
                    if (BlockUtils.isNormalBlock(block)) {
                        return slot;
                    }
                }
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (placeMop != null) {
            if (placingPitch > 90.0f) {
                if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), placeMop.getBlockPos(), placeMop.sideHit, placeMop.hitVec)) {
                    mc.thePlayer.swingItem();
                    mc.getItemRenderer().resetEquippedProgress();
                    delay = (int) placeDelay.getInput();
                    fakeSwing = false;
                }
                placeMop = null;
            }
            else {
                placingPitch += 300.0f;
            }
            e.setYaw(placingYaw);
            e.setPitch(placingPitch - 300.0f);
            return;
        }
        if (miningPos != null) {
            float[] rotations = RotationUtils.getRotationsToBlock(miningPos, EnumFacing.UP, e.getYaw(), e.getPitch());
            if (rotations != null) {
                e.setRotations(rotations[0], rotations[1]);
            }
        }
        if (delay > 0 && (!onlyMiddleClick.isToggled() || Mouse.isButtonDown(2))) {
            List<BlockPos> posList = getPossiblePos(middlePos, true);
            BlockPos closestPos = null;
            if (!posList.isEmpty()) {
                closestPos = getClosestPos(posList, true);
            }
            if (closestPos == null) {
                posList = getPossiblePos(middlePos, false);
                closestPos = getClosestPos(posList, false);
            }
            if (closestPos != null) {
                float[] rotations = RotationUtils.getRotationsToBlock(closestPos, EnumFacing.UP, e.getYaw(), e.getPitch());
                if (rotations != null) {
                    e.setRotations(rotations[0], rotations[1]);
                }
            }
        }
    }

    private boolean isWoolWars() {
        if (!Utils.nullCheck() || !Utils.isHypixel()) {
            return false;
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return false;
        }
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        return objective != null && Utils.stripString(objective.getDisplayName()).contains("WOOL WARS");
    }

    private void swapBack() {
        if (swapBack != -1) {
            mc.thePlayer.inventory.currentItem = swapBack;
            swapBack = -1;
        }
    }

    private void reset() {
        middlePos = (miningPos = null);
        placeMop = null;
        curBlockDamageMP = (delay = 0);
        swapBack = -1;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (middlePos != null) {
            for (BlockPos pos : getPossiblePos(middlePos, false)) {
                RenderUtils.renderBlock(pos, this.middlePositionColors, true, false);
            }
        }
        if (miningPos != null) {
            RenderUtils.renderBlock(miningPos, this.miningColor, false, true);
        }
        else if (placeMop != null) {
            RenderUtils.renderBlock(BlockUtils.offsetPos(placeMop), this.placeColor, false, true);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 0) {
            if (e.buttonstate && (miningPos != null || placeMop != null)) {
                e.setCanceled(true);
            }
        } 
        else if (e.button == 1 && (miningPos != null || placeMop != null)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            reset();
        }
    }

    public static void startBreak(BlockPos pos) {
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
    }

    public static void stopBreak(BlockPos pos) {
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP));
    }

    public static void abortBreak(final BlockPos pos) {
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN));
    }
}