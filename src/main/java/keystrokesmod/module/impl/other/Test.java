package keystrokesmod.module.impl.other;

import com.mojang.authlib.GameProfile;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Test extends Module {
    public EntityOtherPlayerMP fakeEntity = null;
    private ButtonSetting spawnDummy;
    private GroupSetting groupSetting;
    private SliderSetting testSlider;
    private ButtonSetting test;

    public Test() {
        super("Test", category.other);
        this.registerSetting(spawnDummy = new ButtonSetting("Spawn dummy", true));
        this.registerSetting(groupSetting = new GroupSetting("Group"));
        this.registerSetting(test = new ButtonSetting(groupSetting, "Test", true));
        this.registerSetting(testSlider = new SliderSetting(groupSetting, "Slider", 0, new String[] { "Option 1", "Option 2" } ));
    }

    public void onEnable() {
        if (spawnDummy.isToggled()) {
            fakeEntity = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(mc.thePlayer.getUniqueID(), "Dummy"));
            fakeEntity.copyLocationAndAnglesFrom(mc.thePlayer);
            mc.theWorld.addEntityToWorld(-8008, fakeEntity);
            fakeEntity.inventory.armorInventory[0] = new ItemStack(Items.golden_helmet);
            fakeEntity.setCurrentItemOrArmor(0, new ItemStack(Blocks.wool));
        }
    }

    public void onDisable() {
        if (fakeEntity != null) {
            mc.theWorld.removeEntity(fakeEntity);
            fakeEntity = null;
        }
    }
}
