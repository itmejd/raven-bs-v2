package keystrokesmod.module.impl.player;

import keystrokesmod.mixins.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class AutoTool extends Module {
    private SliderSetting hoverDelay;
    private ButtonSetting rightDisable;
    private ButtonSetting requireCrouch;
    private ButtonSetting requireMouse;
    public ButtonSetting spoofItem;
    private ButtonSetting swap;
    public int previousSlot = -1;
    private long ticksHovered;

    public AutoTool() {
        super("AutoTool", category.player);
        this.registerSetting(hoverDelay = new SliderSetting("Hover delay", 0.0, 0.0, 20.0, 1.0));
        this.registerSetting(rightDisable = new ButtonSetting("Disable while right click", true));
        this.registerSetting(requireCrouch = new ButtonSetting("Only while crouching", false));
        this.registerSetting(requireMouse = new ButtonSetting("Require mouse down", true));
        this.registerSetting(spoofItem = new ButtonSetting("Spoof item", false));
        this.registerSetting(swap = new ButtonSetting("Swap to previous slot", true));
        this.closetModule = true;
    }

    public void onDisable() {
        resetVariables();
    }

    public void setSlot(final int currentItem) {
        if (currentItem == -1) {
            return;
        }
        mc.thePlayer.inventory.currentItem = currentItem;
    }

    public void onUpdate() {
        if (spoofItem.isToggled() && previousSlot != mc.thePlayer.inventory.currentItem && previousSlot != -1) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        if (!mc.inGameHasFocus || mc.currentScreen != null || (rightDisable.isToggled() && Mouse.isButtonDown(1)) || !mc.thePlayer.capabilities.allowEdit || (requireCrouch.isToggled() && !mc.thePlayer.isSneaking())) {
            resetVariables();
            return;
        }
        if (!Mouse.isButtonDown(0) && requireMouse.isToggled()) {
            resetSlot();
            return;
        }
        MovingObjectPosition over = mc.objectMouseOver;
        if (over == null || over.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            resetSlot();
            resetVariables();
            return;
        }
        if (hoverDelay.getInput() != 0) {
            long ticks = this.ticksHovered + 1L;
            this.ticksHovered = ticks;
            if (ticks < hoverDelay.getInput()) {
                return;
            }
        }
        int slot = Utils.getTool(BlockUtils.getBlock(over.getBlockPos()));
        if (slot == -1) {
            return;
        }
        if (previousSlot == -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
        }
        setSlot(slot);
    }

    private void resetVariables() {
        ticksHovered = 0;
        resetSlot();
        previousSlot = -1;
    }

    private void resetSlot() {
        if (previousSlot == -1 || !swap.isToggled()) {
            return;
        }
        setSlot(previousSlot);
        previousSlot = -1;
    }
}
