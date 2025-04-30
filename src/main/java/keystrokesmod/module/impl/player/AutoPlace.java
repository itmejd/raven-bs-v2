package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AutoPlace extends Module {
    private SliderSetting frameDelay;
    private SliderSetting minPlaceDelay;
    private ButtonSetting disableLeft;
    private ButtonSetting holdRight;
    private ButtonSetting fastPlaceOnJump;
    private ButtonSetting pitchCheck;
    private ButtonSetting silentSwing;

    private double cachedFrameDelay = 0.0D;
    private long lastPlace = 0L;
    private int frameCount = 0;
    private MovingObjectPosition lastRayTrace = null;
    private BlockPos lastBlockPos = null;

    public AutoPlace() {
        super("AutoPlace", category.player);
        this.registerSetting(new DescriptionSetting("Best with safewalk."));
        this.registerSetting(frameDelay = new SliderSetting("Frame delay", 8.0D, 0.0D, 30.0D, 1.0D));
        this.registerSetting(minPlaceDelay = new SliderSetting("Min place delay", 60.0, 25.0, 500.0, 5.0));
        this.registerSetting(disableLeft = new ButtonSetting("Disable left", false));
        this.registerSetting(holdRight = new ButtonSetting("Hold right", true));
        this.registerSetting(fastPlaceOnJump = new ButtonSetting("Fast place on jump", true));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHighlight(DrawBlockHighlightEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
            return;
        }
        if (disableLeft.isToggled() && Mouse.isButtonDown(0)) {
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
        this.lastRayTrace = mouseOverResult;

        BlockPos currentBlockPosition = mouseOverResult.getBlockPos();

        if (this.lastBlockPos != null && currentBlockPosition.getX() == this.lastBlockPos.getX() && currentBlockPosition.getY() == this.lastBlockPos.getY() && currentBlockPosition.getZ() == this.lastBlockPos.getZ()) {
            return;
        }
        Block targetBlock = mc.theWorld.getBlockState(currentBlockPosition).getBlock();
        if (targetBlock == null || targetBlock == Blocks.air || targetBlock instanceof BlockLiquid) {
            return;
        }
        if (holdRight.isToggled() && !Mouse.isButtonDown(1)) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.lastPlace < minPlaceDelay.getInput()) {
            return;
        }
        this.lastPlace = currentTime;
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, currentBlockPosition, mouseOverResult.sideHit, mouseOverResult.hitVec)) {
            ReflectionUtils.setButton(1, true);
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
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
}