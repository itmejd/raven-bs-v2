package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class AutoPlace extends Module {
    private SliderSetting mode;
    private SliderSetting frameDelay;
    private SliderSetting minPlaceDelay;
    private ButtonSetting disableLeft;
    private ButtonSetting holdRight;
    private ButtonSetting fastPlaceOnJump;
    private ButtonSetting pitchCheck;

    private double cachedFrameDelay = 0.0D;
    private long lastPlace = 0L;
    private int frameCount = 0;
    private MovingObjectPosition lastRayTrace = null;
    private BlockPos lastBlockPos = null;

    private String[] modes = new String[] { "Post", "Multi-place" };

    public AutoPlace() {
        super("AutoPlace", category.player);
        this.registerSetting(new DescriptionSetting("Best with safewalk."));
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(frameDelay = new SliderSetting("Frame delay", 8.0D, 0.0D, 30.0D, 1.0D));
        this.registerSetting(minPlaceDelay = new SliderSetting("Min place delay", 60.0, 25.0, 500.0, 5.0));
        this.registerSetting(disableLeft = new ButtonSetting("Disable if LMB down", false));
        this.registerSetting(holdRight = new ButtonSetting("RMB required", true));
        this.registerSetting(fastPlaceOnJump = new ButtonSetting("Fast place on jump", true));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
    }

    public void guiUpdate() {
        if (this.cachedFrameDelay != frameDelay.getInput()) {
            resetVariables();
        }
        this.cachedFrameDelay = frameDelay.getInput();
    }

    @Override
    public void onDisable() {
        if (holdRight.isToggled()) {
            setRightClickDelay(4);
        }
        resetVariables();
    }

    @Override
    public void onUpdate() {
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }
        if (fastPlaceOnJump.isToggled() && holdRight.isToggled() && !ModuleManager.fastPlace.isEnabled() && Mouse.isButtonDown(1)) {
            if (mc.thePlayer.motionY > 0.0) {
                setRightClickDelay(1);
            }
            else if (!pitchCheck.isToggled() || mc.thePlayer.rotationPitch >= 70.0F) {
                setRightClickDelay(1000);
            }
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!conditions()) {
            return;
        }
        if (mode.getInput() != 1) {
            return;
        }
        dp();
        dp();
        dp();
        dp();
    }

    private void dp() {
        MovingObjectPosition mouseOverResult = mc.objectMouseOver;
        if (mouseOverResult == null || mouseOverResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mouseOverResult.sideHit == EnumFacing.UP || mouseOverResult.sideHit == EnumFacing.DOWN) {
            return;
        }

        if (this.lastRayTrace != null && (double) this.frameCount < frameDelay.getInput()) {
            this.frameCount++;
            return;
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        this.lastRayTrace = mouseOverResult;

        BlockPos currentBlockPosition = mouseOverResult.getBlockPos();

        if (this.lastBlockPos != null && currentBlockPosition.getX() == this.lastBlockPos.getX() && currentBlockPosition.getY() == this.lastBlockPos.getY() && currentBlockPosition.getZ() == this.lastBlockPos.getZ()) {
            return;
        }
        Block targetBlock = mc.theWorld.getBlockState(currentBlockPosition).getBlock();
        if (targetBlock == null || targetBlock == Blocks.air || targetBlock instanceof BlockLiquid) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, currentBlockPosition, mouseOverResult.sideHit, mouseOverResult.hitVec)) {
            ReflectionUtils.setButton(1, true);
            mc.thePlayer.swingItem();
            mc.getItemRenderer().resetEquippedProgress();
            ReflectionUtils.setButton(1, false);

            this.lastBlockPos = currentBlockPosition;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHighlight(DrawBlockHighlightEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (!conditions()) {
            return;
        }
        if (mode.getInput() != 0) {
            return;
        }
        MovingObjectPosition mouseOverResult = mc.objectMouseOver;
        if (mouseOverResult == null || mouseOverResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mouseOverResult.sideHit == EnumFacing.UP || mouseOverResult.sideHit == EnumFacing.DOWN) {
            return;
        }

        if (this.lastRayTrace != null && (double) this.frameCount < frameDelay.getInput()) {
            this.frameCount++;
            return;
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        this.lastRayTrace = mouseOverResult;

        BlockPos currentBlockPosition = mouseOverResult.getBlockPos();

        if (this.lastBlockPos != null && currentBlockPosition.getX() == this.lastBlockPos.getX() && currentBlockPosition.getY() == this.lastBlockPos.getY() && currentBlockPosition.getZ() == this.lastBlockPos.getZ()) {
            return;
        }
        Block targetBlock = mc.theWorld.getBlockState(currentBlockPosition).getBlock();
        if (targetBlock == null || targetBlock == Blocks.air || targetBlock instanceof BlockLiquid) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastPlace < minPlaceDelay.getInput()) {
            return;
        }
        this.lastPlace = currentTime;
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, currentBlockPosition, mouseOverResult.sideHit, mouseOverResult.hitVec)) {
            ReflectionUtils.setButton(1, true);
            mc.thePlayer.swingItem();
            mc.getItemRenderer().resetEquippedProgress();
            ReflectionUtils.setButton(1, false);

            this.lastBlockPos = currentBlockPosition;
            this.frameCount = 0;
        }
    }
    private void setRightClickDelay(int delay) {
        ((IAccessorMinecraft) mc).setRightClickDelayTimer(delay);
    }

    private void resetVariables() {
        this.lastBlockPos = null;
        this.lastRayTrace = null;
        this.frameCount = 0;
    }

    private boolean conditions() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return false;
        }
        if (disableLeft.isToggled() && Mouse.isButtonDown(0)) {
            return false;
        }
        if (holdRight.isToggled() && !Mouse.isButtonDown(1)) {
            return false;
        }
        if (ModuleManager.scaffold.isEnabled) {
            return false;
        }
        if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0F) {
            return false;
        }
        if (mc.thePlayer.moveForward > -0.2) {
            return false;
        }
        return true;
    }
}