package keystrokesmod.module.impl.player;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.script.model.Simulation;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Safewalk extends Module {
    private SliderSetting motion;
    public static ButtonSetting blocksOnly, pitchCheck, disableOnForward, legit;

    private SliderSetting edgeOffset, unsneakDelay, sneakOnJump;
    public static ButtonSetting sneakKeyPressed, holdingBlocks, rmbDown, lookingDown, disableForward;

    public boolean isSneaking;
    private boolean wasOn;
    private double HW = 0.3;
    private double[][] CORNERS = {{ -HW, -HW }, { HW, -HW }, { -HW, HW }, { HW, HW }};
    private boolean setSneaking;
    private int sneakJumpDelayTicks;
    private int sneakJumpStartTick = -1;
    private int unsneakDelayTicks;
    private int unsneakStartTick = -1;

    public Safewalk() {
        super("Safewalk", Module.category.player, 0);
        this.registerSetting(motion = new SliderSetting("Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        this.registerSetting(disableOnForward = new ButtonSetting("Disable on forward", false));
        this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
        this.registerSetting(legit = new ButtonSetting("Legit", false));

        this.registerSetting(edgeOffset = new SliderSetting("Edge offset", " blocks", 0, 0, 0.3, 0.01));
        this.registerSetting(unsneakDelay = new SliderSetting("Unsneak delay", "ms", 50, 50, 300, 5));
        this.registerSetting(sneakOnJump = new SliderSetting("Sneak on jump", "ms", 50, 50, 300, 5));
        this.registerSetting(sneakKeyPressed = new ButtonSetting("Sneak key pressed", false));
        this.registerSetting(holdingBlocks = new ButtonSetting("Holding blocks", false));
        this.registerSetting(rmbDown = new ButtonSetting("RMB down", false));
        this.registerSetting(lookingDown = new ButtonSetting("Looking down", false));
        this.registerSetting(disableForward = new ButtonSetting("Disable on forward", false));
    }

    public void guiUpdate() {
        blocksOnly.setVisible(!legit.isToggled(), this);
        disableOnForward.setVisible(!legit.isToggled(), this);
        pitchCheck.setVisible(!legit.isToggled(), this);

        edgeOffset.setVisible(legit.isToggled(), this);
        unsneakDelay.setVisible(legit.isToggled(), this);
        sneakOnJump.setVisible(legit.isToggled(), this);
        sneakKeyPressed.setVisible(legit.isToggled(), this);
        holdingBlocks.setVisible(legit.isToggled(), this);
        rmbDown.setVisible(legit.isToggled(), this);
        lookingDown.setVisible(legit.isToggled(), this);
        disableForward.setVisible(legit.isToggled(), this);
    }

    @Override
    public String getInfo() {
        return legit.isToggled() ? ((int) unsneakDelay.getInput() + "ms") : "";
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onUpdate() {
        if (motion.getInput() != 1.0 && mc.thePlayer.onGround && Utils.isMoving() && safewalkSettingsMet()) {
            mc.thePlayer.motionX *= motion.getInput();
            mc.thePlayer.motionZ *= motion.getInput();
        }
    }

    public static boolean canSafeWalk() {
        if (ModuleManager.safeWalk != null && ModuleManager.safeWalk.isEnabled()) {
            if (legit.isToggled()) {
                return false;
            }
            if (!safewalkSettingsMet()) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean safewalkSettingsMet() {
        if (ModuleManager.scaffold.isEnabled) {
            return false;
        }
        if (blocksOnly.isToggled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                return false;
            }
        }
        if (disableOnForward.isToggled() && mc.thePlayer.moveForward > -0.2) {
            return false;
        }
        if (pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
            return false;
        }
        return true;
    }

    public boolean legitScafSettingsMet() {
        if (!Utils.tabbedIn()) {
            return false;
        }
        if (ModuleManager.scaffold.isEnabled) {
            return false;
        }
        if (holdingBlocks.isToggled()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                return false;
            }
        }
        if (disableForward.isToggled() && mc.thePlayer.moveForward > -0.2) {
            return false;
        }
        if (lookingDown.isToggled() && mc.thePlayer.rotationPitch < 70.0f) {
            return false;
        }
        if (sneakKeyPressed.isToggled() && !Utils.isBindDown(mc.gameSettings.keyBindSneak)) {
            return false;
        }
        else if (!sneakKeyPressed.isToggled() && Utils.isBindDown(mc.gameSettings.keyBindSneak)) {
            return false;
        }
        if (rmbDown.isToggled() && !Utils.keybinds.isMouseDown(1)) {
            return false;
        }
        return true;
    }

    private void reset() {
        if (!legit.isToggled()) {
            return;
        }
        setSneaking = false;
        sneakJumpDelayTicks = unsneakDelayTicks = 0;
        sneakJumpStartTick = unsneakStartTick = -1;
        if (!wasOn) {
            return;
        }
        wasOn = false;
        if (!Utils.tabbedIn()) {
            if (mc.thePlayer.isSneaking()) {
                Utils.setSneak(false);
            }
            return;
        }
        if (mc.thePlayer.isSneaking() && !Utils.sneakDown()) {
            Utils.setSneak(false);
        }
        if (!mc.thePlayer.isSneaking() && Utils.sneakDown()) {
            Utils.setSneak(true);
        }
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (!legit.isToggled()) {
            return;
        }
        if (!legitScafSettingsMet()) {
            reset();
            return;
        }
        if (!setSneaking) {
            e.setSneak(false);
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!legit.isToggled()) {
            return;
        }
        if (!legitScafSettingsMet()) {
            reset();
            return;
        }
        wasOn = true;

        if (Utils.jumpDown() && mc.thePlayer.onGround && (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0) && sneakOnJump.getInput() > 0) {
            sneakJumpStartTick = mc.thePlayer.ticksExisted;
            double raw = sneakOnJump.getInput() / 50;
            int base = (int) raw;
            sneakJumpDelayTicks = base + (Utils.randomizeDouble(0, 1) < (raw - base) ? 1 : 0);
            pressSneak(true);
            return;
        }

        Vec3 position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        Simulation sim = Simulation.create();
        if (mc.thePlayer.isSneaking()) {
            sim.setForward(mc.thePlayer.moveForward / 0.3f);
            sim.setStrafe(mc.thePlayer.moveStrafing / 0.3f);
            sim.setSneak(false);
        }
        sim.tick();
        Vec3 simPosition = sim.getPosition();

        double edgeOffsetv = computeEdgeOffset(simPosition, position);
        if (Double.isNaN(edgeOffsetv)) {
            if (setSneaking) tryReleaseSneak(true);
            return;
        }

        boolean shouldSneak = edgeOffsetv > edgeOffset.getInput();
        boolean shouldRelease = setSneaking;

        if (shouldSneak) {
            pressSneak(true);
        } else if (shouldRelease) {
            tryReleaseSneak(true);
        }
    }

    private void pressSneak(boolean resetDelay) {
        Utils.setSneak(true);
        setSneaking = true;
        if (resetDelay) {
            unsneakStartTick = -1;
        }
    }

    private void tryReleaseSneak(boolean resetDelay) {
        int existed = mc.thePlayer.ticksExisted;
        if (unsneakStartTick == -1 && sneakJumpStartTick == -1) {
            unsneakStartTick = existed;
            double raw = (unsneakDelay.getInput() - 50) / 50;
            int base = (int) raw;
            unsneakDelayTicks = base + (Utils.randomizeDouble(0, 1) < (raw - base) ? 1 : 0);
        }

        if (existed - sneakJumpStartTick < sneakJumpDelayTicks) {
            pressSneak(false);
            return;
        }
        if (existed - unsneakStartTick < unsneakDelayTicks) {
            pressSneak(false);
            return;
        }

        releaseSneak(resetDelay);
    }

    private void releaseSneak(boolean resetDelay) {
        Utils.setSneak(false);
        setSneaking = false;
        if (resetDelay) {
            unsneakStartTick = sneakJumpStartTick = -1;
        }
    }

    private double computeEdgeOffset(Vec3 pos1, Vec3 pos2) {
        int floorY = (int)(pos1.y - 0.01);
        double best = Double.NaN;

        for (double[] c : CORNERS) {
            int bx = (int)Math.floor(pos2.x + c[0]);
            int bz = (int)Math.floor(pos2.z + c[1]);
            if (ScriptDefaults.world.getBlockAt(bx, floorY, bz).name.equals("air")) continue;

            double offX = Math.abs(pos1.x - (bx + (pos1.x < bx + 0.5 ? 0 : 1)));
            double offZ = Math.abs(pos1.z - (bz + (pos1.z < bz + 0.5 ? 0 : 1)));
            boolean xDiff = (int)Math.floor(pos1.x) != bx;
            boolean zDiff = (int)Math.floor(pos1.z) != bz;

            double cornerDist;
            if (xDiff) {
                cornerDist = zDiff ? Math.max(offX, offZ) : offX;
            } else {
                cornerDist = zDiff ? offZ : 0;
            }

            best = Double.isNaN(best) ? cornerDist : Math.min(best, cornerDist);
        }

        return best;
    }

}