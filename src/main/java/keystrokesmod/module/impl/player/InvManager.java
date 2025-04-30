package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

public class InvManager extends Module {
    private static SliderSetting stealerDelay;
    private static SliderSetting sortDelay;
    private static SliderSetting armorDelay;
    private static SliderSetting cleanerDelay;

    private SliderSetting swordSlot;
    private SliderSetting blocksSlot;
    private SliderSetting goldenAppleSlot;
    private SliderSetting projectileSlot;
    private SliderSetting speedSlot;
    private SliderSetting pearlSlot;
    private ButtonSetting autoArmor;
    private ButtonSetting autoSort;
    private ButtonSetting customChests;
    private ButtonSetting chestSteal;
    private ButtonSetting closeAfterStealing;
    private ButtonSetting invCleaner;
    private ButtonSetting clickToClean;

    private long ticks = 0l;
    private long nextDelay = 0l;
    private boolean closeGui = false;
    private double[] currentSword = new double[] { -1.0, -1.0 };

    private CurrentArmor[] armorArr = CurrentArmor.values();

    public InvManager() {
        super("InvManager", category.player);
        this.registerSetting(autoArmor = new ButtonSetting("Auto armor", false));
        this.registerSetting(armorDelay = new SliderSetting("Auto armor delay", 3.0, 1.0, 20.0, 1.0));

        this.registerSetting(autoSort = new ButtonSetting("Auto sort", false));
        this.registerSetting(sortDelay = new SliderSetting("Sort delay", 3.0, 1.0, 20.0, 1.0));

        this.registerSetting(chestSteal = new ButtonSetting("Steal chests", false));
        this.registerSetting(customChests = new ButtonSetting("Custom chest", false));
        this.registerSetting(closeAfterStealing = new ButtonSetting("Close after stealing", false));
        this.registerSetting(stealerDelay = new SliderSetting("Stealer delay", 3.0, 1.0, 20.0, 1.0));

        this.registerSetting(invCleaner = new ButtonSetting("Inventory cleaner", false));
        this.registerSetting(clickToClean = new ButtonSetting("Middle click to clean", false));

        this.registerSetting(cleanerDelay = new SliderSetting("Cleaner delay", 5.0, 1.0, 20.0, 1.0));
        this.registerSetting(swordSlot = new SliderSetting("Sword slot", true, -1, 1, 9, 1));
        this.registerSetting(blocksSlot = new SliderSetting("Blocks slot", true, -1, 1, 9, 1));
        this.registerSetting(goldenAppleSlot = new SliderSetting("Golden apple slot", true, -1, 1, 9, 1));
        this.registerSetting(projectileSlot = new SliderSetting("Projectile slot", true,-1, 1, 9, 1));
        this.registerSetting(speedSlot = new SliderSetting("Speed potion slot", true,-1, 1, 9, 1));
        this.registerSetting(pearlSlot = new SliderSetting("Pearl slot", true,-1, 1, 9, 1));
    }

    @Override
    public void onDisable() {
        this.nextDelay = 0L;
        this.ticks = 0L;
        this.closeGui = false;
    }

    @Override
    public void onUpdate() {
        if (mc.currentScreen == null) {
            return;
        }
        if (closeAfterStealing.isToggled() && this.closeGui) {
            this.closeGui = false;
            mc.thePlayer.closeScreen();
            return;
        }
        long ticks = this.ticks + 1L;
        this.ticks = ticks;
        if (ticks < this.nextDelay) {
            return;
        }
        this.ticks = 0L;
        if (mc.currentScreen instanceof GuiInventory) {
            if (autoArmor.isToggled() || autoSort.isToggled() || invCleaner.isToggled()) {
                this.updateCurrentArmor();
                InventoryData data = new InventoryData(mc.thePlayer.inventory, true, !autoSort.isToggled());
                if (autoArmor.isToggled()) {
                    for (int i = 0; i < data.armorData[0].length; ++i) {
                        if (data.armorData[1][i] != -1) {
                            CurrentArmor currentArmor = this.armorArr[i];
                            if (data.armorData[1][i] > currentArmor.defenceLevel) {
                                if (currentArmor.getItemStack() != null) {
                                    this.guiClick(currentArmor.invSlot, 0, 4, Delay.AUTOARMOR);
                                }
                                int slot = data.armorData[0][i];
                                this.guiClick((slot < 9) ? (slot + 36) : slot, 0, 1, Delay.AUTOARMOR);
                                return;
                            }
                        }
                    }
                }
                if (autoSort.isToggled()) {
                    if (this.fixSwordSlot(data, true)) {
                        return;
                    }
                    for (int i = 0; i < data.size; ++i) {
                        ItemStack itemStack = data.inventory.getStackInSlot(i);
                        if (itemStack != null && !this.isSword(itemStack)) {
                            if (!this.isArmor(itemStack)) {
                                if (itemStack.getItem() instanceof ItemBlock) {
                                    int slot = (int)(blocksSlot.getInput() - 1.0);
                                    if (slot <= -1) {
                                        if (i != slot) {
                                            ItemStack currentBlocks = data.inventory.getStackInSlot(slot);
                                            int i2 = (i < 9) ? (i + 36) : i;
                                            if (currentBlocks == null || !(currentBlocks.getItem() instanceof ItemBlock)) {
                                                this.guiClick(i2, slot, 2, Delay.SORT);
                                                return;
                                            }
                                            if (currentBlocks.stackSize < 64) {
                                                if (itemStack.getItem() == currentBlocks.getItem() && itemStack.getMetadata() == currentBlocks.getMetadata()) {
                                                    if (itemStack.stackSize == 64) {
                                                        this.guiClick(i2, slot, 2, Delay.SORT);
                                                        return;
                                                    }
                                                    if (data.emptyWithoutHotbar != 0 || i == i2) {
                                                        this.guiClick(i2, 0, 1, Delay.SORT);
                                                        return;
                                                    }
                                                }
                                                else if (itemStack.stackSize > currentBlocks.stackSize) {
                                                    this.guiClick(i2, slot, 2, Delay.SORT);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (itemStack.getItem() instanceof ItemAppleGold) {
                                    int slot = (int)(goldenAppleSlot.getInput() - 1.0);
                                    if (slot <= -1) {
                                        if (i != slot) {
                                            ItemStack currentGoldenApple = data.inventory.getStackInSlot(slot);
                                            if (currentGoldenApple == null || !(currentGoldenApple.getItem() instanceof ItemAppleGold)) {
                                                int i2 = (i < 9) ? (i + 36) : i;
                                                this.guiClick(i2, slot, 2, Delay.SORT);
                                                return;
                                            }
                                        }
                                    }
                                }
                                else if (itemStack.getItem() instanceof ItemSnowball || itemStack.getItem() instanceof ItemEgg) {
                                    int slot = (int)(projectileSlot.getInput() - 1.0);
                                    if (slot <= -1) {
                                        if (i != slot) {
                                            ItemStack currentProjectile = data.inventory.getStackInSlot(slot);
                                            int i2 = (i < 9) ? (i + 36) : i;
                                            if (currentProjectile == null || (!(currentProjectile.getItem() instanceof ItemSnowball) && !(currentProjectile.getItem() instanceof ItemEgg))) {
                                                this.guiClick(i2, slot, 2, Delay.SORT);
                                                return;
                                            }
                                            if (itemStack.stackSize >= 16) {
                                                if (itemStack.stackSize > currentProjectile.stackSize) {
                                                    this.guiClick(i2, slot, 2, Delay.SORT);
                                                    return;
                                                }
                                            }
                                            else if (currentProjectile.stackSize < 16) {
                                                if (itemStack.getItem() == currentProjectile.getItem()) {
                                                    if (data.emptyWithoutHotbar != 0 || i == i2) {
                                                        this.guiClick(i2, 0, 1, Delay.SORT);
                                                        return;
                                                    }
                                                }
                                                else if (itemStack.stackSize > currentProjectile.stackSize) {
                                                    this.guiClick(i2, slot, 2, Delay.SORT);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (itemStack.getItem() instanceof ItemPotion) {
                                    int slot = (int)(speedSlot.getInput() - 1.0);
                                    if (slot <= -1) {
                                        if (i != slot) {
                                            if (this.isSpeedPotion(itemStack)) {
                                                ItemStack currentPotion = data.inventory.getStackInSlot(slot);
                                                if (!this.isSpeedPotion(currentPotion)) {
                                                    int i2 = (i < 9) ? (i + 36) : i;
                                                    this.guiClick(i2, slot, 2, Delay.SORT);
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                                else if (itemStack.getItem() instanceof ItemEnderPearl) {
                                    int slot = (int)(pearlSlot.getInput() - 1.0);
                                    if (slot <= -1) {
                                        if (i != slot) {
                                            ItemStack currentPearl = data.inventory.getStackInSlot(slot);
                                            if (currentPearl == null || !(currentPearl.getItem() instanceof ItemEnderPearl)) {
                                                int i2 = (i < 9) ? (i + 36) : i;
                                                this.guiClick(i2, slot, 2, Delay.SORT);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (invCleaner.isToggled()) {
                    if (clickToClean.isToggled() && !Mouse.isButtonDown(2)) {
                        return;
                    }
                    List<Integer> duplicateItems = new ArrayList<>();
                    for (int j = 0; j < data.size; ++j) {
                        ItemStack itemStack2 = data.inventory.getStackInSlot(j);
                        if (itemStack2 != null) {
                            Item item = itemStack2.getItem();
                            int slot2 = (j < 9) ? (j + 36) : j;
                            if (this.isSword(itemStack2)) {
                                if (this.currentSword[1] <= Utils.getDamageLevel(itemStack2)) {
                                    continue;
                                }
                            }
                            else if (this.isArmor(itemStack2)) {
                                ItemArmor armor = (ItemArmor)item;
                                int armorSlot = 3 - armor.armorType;
                                int defenceLevel = this.getDefenceLevel(itemStack2);
                                if (this.armorArr[armorSlot].defenceLevel <= defenceLevel) {
                                    continue;
                                }
                            }
                            else {
                                if (item instanceof ItemBlock || item instanceof ItemAppleGold || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl || item == Items.arrow) {
                                    continue;
                                }
                                if (item == Items.spawn_egg) {
                                    continue;
                                }
                                if (item instanceof ItemPotion) {
                                    boolean shitPotion = false;
                                    for (PotionEffect effect : ((ItemPotion)item).getEffects(itemStack2)) {
                                        String desc = effect.toString();
                                        if (desc.contains("poison")) {
                                            shitPotion = true;
                                            break;
                                        }
                                    }
                                    if (!shitPotion) {
                                        continue;
                                    }
                                }
                                else if (itemStack2.getMaxStackSize() == 1) {
                                    int id = Item.getIdFromItem(item);
                                    if (!duplicateItems.contains(id)) {
                                        duplicateItems.add(id);
                                        continue;
                                    }
                                }
                            }
                            this.guiClick(slot2, 1, 4, Delay.CLEANER);
                            return;
                        }
                    }
                }
            }
        }
        else if (mc.currentScreen instanceof GuiChest && chestSteal.isToggled()) {
            IInventory chestInventory = ((ContainerChest)mc.thePlayer.openContainer).getLowerChestInventory();
            if (!customChests.isToggled() && !chestInventory.getName().contains("Chest")) {
                return;
            }
            this.updateCurrentArmor();
            InventoryData chestData = new InventoryData(chestInventory, false, false);
            InventoryData playerData = new InventoryData(mc.thePlayer.inventory, true, false);
            chestData.compareAndRemove(playerData);
            if (playerData.size != playerData.filled) {
                if (this.fixSwordSlot(chestData, false)) {
                    return;
                }
                for (int k = 0; k < chestData.armorData[0].length; ++k) {
                    if (chestData.armorData[1][k] != -1) {
                        CurrentArmor currentArmor2 = this.armorArr[k];
                        if (chestData.armorData[1][k] > currentArmor2.defenceLevel) {
                            this.guiClick(chestData.armorData[0][k], 0, 1, Delay.STEALER);
                            return;
                        }
                    }
                }
                for (int k = 0; k < chestData.size; ++k) {
                    ItemStack itemStack3 = chestData.inventory.getStackInSlot(k);
                    if (itemStack3 != null && !this.isSword(itemStack3) && !this.isArmor(itemStack3)) {
                        if (itemStack3.getItem() instanceof ItemBlock) {
                            int slot2 = (int)(blocksSlot.getInput() - 1.0);
                            if (slot2 <= -1) {
                                this.guiClick(k, 0, 1, Delay.STEALER);
                                return;
                            }
                            ItemStack currentBlocks2 = playerData.inventory.getStackInSlot(slot2);
                            if (currentBlocks2 == null || !(currentBlocks2.getItem() instanceof ItemBlock) || (((ItemBlock)itemStack3.getItem()).getBlock() != ((ItemBlock)currentBlocks2.getItem()).getBlock() && itemStack3.stackSize > currentBlocks2.stackSize)) {
                                this.guiClick(k, slot2, 2, Delay.STEALER);
                                return;
                            }
                        }
                        else if (itemStack3.getItem() instanceof ItemAppleGold) {
                            int slot2 = (int)(goldenAppleSlot.getInput() - 1.0);
                            if (slot2 <= -1) {
                                this.guiClick(k, 0, 1, Delay.STEALER);
                                return;
                            }
                            ItemStack currentGoldenApple2 = playerData.inventory.getStackInSlot(slot2);
                            if (currentGoldenApple2 == null || !(currentGoldenApple2.getItem() instanceof ItemAppleGold)) {
                                this.guiClick(k, slot2, 2, Delay.STEALER);
                                return;
                            }
                        }
                        else if (itemStack3.getItem() instanceof ItemSnowball || itemStack3.getItem() instanceof ItemEgg) {
                            int slot2 = (int)(projectileSlot.getInput() - 1.0);
                            if (slot2 <= -1) {
                                this.guiClick(k, 0, 1, Delay.STEALER);
                                return;
                            }
                            ItemStack currentProjectile2 = playerData.inventory.getStackInSlot(slot2);
                            if (currentProjectile2 == null || (!(currentProjectile2.getItem() instanceof ItemSnowball) && !(currentProjectile2.getItem() instanceof ItemEgg))) {
                                this.guiClick(k, slot2, 2, Delay.STEALER);
                                return;
                            }
                            if (itemStack3.stackSize > currentProjectile2.stackSize) {
                                if (itemStack3.getItem() == currentProjectile2.getItem()) {
                                    if (itemStack3.stackSize <= 16) {
                                        this.guiClick(k, 0, 1, Delay.STEALER);
                                    }
                                    else {
                                        this.guiClick(k, slot2, 2, Delay.STEALER);
                                    }
                                }
                                else {
                                    this.guiClick(k, slot2, 2, Delay.STEALER);
                                }
                                return;
                            }
                        }
                        else if (itemStack3.getItem() instanceof ItemPotion) {
                            if (this.isSpeedPotion(itemStack3)) {
                                int slot2 = (int)(speedSlot.getInput() - 1.0);
                                if (slot2 <= -1) {
                                    this.guiClick(k, 0, 1, Delay.STEALER);
                                    return;
                                }
                                ItemStack currentPotion2 = playerData.inventory.getStackInSlot(slot2);
                                if (!this.isSpeedPotion(currentPotion2)) {
                                    this.guiClick(k, slot2, 2, Delay.STEALER);
                                    return;
                                }
                            }
                        }
                        else if (itemStack3.getItem() instanceof ItemEnderPearl) {
                            int slot2 = (int)(pearlSlot.getInput() - 1.0);
                            if (slot2 <= -1) {
                                this.guiClick(k, 0, 1, Delay.STEALER);
                                return;
                            }
                            ItemStack currentPearl2 = playerData.inventory.getStackInSlot(slot2);
                            if (currentPearl2 == null || !(currentPearl2.getItem() instanceof ItemEnderPearl)) {
                                this.guiClick(k, slot2, 2, Delay.STEALER);
                                return;
                            }
                        }
                        this.guiClick(k, 0, 1, Delay.STEALER);
                        return;
                    }
                }
            }
            else {
                for (int k = 0; k < chestData.size; ++k) {
                    ItemStack itemStack3 = chestData.inventory.getStackInSlot(k);
                    if (itemStack3 != null) {
                        if (itemStack3.getItem() instanceof ItemBlock || itemStack3.getItem() instanceof ItemAppleGold || itemStack3.getItem() instanceof ItemSnowball || itemStack3.getItem() instanceof ItemEgg || itemStack3.getItem() instanceof ItemEnderPearl) {
                            for (int l = 0; l < playerData.size; ++l) {
                                ItemStack itemStack4 = playerData.inventory.getStackInSlot(l);
                                if (itemStack4 != null && itemStack3.getItem() == itemStack4.getItem()) {
                                    if (itemStack4.stackSize < itemStack4.getMaxStackSize()) {
                                        if (!(itemStack3.getItem() instanceof ItemBlock) || ((ItemBlock)itemStack3.getItem()).getBlock() == ((ItemBlock)itemStack4.getItem()).getBlock()) {
                                            this.guiClick(k, 0, 1, Delay.STEALER);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (closeAfterStealing.isToggled()) {
                this.closeGui = true;
            }
        }
    }

    private boolean isSword(ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemSword;
    }

    private boolean fixSwordSlot(InventoryData data, boolean playerInventory) {
        if (swordSlot.getInput() != 0.0 && data.swordData[1] != -1.0 && data.swordData[1] > this.currentSword[1]) {
            int slot = (int)data.swordData[0];
            if (playerInventory) {
                slot = ((slot < 9) ? (slot + 36) : slot);
            }
            this.guiClick(slot, (int)(swordSlot.getInput() - 1.0), 2, Delay.SORT);
            return true;
        }
        return false;
    }

    private boolean isArmor(ItemStack itemStack) {
        return itemStack.getItem() instanceof ItemArmor;
    }

    private int getDefenceLevel(ItemStack itemStack) {
        return ((ItemArmor)itemStack.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(new ItemStack[] { itemStack }, DamageSource.generic);
    }

    private void updateCurrentArmor() {
        for (CurrentArmor armor : this.armorArr) {
            ItemStack itemStack = armor.getItemStack();
            if (itemStack != null && this.isArmor(itemStack)) {
                armor.defenceLevel = this.getDefenceLevel(itemStack);
            }
            else {
                armor.defenceLevel = 0;
            }
        }
    }

    private boolean isSpeedPotion(ItemStack itemStack) {
        if (itemStack != null && itemStack.getItem() instanceof ItemPotion) {
            for (PotionEffect effect : ((ItemPotion)itemStack.getItem()).getEffects(itemStack)) {
                String desc = effect.toString();
                if (desc.contains("moveSpeed")) {
                    return true;
                }
            }
        }
        return false;
    }

    private void guiClick(int slot, int mouse, int mode, Delay delayType) {
        this.nextDelay = (long)delayType.slider.getInput();
        guiClick(slot, mouse, mode);
    }

    public static void guiClick(int slot, int mouse, int mode) {
        mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, mouse, mode, mc.thePlayer);
    }

    enum CurrentArmor
    {
        BOOTS(0, 8),
        LEGGINGS(1, 7),
        CHESTPLATE(2, 6),
        HELMET(3, 5);

        int slot;
        int invSlot;
        int defenceLevel;

        CurrentArmor(int slot, int invSlot) {
            this.slot = slot;
            this.invSlot = invSlot;
        }

        public ItemStack getItemStack() {
            return mc.thePlayer.inventory.armorItemInSlot(this.slot);
        }
    }

    class InventoryData
    {
        IInventory inventory;
        int size;
        int filled;
        int emptyWithoutHotbar;
        double[] swordData;
        int[][] armorData;

        InventoryData(IInventory inventory, boolean playerInventory, boolean armorOnly) {
            super();
            this.swordData = new double[] { -1.0, -1.0 };
            this.armorData = new int[][] { { -1, -1, -1, -1 }, { -1, -1, -1, -1 } };
            this.inventory = inventory;
            this.size = (playerInventory ? (inventory.getSizeInventory() - 4) : inventory.getSizeInventory());
            int currentSwordSlot = (int)(swordSlot.getInput() - 1.0) + (playerInventory ? 0 : ((this.size == 54) ? 81 : 54));
            for (int i = 0; i < this.size; ++i) {
                ItemStack itemStack = inventory.getStackInSlot(i);
                if (itemStack != null) {
                    ++this.filled;
                }
                else if (i >= 9) {
                    ++this.emptyWithoutHotbar;
                }
                if (!armorOnly) {
                    if (i == currentSwordSlot) {
                        if (itemStack != null && isSword(itemStack)) {
                            currentSword[1] = Utils.getDamageLevel(itemStack);
                            currentSword[0] = i;
                            continue;
                        }
                        currentSword[0] = (currentSword[1] = -1.0);
                    }
                    else if (itemStack != null && isSword(itemStack)) {
                        double damageLevel = Utils.getDamageLevel(itemStack);
                        if (damageLevel > this.swordData[1]) {
                            this.swordData[1] = damageLevel;
                            this.swordData[0] = i;
                        }
                        continue;
                    }
                }
                if (itemStack != null && isArmor(itemStack)) {
                    ItemArmor armor = (ItemArmor)itemStack.getItem();
                    int slot = 3 - armor.armorType;
                    int defenceLevel = getDefenceLevel(itemStack);
                    if (defenceLevel > this.armorData[1][slot]) {
                        this.armorData[1][slot] = defenceLevel;
                        this.armorData[0][slot] = i;
                    }
                }
            }
        }

        void compareAndRemove(InventoryData data) {
            if (data.swordData[1] > this.swordData[1]) {
                this.swordData[1] = -1.0;
            }
            for (int i = 0; i < this.armorData[0].length; ++i) {
                if (data.armorData[1][i] > this.armorData[1][i]) {
                    this.armorData[1][i] = -1;
                }
            }
        }
    }

    enum Delay
    {
        AUTOARMOR(armorDelay),
        SORT(sortDelay),
        STEALER(stealerDelay),
        CLEANER(cleanerDelay);

        SliderSetting slider;

        Delay(SliderSetting slider) {
            this.slider = slider;
        }
    }
}