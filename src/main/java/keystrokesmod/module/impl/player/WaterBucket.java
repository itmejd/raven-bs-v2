package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WaterBucket extends Module {
    public ButtonSetting pickupWater;
    public ButtonSetting silentAim;
    public ButtonSetting switchToItem;

    private final long PLACE_DELAY = 500L;
    private final long PICKUP_WAIT = 150L;

    private long lastPlace = 0L;
    private boolean shouldPickup = false;
    private int lastSlot = -1;

    public WaterBucket() {
        super("Water Bucket", category.player);
        this.registerSetting(pickupWater = new ButtonSetting("Pickup water", true));
        this.registerSetting(silentAim = new ButtonSetting("Silent aim", true));
        this.registerSetting(switchToItem = new ButtonSetting("Switch to item", true));
    }

    @Override
    public void onDisable() {
        this.lastPlace = 0L;
        this.shouldPickup = false;
        this.lastSlot = -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || mc.isGamePaused() || mc.thePlayer.capabilities.isFlying || mc.thePlayer.capabilities.isCreativeMode) {
            return;
        }
        if (!fallCheck()) {
            return;
        }
        MovingObjectPosition mop = Utils.getTarget(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, silentAim.isToggled() ? 90.0f : mc.thePlayer.rotationPitch);
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mop.sideHit != EnumFacing.UP) {
            return;
        }
        long now = System.currentTimeMillis();
        if (Utils.timeBetween(lastPlace, now) < PLACE_DELAY) {
            return;
        }
        if (!isItem(mc.thePlayer.getHeldItem(), Items.water_bucket) && switchToItem.isToggled()) {
            this.attemptSwitch();
        }
        if (!silentAim.isToggled() && mc.thePlayer.rotationPitch < 80.0f) {
            return;
        }
        lastPlace = now;
        this.useCurrentItem();
        if (!(shouldPickup = pickupWater.isToggled())) {
            this.lastSlot = -1;
        }
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "&7Placed with motionY &d" + Utils.round(mc.thePlayer.motionY, 2) + " &7and fall distance &d" + Utils.round(mc.thePlayer.fallDistance, 2));
        }
    }

    @Override
    public void onUpdate() {
        if (mc.isGamePaused()) {
            return;
        }
        if (shouldPickup && Utils.timeBetween(lastPlace, System.currentTimeMillis()) > PICKUP_WAIT && isItem(mc.thePlayer.getHeldItem(), Items.bucket)) {
            shouldPickup = false;
            this.useCurrentItem();
            if (this.lastSlot != -1) {
                Utils.switchSlot(this.lastSlot, true);
                this.lastSlot = -1;
            }
        }
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (silentAim.isToggled() && (fallCheck() || Utils.timeBetween(lastPlace, System.currentTimeMillis()) < PLACE_DELAY) && getWaterBucketSlot() != -1) {
            e.setYaw(mc.thePlayer.rotationYaw);
            e.setPitch(90.0f);
        }
    }

    private void attemptSwitch() {
        int slot = getWaterBucketSlot();
        if (slot != -1) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
            Utils.switchSlot(slot, true);
        }
    }

    private int getWaterBucketSlot() {
        for (int slot = 0; slot < InventoryPlayer.getHotbarSize(); ++slot) {
            if (isItem(mc.thePlayer.inventory.getStackInSlot(slot), Items.water_bucket)) {
                return slot;
            }
        }
        return -1;
    }

    private void useCurrentItem() {
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
    }

    private boolean isItem(final ItemStack itemStack, final Item item) {
        return itemStack != null && itemStack.getItem() == item;
    }

    private boolean fallCheck() {
        return !mc.thePlayer.onGround && mc.thePlayer.fallDistance >= 3.3;
    }
}