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
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;


public class NoSlow extends Module {
    public static SliderSetting sword, mode;
    public static SliderSetting slowed;
    public static ButtonSetting disableBow, disableSword, autoblockOnly;
    public static ButtonSetting disablePotions;
    public static ButtonSetting swordOnly;
    public ButtonSetting renderTimer;

    private String[] swordMode = new String[] { "Vanilla", "Item mode", "Fake" };
    private String[] modes = new String[] { "Vanilla", "Pre", "Post", "Alpha", "Float", "Blink" };

    private boolean postPlace;
    private boolean canFloat;
    public boolean noSlowing;
    public boolean offset;
    public boolean blockingClient;
    public static boolean blink;
    private int b4ticks;
    private boolean requireJump;
    private static boolean fix;
    private boolean didC, md;
    private boolean jumped;
    private boolean setCancelled;
    private boolean setJump;
    private static boolean hasClicked;

    public NoSlow() {
        super("NoSlow", category.movement, 0);
        this.registerSetting(new DescriptionSetting("Default is 80% motion reduction."));
        this.registerSetting(sword = new SliderSetting("Sword", 0, swordMode));
        this.registerSetting(mode = new SliderSetting("Item", 0, modes));
        this.registerSetting(renderTimer = new ButtonSetting("Render timer", false));
        this.registerSetting(slowed = new SliderSetting("Slow %", 80.0D, 0.0D, 80.0D, 1.0D));
        this.registerSetting(disableSword = new ButtonSetting("Disable sword", false));
        this.registerSetting(disableBow = new ButtonSetting("Disable bow", false));
        this.registerSetting(disablePotions = new ButtonSetting("Disable potions", false));
        this.registerSetting(autoblockOnly = new ButtonSetting("Sword only while autoblock", false));
        this.registerSetting(swordOnly = new ButtonSetting("Sword only", false));


    }

    public void guiUpdate() {
        this.renderTimer.setVisible(mode.getInput() == 5, this);
    }

    @Override
    public void onDisable() {
        resetFloat();
        if (blockingClient) {
            ReflectionUtils.setItemInUse(blockingClient = false);
        }
        blink = false;
        b4ticks = 0;
        cantBlock = hasClicked = false;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 1 && e.buttonstate) {
            handleFloatSetup();
            if (setCancelled) {
                setCancelled = false;
                e.setCanceled(true);
            }
        }

        if (sword.getInput() == 2 && Utils.tabbedIn() && !ModuleManager.killAura.blockingClient) {
            if (e.button == 1) {
                EntityLivingBase g = Utils.raytrace(4);
                if (Utils.holdingSword() && g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreUpdate(PreUpdateEvent e) {

        boolean apply = getSlowed() != 0.2f;
        EntityLivingBase g = Utils.raytrace(4);
        if (sword.getInput() == 2) {
            if (Utils.holdingSword() && g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        }
        if (ModuleManager.killAura.blockingClient) {
            blockingClient = false;
        }
        if (blockingClient && (!Mouse.isButtonDown(1) || !Utils.holdingSword())) {
            ReflectionUtils.setItemInUse(blockingClient = false);
        }
        if (sword.getInput() == 2) {
            if (blocking && (g == null && !BlockUtils.isInteractable(mc.objectMouseOver) || !Utils.holdingSword() && !Utils.keybinds.isMouseDown(1) || !Utils.tabbedIn())) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                blocking = false;
                hasClicked = false;
            }
            if (Utils.holdingSword()) {
                if (g == null && !BlockUtils.isInteractable(mc.objectMouseOver)) {
                    cantBlock = true;

                    if (Utils.tabbedIn() && Mouse.isButtonDown(1)) {
                        blockingClient = true;
                    }
                } else {
                    blocking = true;
                    cantBlock = false;
                    if (ModuleUtils.rcTick == 1) {
                        hasClicked = true;
                    }
                }
                if (blockingClient) {
                    ReflectionUtils.setItemInUse(blockingClient = true);
                    if (Mouse.isButtonDown(0)) {
                        mc.thePlayer.swingItem();
                    }
                }
            }
            else {
                cantBlock = false;
                hasClicked = false;
            }
        }
        else {
            cantBlock = false;
            hasClicked = false;
        }




        postPlace = false;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            b4ticks = 0;
            blink = false;
            return;
        }
        if (sword.getInput() != 1 && Utils.holdingSword()) {
            return;
        }
        switch ((int) mode.getInput()) {
            case 1: // pre
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                }
                break;
            case 2: // post
                postPlace = true;
                break;
            case 3: // alpha
                if (mc.thePlayer.ticksExisted % 3 == 0 && !Raven.packetsHandler.C07.sentCurrentTick.get()) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(new BlockPos(-1, -1, -1), 1, null, 0, 0, 0));
                }
                break;
            case 4: // float
                if (Mouse.isButtonDown(1)) {
                    handleFloatSetup();
                }
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
            case 5: // blink
                b4ticks++;
                if (b4ticks >= 12) blink = true;
                if (b4ticks > 33) {
                    blink = false;
                    b4ticks = 0;
                }
                break;
        }
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
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (setJump) {
            mc.thePlayer.movementInput.jump = true;
            setJump = false;
        }
    }

    private boolean blocking;

    public static boolean cantBlock;

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        if (!Mouse.isButtonDown(1)) {
            md = false;
        }

        postPlace = false;
        if (mode.getInput() != 4) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!Mouse.isButtonDown(1) || !holdingUsable() || !Utils.tabbedIn()) {
            resetFloat();
        }
        if (!apply || fix || didC) {
            return;
        }
        if (!canFloat && jumped && ModuleUtils.inAirTicks > 1) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
            canFloat = true;
            setCancelled = false;
        }
        else if (canFloat && canFloat() && !requireJump) {
            md = true;
            if (!mc.thePlayer.onGround) {
                //-0.0784000015258789 ground value
                //if (mc.thePlayer.motionY <= -0.1784000015258789) {
                    //e.setPosY(e.getPosY() + 1e-3);
                //} else {
                    e.setPosY(e.getPosY() - 1e-3);
                //}
            }
            else {
                e.setPosY(e.getPosY() + 1e-3);
            }
        }
    }

    private void handleFloatSetup() {
        boolean apply = getSlowed() != 0.2f;
        if (mode.getInput() != 4) {
            return;
        }
        if (!apply || fix || didC || !holdingUsable() || canFloat || jumped || BlockUtils.isInteractable(mc.objectMouseOver) || md) {
            return;
        }
        if (mc.thePlayer.onGround) {
            setJump = true;
            jumped = true;
            setCancelled = true;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        }
        else {
            canFloat = true;
        }
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
            if (mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && disableSword.isToggled()) {
                return 0.2f;
            }
            else if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()) && disablePotions.isToggled()) {
                return 0.2f;
            }
            else if (fix && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
                return 0.2f;
            }
            else if (!ModuleManager.killAura.hasAutoblocked && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && autoblockOnly.isToggled() && !(sword.getInput() == 2 && !hasClicked)) {
                return 0.2f;
            }
            else if (ModuleManager.killAura.autoBlockMode.getInput() == 7 && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword && ModuleManager.killAura.hasBlocked) {
                return 0.2f;
            }
        }
        float val = (100.0F - (float) slowed.getInput()) / 100.0F;
        return val;
    }

    public static boolean requireSlow() {
        if (mode.getInput() == 5 && !blink && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            return true;
        }
        return false;
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private void resetFloat() {
        noSlowing = false;
        fix = didC = requireJump = canFloat = jumped = md = setJump = setCancelled = false;
    }

    private boolean holdingUsable() {
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        Item heldItem = itemStack.getItem();
        if (heldItem == null) {
            return false;
        }
        if (heldItem instanceof ItemFood || heldItem instanceof ItemBucketMilk || (heldItem instanceof ItemBow && Utils.hasArrows(itemStack)) || (heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage()))) {
            return true;
        }
        if (sword.getInput() == 1 && heldItem instanceof ItemSword) {
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