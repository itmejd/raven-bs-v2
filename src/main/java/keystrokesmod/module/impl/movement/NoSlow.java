package keystrokesmod.module.impl.movement;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSnow;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class NoSlow extends Module {
    public static SliderSetting sword, mode, blinkMode, vanillaMode;
    public static SliderSetting slowed;
    public static ButtonSetting disableBow;
    public static ButtonSetting disablePotions;
    public static ButtonSetting swordOnly;
    private ButtonSetting renderTimer;

    private String[] swordMode = new String[] { "Vanilla", "Item mode", "Fake" };
    private String[] modes = new String[] { "Vanilla", "Pre", "Post", "Alpha", "Float", "Blink" };
    private String[] blinkModes = new String[] { "Default", "Begin off ground" };
    private String[] vanillaModes = new String[] { "Default", "Only on ground" };

    private boolean postPlace;
    private boolean canFloat;
    public boolean noSlowing, offset;
    private int offsetDelay;
    private boolean setJump;
    private boolean bl;
    private boolean blink, wentOffGround;
    private ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
    private int color = new Color(0, 187, 255, 255).getRGB();
    private int blinkTicks, floatTicks;
    private boolean requireJump;
    private static boolean fix;
    private boolean didC;
    private boolean jumped, setCancelled;

    public NoSlow() {
        super("NoSlow", category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(sword = new SliderSetting("Sword", 0, swordMode));
        this.registerSetting(mode = new SliderSetting("Item", 0, modes));
        this.registerSetting(vanillaMode = new SliderSetting("Vanilla mode", 0, vanillaModes));
        this.registerSetting(blinkMode = new SliderSetting("Blink Mode", 0, blinkModes));
        this.registerSetting(renderTimer = new ButtonSetting("Render timer", false));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0D, 0.0D, 80.0D, 1.0D));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));
    }

    public void guiUpdate() {
        this.renderTimer.setVisible(mode.getInput() == 5, this);
        this.blinkMode.setVisible(mode.getInput() == 5, this);
        this.vanillaMode.setVisible(mode.getInput() == 0, this);
    }

    @Override
    public void onDisable() {
        resetFloat();
        noSlowing = false;
        if (bl) {
            ReflectionUtils.setItemInUse(false);
            bl = false;
        }
        release();
    }

    public void onUpdate() {
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            release();
            return;
        }
        postPlace = false;
        if (sword.getInput() != 1 && Utils.holdingSword()) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 1:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case 2:
                postPlace = true;
                break;
            case 3:
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                }
                break;
            case 4:
                handleFloatSetup();
                if (!blockConditions()) {
                    didC = true;
                    requireJump = true;
                }
                else if (didC) {
                    if (!mc.thePlayer.onGround) {
                        fix = true;
                    }
                }
                break;
            case 5:
                if (blinkConditions()) {
                    blink = true;
                }
                else {
                    release();
                }
                if (blink) blinkTicks++;
                break;
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !renderTimer.isToggled() || mode.getInput() != 5 || blinkTicks == 0 || blinkTicks >= 99999) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        Utils.handleTimer(color, blinkTicks);
    }

    @SubscribeEvent
    public void onPostMotion(PostMotionEvent e) {
        if (postPlace && mode.getInput() == 2) {
            if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            }
            postPlace = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (sword.getInput() == 2 && !ModuleManager.killAura.blockingClient) {
            if (Utils.holdingSword() && Utils.tabbedIn() && Mouse.isButtonDown(1)) {
                ReflectionUtils.setItemInUse(true);
                bl = true;
                if (Mouse.isButtonDown(0)) {
                    mc.thePlayer.swingItem();
                }
            }
            else if (bl) {
                ReflectionUtils.setItemInUse(false);
                bl = false;
            }
        }
        postPlace = false;
        if (mode.getInput() != 4) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!Mouse.isButtonDown(1) || !apply || fix || didC || !holdingUsable(mc.thePlayer.getHeldItem())) {
            resetFloat();
            noSlowing = false;
            offsetDelay = 0;
            if (!Mouse.isButtonDown(1)) {
                fix = didC = requireJump = false;
            }
            return;
        }
        if (!canFloat && jumped && ModuleUtils.inAirTicks > 1) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            canFloat = true;
        }
        else if (canFloat && canFloat() && !requireJump && (!jumped || ++offsetDelay > 1)) {
            if (!mc.thePlayer.onGround) {
                if (mc.thePlayer.motionY < -0.0784000015258789 && !(mc.thePlayer.posY % 1 == 0)) {
                    e.setPosY(e.getPosY() + 1e-3);
                } else {
                    e.setPosY(e.getPosY() - 1e-3);
                }
            }
            else {
                e.setPosY(e.getPosY() + 1e-3);
            }
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (sword.getInput() == 2 && Utils.tabbedIn() && !ModuleManager.killAura.blockingClient) {
            if (e.button == 1) {
                EntityLivingBase g = Utils.raytrace(4);
                if (Utils.holdingSword() && g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
                    e.setCanceled(true);
                }
            }
        }

        handleFloatSetup();

        if (setCancelled) {
            if (e.button == 1) {
                e.setCanceled(true);
            }
            setCancelled = false;
        }
    }

    private void handleFloatSetup() {
        boolean apply = getSlowed() != 0.2f;
        if (mode.getInput() != 4) {
            return;
        }
        if (!Mouse.isButtonDown(1) || !apply || fix || didC || !holdingUsable(mc.thePlayer.getHeldItem()) || canFloat || jumped) {
            return;
        }
        if (mc.thePlayer.onGround) {
            setJump = jumped = true;
            setCancelled = true;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
        else {
            canFloat = true;
        }
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (setJump) {
            mc.thePlayer.movementInput.jump = true;
            setJump = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (blink) {
            if (!e.isCanceled()) {
                blinkedPackets.add(e.getPacket());
                e.setCanceled(true);
            }
        }
    }

    private void release() {
        if (!blink) {
            return;
        }
        synchronized (blinkedPackets) {
            for (Packet packet : blinkedPackets) {
                Raven.packetsHandler.handlePacket(packet);
                PacketUtils.sendPacketNoEvent(packet);
            }
        }
        blinkedPackets.clear();
        blink = false;
        blinkTicks = 0;
        wentOffGround = false;
    }

    private boolean blinkConditions() {
        if (blinkMode.getInput() == 0) {
            return true;
        }
        if (blinkMode.getInput() == 1 && (!mc.thePlayer.onGround || wentOffGround)) {
            wentOffGround = true;
            return true;
        }

        return false;
    }

    private boolean blockConditions() {
        Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
        int edge = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
        if (mc.thePlayer.posY % 1 == 0) {
            return true;
        }
        if (edge < 10) {
            return true;
        }
        if (!mc.thePlayer.onGround) {
            return true;
        }
        if (block instanceof BlockSnow) {
            return true;
        }
        if (block instanceof BlockCarpet) {
            return true;
        }
        if (block instanceof BlockSlab) {
            return true;
        }
        return false;
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
            else if (fix && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return 0.2f;
            }
            else if (mode.getInput() == 0 && vanillaMode.getInput() == 1 && (!mc.thePlayer.onGround || Utils.jumpDown() || ModuleManager.bhop.isEnabled()) && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return 0.2f;
            }
        }
        float val = (100.0F - (float) slowed.getInput()) / 100.0F;
        return val;
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private void resetFloat() {
        jumped = false;
        canFloat = false;
        setJump = false;
        offsetDelay = 0;
        floatTicks = 0;
    }

    private boolean holdingUsable(ItemStack itemStack) {
        Item heldItem = itemStack.getItem();
        if (heldItem instanceof ItemFood || heldItem instanceof ItemBucketMilk || (heldItem instanceof ItemBow && Utils.hasArrows(itemStack)) || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())) || (heldItem instanceof ItemSword && sword.getInput() == 1)) {
            return true;
        }
        if (sword.getInput() == 1 && Utils.holdingSword()) {
            return true;
        }
        return false;
    }

    private boolean canFloat() {
        if (mc.thePlayer.isOnLadder() || mc.thePlayer.isInLava() || mc.thePlayer.isInWater()) {
            return false;
        }
        return true;
    }

    private ItemStack getMaxBook() {
        ItemStack stack = new ItemStack(Items.golden_apple);
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList pages = new NBTTagList();

        for (int i =0; i < 50; i++) {
            pages.appendTag(new NBTTagString("NIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGERNIGGER"));
        }

        tag.setTag("pages", pages);
        tag.setString("author", "George Floyd");
        tag.setString("title", "History of the KKK");
        stack.setTagCompound(tag);
        return stack;
    }
}