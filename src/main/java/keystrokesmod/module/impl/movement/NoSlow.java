package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class NoSlow extends Module {
    public static SliderSetting mode;
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
    private boolean setRotation;
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
                if (reSendConsume) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                        break;
                    } else {
                        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
                        canFloat = true;
                        reSendConsume = false;
                    }
                }
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
            if (groundSpeedOption.isToggled() && !Utils.jumpDown() && !ModuleManager.bhop.isEnabled() && Utils.isMoving() && !Utils.bowBackwards()) {
                Utils.setSpeed(getSpeedModifier());
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
        if (!Mouse.isButtonDown(1)) {
            resetFloat();
            noSlowing = false;
            //Utils.print("!Noslowing");
            return;
        }
        if (!canFloat) {
            return;
        }
        if (canFloat) {
            e.setPosY(e.getPosY() + 1E-14);
            noSlowing = true;
            //Utils.print("Noslowing");
        }
        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.moveStrafing == 0 && mc.thePlayer.moveForward <= 0 && Utils.isMoving()) {
                setRotation = true;
            } else {
                setRotation = false;
            }
        }
        if (Utils.noSlowingBackWithBow()) setRotation = false;
        if (groundSpeedOption.isToggled()) {
            if (setRotation) {
                if (!ModuleManager.killAura.isTargeting && !Utils.noSlowingBackWithBow()) {
                    float playerYaw = mc.thePlayer.rotationYaw;
                    e.setYaw(playerYaw -= 55);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPacketSend(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && mode.getInput() == 4 && getSlowed() != 0.2f && holdingConsumable(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack()) && !BlockUtils.isInteractable(mc.objectMouseOver) && holdingEdible(((C08PacketPlayerBlockPlacement) e.getPacket()).getStack())) {
            if (ModuleManager.skyWars.isEnabled() && Utils.getSkyWarsStatus() == 1) {
                return;
            }
            if (!canFloat) {
                if (!mc.thePlayer.onGround) {
                    canFloat = true;
                } else {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    reSendConsume = true;
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        if (reSendConsume) {
            e.setSprint(false);
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
        return groundSpeedOption.isToggled() && noSlowing;
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private void resetFloat() {
        reSendConsume = false;
        canFloat = setRotation = false;
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
        return speedModifier;
    }

    private boolean holdingConsumable(ItemStack itemStack) {
        Item heldItem = itemStack.getItem();
        if (heldItem instanceof ItemFood || heldItem instanceof ItemBow || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())) || (heldItem instanceof ItemSword && !vanillaSword.isToggled())) {
            return true;
        }
        return false;
    }

    private boolean holdingEdible(ItemStack stack) {
        if (stack.getItem() instanceof ItemFood && mc.thePlayer.getFoodStats().getFoodLevel() == 20) {
            ItemFood food = (ItemFood) stack.getItem();
            boolean alwaysEdible = false;
            try {
                alwaysEdible = Reflection.alwaysEdible.getBoolean(food);
            }
            catch (Exception e) {
                Utils.sendMessage("&cError checking food edibility, check logs.");
                e.printStackTrace();
            }
            return alwaysEdible;
        }
        return true;
    }
}
