package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BedAura extends Module {
    public SliderSetting mode;
    private SliderSetting breakSpeed;
    private SliderSetting fov;
    public SliderSetting range;
    private SliderSetting rate;
    public ButtonSetting allowAura;
    private ButtonSetting breakNearBlock;
    private ButtonSetting cancelKnockback;
    private ButtonSetting disableBreakEffects;
    public ButtonSetting groundSpoof;
    private ButtonSetting onlyWhileVisible;
    private ButtonSetting renderOutline;
    private ButtonSetting sendAnimations;
    private ButtonSetting silentSwing;
    private String[] modes = new String[] { "Legit", "Instant", "Swap" };

    private BlockPos[] bedPos;
    private BlockPos packetPos;
    public float breakProgress;
    private int lastSlot = -1;
    public BlockPos currentBlock, lastBlock;
    private long lastCheck = 0;
    public boolean stopAutoblock, breakTick;
    private int outlineColor = new Color(226, 65, 65).getRGB();
    private BlockPos nearestBlock;
    private Map<BlockPos, Float> breakProgressMap = new HashMap<>();
    public double lastProgress;
    public float vanillaProgress;
    private int defaultOutlineColor = new Color(226, 65, 65).getRGB();
    private BlockPos previousBlockBroken;
    private BlockPos rotateLastBlock;
    private boolean spoofGround, firstStop;
    private boolean isBreaking, startPacket, stopPacket, ignoreSlow, delayStop;

    public BedAura() {
        super("BedAura", category.player, 0);
        this.registerSetting(mode = new SliderSetting("Break mode", 0, modes));
        this.registerSetting(breakSpeed = new SliderSetting("Break speed", "x", 1, 1, 2, 0.01));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(range = new SliderSetting("Range", 4.5, 1.0, 8.0, 0.5));
        this.registerSetting(rate = new SliderSetting("Rate", " second", 0.2, 0.05, 3.0, 0.05));
        this.registerSetting(allowAura = new ButtonSetting("Allow aura", true));
        this.registerSetting(breakNearBlock = new ButtonSetting("Break near block", false));
        this.registerSetting(cancelKnockback = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(disableBreakEffects = new ButtonSetting("Disable break effects", false));
        this.registerSetting(groundSpoof = new ButtonSetting("Ground spoof", false));
        this.registerSetting(onlyWhileVisible = new ButtonSetting("Only while visible", false));
        this.registerSetting(renderOutline = new ButtonSetting("Render block outline", true));
        this.registerSetting(sendAnimations = new ButtonSetting("Send animations", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @Override
    public void onDisable() {
        reset(true, true);
        bedPos = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST) // takes priority over ka & antifireball
    public void onPreUpdate(PreUpdateEvent e) {

    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            reset(true, true);
            bedPos = null;
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || !cancelKnockback.isToggled() || currentBlock == null) {
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (delayStop) {
            delayStop = false;
        } else {
            stopAutoblock = false;
        }
        breakTick = false;
        if (currentBlock == null || !RotationUtils.inRange(currentBlock, range.getInput())) {
            reset(true, true);
            bedPos = null;
        }
        if (Utils.isBedwarsPracticeOrReplay()) {
            return;
        }
        if (ModuleManager.bedwars != null && ModuleManager.bedwars.isEnabled() && BedWars.whitelistOwnBed.isToggled() && !BedWars.outsideSpawn) {
            reset(true, true);
            return;
        }
        if (!mc.thePlayer.capabilities.allowEdit || mc.thePlayer.isSpectator()) {
            reset(true, true);
            return;
        }
        if (bedPos == null) {
            if (!isBreaking && System.currentTimeMillis() - lastCheck >= (rate.getInput() * 1000)) {
                lastCheck = System.currentTimeMillis();
                bedPos = getBedPos();
            }
            if (bedPos == null) {
                reset(true, true);
                return;
            }
        }
        else {
            if (!(BlockUtils.getBlock(bedPos[0]) instanceof BlockBed) || (currentBlock != null && BlockUtils.replaceable(currentBlock))) {
                reset(true, true);
                return;
            }
        }
        if (breakNearBlock.isToggled() && isCovered(bedPos[0]) && isCovered(bedPos[1])) {
            if (nearestBlock == null) {
                nearestBlock = getBestBlock(bedPos, true);
            }
            breakBlock(e, nearestBlock);
        }
        else {
            nearestBlock = null;
            breakBlock(e, bedPos[0]);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        if (stopAutoblock) {
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7stopping autoblock (&3" + mc.thePlayer.ticksExisted + "&7).");
            }
        }

        if (groundSpoof.isToggled() && !mc.thePlayer.isInWater() && spoofGround) {
            e.setOnGround(true);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7ground spoof (&3" + mc.thePlayer.ticksExisted + "&7).");
            }
        }

        if (startPacket) {
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, packetPos, EnumFacing.UP));
            swing();
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "sending c07 &astart &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
            }
        }
        if (stopPacket) {
            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, packetPos, EnumFacing.UP));
            swing();
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "sending c07 &cstop &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
            }
        }
        if (isBreaking && !startPacket && !stopPacket) {
            swing();
        }

        startPacket = stopPacket = spoofGround = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!renderOutline.isToggled() || currentBlock == null || !Utils.nullCheck()) {
            return;
        }
        if (ModuleManager.bedESP != null && ModuleManager.bedESP.isEnabled()) {
            outlineColor = Theme.getGradient((int) ModuleManager.bedESP.theme.getInput(), 0);
        }
        else if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            outlineColor = Theme.getGradient((int) ModuleManager.hud.theme.getInput(), 0);
        }
        else {
            outlineColor = defaultOutlineColor;
        }
        RenderUtils.renderBlock(currentBlock, outlineColor, (Arrays.asList(bedPos).contains(currentBlock) ? 0.5625 : 1),true, false);
    }

    private void resetSlot() {
        if (Raven.packetsHandler != null && Raven.packetsHandler.playerSlot != null && Utils.nullCheck() && Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem && mode.getInput() == 2) {
            setPacketSlot(mc.thePlayer.inventory.currentItem);
        }
        else if (lastSlot != -1) {
            lastSlot = mc.thePlayer.inventory.currentItem = lastSlot;
        }
    }

    public boolean cancelKnockback() {
        return cancelKnockback.isToggled() && currentBlock != null && RotationUtils.inRange(currentBlock, range.getInput());
    }

    private BlockPos[] getBedPos() {
        int range;
        priority:
        for (int n = range = (int) this.range.getInput(); range >= -n; --range) {
            for (int j = -n; j <= n; ++j) {
                for (int k = -n; k <= n; ++k) {
                    final BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + range, mc.thePlayer.posZ + k);
                    final IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                    if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue((IProperty) BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                        float fov = (float) this.fov.getInput();
                        if (fov != 360 && !Utils.inFov(fov, blockPos)) {
                            continue priority;
                        }
                        return new BlockPos[]{blockPos, blockPos.offset((EnumFacing) getBlockState.getValue((IProperty) BlockBed.FACING))};
                    }
                }
            }
        }
        return null;
    }

    private void setRots(ClientRotationEvent e) {
        float[] rotations = RotationUtils.getRotations(currentBlock == null ? rotateLastBlock : currentBlock, e.getYaw(), e.getPitch());
        e.setYaw(RotationUtils.applyVanilla(rotations[0]));
        e.setPitch(rotations[1]);
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "&7rotating (&3" + mc.thePlayer.ticksExisted + "&7).");
        }
    }

    public BlockPos getBestBlock(BlockPos[] positions, boolean getSurrounding) {
        if (positions == null || positions.length == 0) {
            return null;
        }
        HashMap<BlockPos, double[]> blockMap = new HashMap<>();
        for (BlockPos pos : positions) {
            if (pos == null) {
                continue;
            }
            if (getSurrounding) {
                for (EnumFacing enumFacing : EnumFacing.values()) {
                    if (enumFacing == EnumFacing.DOWN) {
                        continue;
                    }
                    BlockPos offset = pos.offset(enumFacing);
                    if (Arrays.asList(positions).contains(offset)) {
                        continue;
                    }
                    if (!RotationUtils.inRange(offset, range.getInput())) {
                        continue;
                    }
                    double efficiency = getEfficiency(offset);
                    double distance = mc.thePlayer.getDistanceSqToCenter(offset);
                    blockMap.put(offset, new double[]{distance, efficiency});
                }
            }
            else {
                if (!RotationUtils.inRange(pos, range.getInput())) {
                    continue;
                }
                double efficiency = getEfficiency(pos);
                double distance = mc.thePlayer.getDistanceSqToCenter(pos);
                blockMap.put(pos, new double[]{distance, efficiency});
            }
        }
        List<Map.Entry<BlockPos, double[]>> sortedByDistance = sortByDistance(blockMap);
        List<Map.Entry<BlockPos, double[]>> sortedByEfficiency = sortByEfficiency(sortedByDistance);
        List<Map.Entry<BlockPos, double[]>> sortedByPreviousBlocks = sortByPreviousBlocks(sortedByEfficiency);
        return sortedByPreviousBlocks.isEmpty() ? null : sortedByPreviousBlocks.get(0).getKey();
    }

    private List<Map.Entry<BlockPos, double[]>> sortByDistance(HashMap<BlockPos, double[]> blockMap) {
        List<Map.Entry<BlockPos, double[]>> list = new ArrayList<>(blockMap.entrySet());
        list.sort(Comparator.comparingDouble(entry -> entry.getValue()[0]));
        return list;
    }

    private List<Map.Entry<BlockPos, double[]>> sortByEfficiency(List<Map.Entry<BlockPos, double[]>> blockList) {
        blockList.sort((entry1, entry2) -> Double.compare(entry2.getValue()[1], entry1.getValue()[1]));
        return blockList;
    }

    private List<Map.Entry<BlockPos, double[]>> sortByPreviousBlocks(List<Map.Entry<BlockPos, double[]>> blockList) {
        blockList.sort((entry1, entry2) -> {
            boolean isEntry1Previous = entry1.getKey().equals(previousBlockBroken);
            boolean isEntry2Previous = entry2.getKey().equals(previousBlockBroken);
            if (isEntry1Previous && !isEntry2Previous) {
                return -1;
            }
            if (!isEntry1Previous && isEntry2Previous) {
                return 1;
            }
            return 0;
        });
        return blockList;
    }

    private double getEfficiency(BlockPos pos) {
        Block block = BlockUtils.getBlock(pos);
        ItemStack tool = (mode.getInput() == 2 && Utils.getTool(block) != -1) ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block)) : mc.thePlayer.getHeldItem();
        double efficiency = BlockUtils.getBlockHardness(block, tool, false, ignoreSlow);

        if (breakProgressMap.get(pos) != null) {
            efficiency = breakProgressMap.get(pos);
        }

        return efficiency;
    }

    private void reset(boolean resetSlot, boolean stopAutoblock) {
        if (resetSlot) {
            resetSlot();
        }
        breakProgress = 0;
        breakProgressMap.clear();
        lastSlot = -1;
        vanillaProgress = 0;
        lastProgress = 0;
        if (stopAutoblock) {
            this.stopAutoblock = false;
        }
        rotateLastBlock = null;
        firstStop = false;
        if (isBreaking) {
            ModuleUtils.isBreaking = false;
            isBreaking = false;
        }
        breakTick = false;
        currentBlock = null;
        nearestBlock = null;
        ignoreSlow = false;
        delayStop = false;
    }

    public void setPacketSlot(int slot) {
        if (slot == -1) {
            return;
        }
        Raven.packetsHandler.updateSlot(slot);
        stopAutoblock = true;
    }

    private void startBreak(ClientRotationEvent e ,BlockPos blockPos) {
        setRots(e);
        packetPos = blockPos;
        startPacket = true;
        isBreaking = true;
        breakTick = true;

        ignoreSlow = true;
    }

    private void stopBreak(ClientRotationEvent e, BlockPos blockPos) {
        setRots(e);
        packetPos = blockPos;
        stopPacket = true;
        isBreaking = false;
        breakTick = true;
        if (ignoreSlow) {
            spoofGround = true;
        }
        ignoreSlow = false;
    }

    private void swing() {
        if (!silentSwing.isToggled()) {
            mc.thePlayer.swingItem();
        }
        else {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
    }

    private void breakBlock(ClientRotationEvent e, BlockPos blockPos) {
        if (blockPos == null) {
            reset(true, true);
            return;
        }
        lastBlock = blockPos;
        float fov = (float) this.fov.getInput();
        if (fov != 360 && !Utils.inFov(fov, blockPos)) {
            return;
        }
        if (onlyWhileVisible.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || !mc.objectMouseOver.getBlockPos().equals(blockPos))) {
            return;
        }
        if (BlockUtils.replaceable(currentBlock == null ? blockPos : currentBlock)) {
            reset(true, true);
            return;
        }
        Block block = BlockUtils.getBlock(blockPos);
        currentBlock = blockPos;
        if ((breakProgress <= 0 || breakProgress >= 1) && mode.getInput() == 2 && !firstStop) {
            firstStop = true;
            stopAutoblock = delayStop = true;
            setRots(e);
            return;
        }
        if (mode.getInput() == 2 || mode.getInput() == 0) {
            if (breakProgress == 0) {
                resetSlot();
                if (mode.getInput() == 0) {
                    setSlot(Utils.getTool(block));
                }
                startBreak(e, blockPos);
            }
            else if (breakProgress >= 1) {
                if (mode.getInput() == 2) {
                    setPacketSlot(Utils.getTool(block));
                }
                stopBreak(e, blockPos);
                previousBlockBroken = currentBlock;
                reset(false, false);
                Iterator<Map.Entry<BlockPos, Float>> iterator = breakProgressMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<BlockPos, Float> entry = iterator.next();
                    if (entry.getKey().equals(blockPos)) {
                        iterator.remove();
                    }
                }
                if (!disableBreakEffects.isToggled()) {
                    mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.UP);
                }
                rotateLastBlock = previousBlockBroken;
                return;
            }
            else {
                if (mode.getInput() == 0) {

                }
            }
            double progress = vanillaProgress = (float) (BlockUtils.getBlockHardness(block, (mode.getInput() == 2 && Utils.getTool(block) != -1) ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block)) : mc.thePlayer.getHeldItem(), false, ignoreSlow) * breakSpeed.getInput());
            if (lastProgress != 0 && breakProgress >= lastProgress - vanillaProgress) {
                if (breakProgress >= lastProgress) {
                    if (mode.getInput() == 2) {
                        if (Raven.debug) {
                            Utils.sendModuleMessage(this, "&7setting slot &7(&b" + mc.thePlayer.ticksExisted + "&7)");
                        }
                        setPacketSlot(Utils.getTool(block));
                    }
                }
            }
            breakProgress += progress;
            breakProgressMap.put(blockPos, breakProgress);
            if (breakProgress > 0) firstStop = false;
            if (sendAnimations.isToggled()) {
                mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int) ((breakProgress * 10) - 1));
            }
            lastProgress = 0;
            while (lastProgress + progress < 1) {
                lastProgress += progress;
            }
        }
        else if (mode.getInput() == 1) {
            swing();
            startBreak(e, blockPos);
            setSlot(Utils.getTool(block));
            stopBreak(e, blockPos);
        }
    }

    private void setSlot(int slot) {
        if (slot == -1 || slot == mc.thePlayer.inventory.currentItem) {
            return;
        }
        if (lastSlot == -1) {
            lastSlot = mc.thePlayer.inventory.currentItem;
        }
        mc.thePlayer.inventory.currentItem = slot;
    }

    private boolean isCovered(BlockPos blockPos) {
        for (EnumFacing enumFacing : EnumFacing.values()) {
            BlockPos offset = blockPos.offset(enumFacing);
            if (BlockUtils.replaceable(offset) || BlockUtils.notFull(BlockUtils.getBlock(offset)) ) {
                return false;
            }
        }
        return true;
    }
}