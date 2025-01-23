package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.mixin.impl.accessor.IAccessorItemFood;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class NoSlow extends Module {
    public SliderSetting mode;
    public static SliderSetting slowed;
    public static ButtonSetting disableBow;
    public static ButtonSetting disablePotions;
    public static ButtonSetting swordOnly;
    public static ButtonSetting vanillaSword;
    public static ButtonSetting groundSpeedOption;
    private String[] modes = new String[]{"Vanilla", "Pre", "Post", "Alpha", "Float"};
    private boolean postPlace;
    private boolean canFloat;
    private boolean reSendConsume;
    public static boolean noSlowing;

    public NoSlow() {
        super("NoSlow", category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0D, 0.0D, 80.0D, 1.0D));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
        this.registerSetting(vanillaSword = new ButtonSetting("Vanilla sword", false));
        this.registerSetting(groundSpeedOption = new ButtonSetting("Ground Speed", false));
    }

    @Override
    public void onDisable() {
        resetFloat();
    }

    public void onUpdate() {
        if (ModuleManager.bedAura.stopAutoblock) {
            return;
        }
        postPlace = false;
        if (vanillaSword.isToggled() && Utils.holdingSword()) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 1:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case 2:
                postPlace = true;
                break;
            case 3:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                }
                break;
            case 4:
                //
                break;
        }
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (postPlace && mode.getInput() == 2) {
            if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.get()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
            postPlace = false;
        }
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if ((canFloat && mc.thePlayer.onGround)) {
            if (groundSpeedOption.isToggled() && !Utils.jumpDown() && !ModuleManager.bhop.isEnabled() && Utils.keysDown() && !Utils.bowBackwards()) {
                Utils.setSpeed(getSpeedModifier());
                //Utils.print("ground speed");
            }
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (ModuleManager.bedAura.stopAutoblock || mode.getInput() != 4) {
            resetFloat();
            return;
        }
        postPlace = false;
        if (mc.thePlayer.getHeldItem() != null && holdingConsumable(mc.thePlayer.getHeldItem()) && !Mouse.isButtonDown(1)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
        if (!Mouse.isButtonDown(1) || (mc.thePlayer.getHeldItem() == null || !holdingConsumable(mc.thePlayer.getHeldItem()))) {
            resetFloat();
            noSlowing = false;
            //Utils.print("!Noslowing");
            return;
        }
        if (reSendConsume) {
            if (ModuleUtils.inAirTicks > 1) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                canFloat = true;
                reSendConsume = false;
            }
        }
        if (!canFloat) {
            return;
        }
        e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
        noSlowing = true;
        if (groundSpeedOption.isToggled()) {
            if (!ModuleManager.killAura.isTargeting && !Utils.noSlowingBackWithBow() && !Utils.jumpDown() && mc.thePlayer.moveForward <= -0.5 && mc.thePlayer.moveStrafing == 0 && Utils.isMoving() && mc.thePlayer.onGround) {
                float yaw = mc.thePlayer.rotationYaw;
                e.setYaw(yaw - 55);
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && mode.getInput() == 4 && getSlowed() != 0.2f && holdingConsumable(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack()) && !BlockUtils.isInteractable(mc.objectMouseOver) && Utils.holdingEdible(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack())) {
            if (ModuleManager.skyWars.isEnabled() && Utils.getSkyWarsStatus() == 1 || canFloat || reSendConsume) {
                return;
            }
            if (!mc.thePlayer.onGround) {
                canFloat = true;
            }
            else {
                if (!Utils.jumpDown()) {
                    mc.thePlayer.jump();
                }
                reSendConsume = true;
                canFloat = false;
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent e) {
        if (mode.getInput() == 4 && getSlowed() != 0.2f && !canFloat) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    public static float getSlowed() {
        if (mc.thePlayer.getHeldItem() == null || ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled()) {
            return 0.2f;
        }
        else {
            if (swordOnly.isToggled() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return 0.2f;
            }
            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && disableBow.isToggled()) {
                return 0.2f;
            }
            else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()) && disablePotions.isToggled()) {
                return 0.2f;
            }
        }
        float val = (100.0F - (float) slowed.getInput()) / 100.0F;
        return val;
    }

    public static boolean groundSpeed() {
        return groundSpeedOption.isToggled() && noSlowing && Utils.isMoving() && !Utils.jumpDown();
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private void resetFloat() {
        reSendConsume = false;
        canFloat = false;
    }

    private double getSpeedModifier() {
        double speedModifier = 0.2;
        final int speedAmplifier = Utils.getSpeedAmplifier();
        switch (speedAmplifier) {
            case 0:
                speedModifier = 0.2;
                break;
            case 1:
                speedModifier = 0.23;
                break;
            case 2:
                speedModifier = 0.28;
                break;
            case 3:
                speedModifier = 0.32;
                break;
            case 4:
                speedModifier = 0.37;
                break;
        }
        return speedModifier - 0.005;
    }

    private boolean holdingConsumable(ItemStack itemStack) {
        Item heldItem = itemStack.getItem();
        if (heldItem instanceof ItemFood || heldItem instanceof ItemBow || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())) || (heldItem instanceof ItemSword && !vanillaSword.isToggled())) {
            return true;
        }
        return false;
    }


    public static boolean holdingEdible(ItemStack stack) {
        if (stack.getItem() instanceof ItemFood && mc.thePlayer.getFoodStats().getFoodLevel() == 20) {
            ItemFood food = (ItemFood) stack.getItem();
            return ((IAccessorItemFood) food).getAlwaysEdible();
        }
        return true;
    }
}
