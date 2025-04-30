package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.player.Safewalk;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.block.*;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;

public class Sprint extends Module {
    private ButtonSetting displayText;
    private ButtonSetting rainbow;
    public SliderSetting omniDirectional;
    private SliderSetting floatSetting;
    private ButtonSetting renderJumpRequired;
    public ButtonSetting disableBackwards;
    public String text = "[Sprint (Toggled)]";
    public float posX = 5;
    public float posY = 5;
    private float limit;
    public boolean canFloat, requireJump;
    public boolean sprintFloat;
    private int color = new Color(255, 0, 0, 255).getRGB();

    private String[] omniDirectionalModes = new String[] { "Disabled", "Vanilla", "Hypixel", "Float" };

    public Sprint() {
        super("Sprint", category.movement, 0);
        this.registerSetting(new DescriptionSetting("Command: '§esprint [msg]§r'"));
        this.registerSetting(new ButtonSetting("Edit text position", () -> {
            mc.displayGuiScreen(new EditScreen());
        }));
        this.registerSetting(displayText = new ButtonSetting("Display text", false));
        this.registerSetting(rainbow = new ButtonSetting("Rainbow", false));
        this.registerSetting(omniDirectional = new SliderSetting("Omni-Directional", 0, omniDirectionalModes));
        this.registerSetting(floatSetting = new SliderSetting("Float speed", "%", 100, 0.0, 100.0, 1.0));
        this.registerSetting(renderJumpRequired = new ButtonSetting("Render jump required", false));
        this.registerSetting(disableBackwards = new ButtonSetting("Disable backwards", false));
        this.closetModule = true;
    }

    public void guiUpdate() {
        this.floatSetting.setVisible(omniDirectional.getInput() == 3, this);
        this.renderJumpRequired.setVisible(omniDirectional.getInput() == 3, this);
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (Utils.jumpDown() && mc.thePlayer.onGround) {
            requireJump = true;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        if (ModuleUtils.groundTicks <= 8 || floatConditions()) {
            canFloat = true;
        }
        if (!floatConditions()) {
            canFloat = false;
        }
        if (!mc.thePlayer.onGround) {
            requireJump = false;
        }

        if (canFloat && floatConditions() && !requireJump && omniSprint()) {
            e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
            sprintFloat = true;
            ModuleUtils.groundTicks = 0;
            if (Utils.isMoving()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
        }
        else {
            sprintFloat = false;
        }

        if (rotationConditions()) {
            float yaw = mc.thePlayer.rotationYaw;
            e.setYaw(yaw - 55);
            RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }
    }

    boolean floatConditions() {
        int edgeY = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
        if (ModuleUtils.stillTicks > 200) {
            requireJump = true;
            return false;
        }
        if (edgeY >= 10 && !allowedBlocks()) {
            requireJump = true;
            return false;
        }
        if (Safewalk.canSafeWalk()) {
            requireJump = true;
            return false;
        }
        if (ModuleManager.scaffold.isEnabled || ModuleManager.bhop.isEnabled()) {
            requireJump = true;
            return false;
        }
        if (ModuleManager.sprint.omniDirectional.getInput() != 3) {
            return false;
        }
        if (!mc.thePlayer.onGround) {
            return false;
        }
        if (Utils.jumpDown()) {
            return false;
        }
        if (ModuleManager.LongJump.function) {
            return false;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return false;
        }
        return true;
    }

    private boolean allowedBlocks() {
        Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        if (block instanceof BlockSnow) {
            return true;
        }
        if (block instanceof BlockCarpet) {
            return true;
        }
        return false;
    }

    private boolean rotationConditions() {
        if (Utils.noSlowingBackWithBow()) {
            ModuleManager.bhop.setRotation = false;
            return false;
        }
        if (omniDirectional.getInput() < 2) {
            return false;
        }
        if (!mc.thePlayer.onGround) {
            return false;
        }
        if (mc.thePlayer.moveForward >= 0 || mc.thePlayer.moveStrafing != 0) {
            return false;
        }
        if (Utils.jumpDown()) {
            return false;
        }
        if (KillAura.attackingEntity != null) {
            return false;
        }
        if (Safewalk.canSafeWalk()) {
            return false;
        }
        if (ModuleManager.scaffold.isEnabled || ModuleManager.bhop.isEnabled()) {
            return false;
        }
        if (Utils.holdingFireball() && mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= -0.5) {
            return false;
        }
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock && Mouse.isButtonDown(1) && mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= -0.8) {
            return false;
        }
        if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return false;
        }
        return true;
    }

    public boolean disableBackwards() {
        limit = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - Utils.getLastReportedYaw());
        double limitVal = 145;
        if (!disableBackwards.isToggled()) {
            return false;
        }
        if (exceptions()) {
            return false;
        }
        if ((limit <= -limitVal || limit >= limitVal)) {
            return true;
        }
        if (omniSprint() && ModuleManager.killAura.rotating && mc.thePlayer.moveForward <= 0.5) {
            return true;
        }
        return false;
    }

    public void onUpdate() {
        if (Utils.nullCheck() && mc.inGameHasFocus) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        if (displayText.isToggled() && !mc.gameSettings.showDebugInfo) {
            mc.fontRendererObj.drawStringWithShadow(text, posX, posY, rainbow.isToggled() ? Utils.getChroma(2, 0) : -1);
        }

        if (omniDirectional.getInput() != 3 || !renderJumpRequired.isToggled() || !requireJump || ModuleManager.scaffold.isEnabled || ModuleManager.bhop.isEnabled()) {
            return;
        }

        String text = "§c[Sprint]: Jump required to re-activate float";
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + 104, display[1] / 2 + 272, color, true);
    }

    public boolean omniSprint() {
        if (!this.isEnabled()) {
            return false;
        }
        if (Utils.safeWalkBackwards()) {
            return false;
        }
        if (!Utils.isMoving()) {
            return false;
        }
        if (mc.thePlayer.moveForward <= 0.5 && Utils.jumpDown()) {
            return false;
        }
        if (Utils.noSlowingBackWithBow()) {
            return false;
        }
        if (Utils.holdingFireball() && mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= -0.5) {
            return false;
        }
        if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock && Mouse.isButtonDown(1) && mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= -0.8) {
            return false;
        }
        if (omniDirectional.getInput() > 0) {
            return true;
        }
        return false;
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        double min = 0;
        if (mc.thePlayer.moveStrafing != 0 && mc.thePlayer.moveForward != 0) min = 0.003;
        if (speedLevel >= 0) {
            return ((floatSpeedLevels[speedLevel] - min) * (floatSetting.getInput() / 100));
        }
        return ((floatSpeedLevels[0] - min) * (floatSetting.getInput() / 100));
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

    private boolean exceptions() {
        return ModuleManager.scaffold.isEnabled || mc.thePlayer.hurtTime > 0;
    }

    static class EditScreen extends GuiScreen {
        GuiButtonExt resetPosition;
        boolean d = false;
        int miX = 0;
        int miY = 0;
        int maX = 0;
        int maY = 0;
        float aX = 5;
        float aY = 5;
        int laX = 0;
        int laY = 0;
        int lmX = 0;
        int lmY = 0;
        int clickMinX = 0;

        public void initGui() {
            super.initGui();
            this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
            this.aX = ModuleManager.sprint.posX;
            this.aY =ModuleManager.sprint.posY;
        }

        public void drawScreen(int mX, int mY, float pt) {
            drawRect(0, 0, this.width, this.height, -1308622848);
            int miX = (int) this.aX;
            int miY = (int) this.aY;
            String text = ModuleManager.sprint.text;
            int maX = miX + this.mc.fontRendererObj.getStringWidth(text);
            int maY = miY + this.mc.fontRendererObj.FONT_HEIGHT;
            this.mc.fontRendererObj.drawStringWithShadow(text, this.aX, this.aY, -1);
            this.miX = miX;
            this.miY = miY;
            this.maX = maX;
            this.maY = maY;
            this.clickMinX = miX;
            ModuleManager.sprint.posX = miX;
            ModuleManager.sprint.posY = miY;
            ScaledResolution res = new ScaledResolution(this.mc);
            int x = res.getScaledWidth() / 2 - 84;
            int y = res.getScaledHeight() / 2 - 20;
            RenderUtils.drawColoredString("Edit the HUD position by dragging.", '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

            try {
                this.handleInput();
            }
            catch (IOException var12) {
            }

            super.drawScreen(mX, mY, pt);
        }

        protected void mouseClickMove(int mX, int mY, int b, long t) {
            super.mouseClickMove(mX, mY, b, t);
            if (b == 0) {
                if (this.d) {
                    this.aX = this.laX + (mX - this.lmX);
                    this.aY = this.laY + (mY - this.lmY);
                }
                else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
                    this.d = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = (int) this.aX;
                    this.laY = (int) this.aY;
                }

            }
        }

        protected void mouseReleased(int mX, int mY, int s) {
            super.mouseReleased(mX, mY, s);
            if (s == 0) {
                this.d = false;
            }
        }

        public void actionPerformed(GuiButton b) {
            if (b == this.resetPosition) {
                this.aX = ModuleManager.sprint.posX = 5;
                this.aY = ModuleManager.sprint.posY = 5;
            }

        }

        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}