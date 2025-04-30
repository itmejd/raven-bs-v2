package keystrokesmod.module.impl.other;

import keystrokesmod.event.SendAllPacketsEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DebugAC extends Module {
    private ButtonSetting debugMob;
    private ButtonSetting debugBlock;
    private ButtonSetting alertPost;
    public static long lastC03;

    public DebugAC() {
        super("Debug AC", category.other);
        this.registerSetting(debugBlock = new ButtonSetting("Debug block", true));
        this.registerSetting(debugMob = new ButtonSetting("Debug mob", true));
        this.registerSetting(alertPost = new ButtonSetting("Alert post", false));
    }

    public void onDisable() {

    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }

        if (debugBlock.isToggled()) {
            MovingObjectPosition mouse = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, false);
            if (mouse == null || mouse.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mouse.getBlockPos() == null) {
                return;
            }

            BlockPos pos = mouse.getBlockPos();
            Block block = BlockUtils.getBlock(pos);
            if (block == null || block == Blocks.air) {
                return;
            }

            IBlockState state = mc.theWorld.getBlockState(pos);

            mc.fontRendererObj.drawStringWithShadow("§7BlockPos: §b" + pos.getX() + "§7, §b" + pos.getY() + "§7, §b" + pos.getZ(), 30, 20, -1);
            mc.fontRendererObj.drawStringWithShadow("§7HitVec: §b" + Utils.round(mouse.hitVec.xCoord, 3) + "§7, §b" + Utils.round(mouse.hitVec.yCoord, 3) + "§7, §b" + Utils.round(mouse.hitVec.zCoord, 3), 30, 30, -1);
            mc.fontRendererObj.drawStringWithShadow("§7Face: §b" + mouse.sideHit.name(), 30, 40, -1);
            mc.fontRendererObj.drawStringWithShadow("§7Unlocalized Name: §b" + block.getUnlocalizedName(), 30, 50, -1);
            mc.fontRendererObj.drawStringWithShadow("§7Registry Name: §b" + block.getRegistryName(), 30, 60, -1);

            int y = 70;

            for (IProperty<?> property : block.getBlockState().getProperties()) {
                Class<?> valueClass = property.getValueClass();
                String propName = property.getName();

                Object currentValue = state.getValue(property);

                Collection<?> allowedValues = property.getAllowedValues();

                mc.fontRendererObj.drawStringWithShadow("§7Property Name: §b" + propName, 30, y, -1);
                y += 10;
                mc.fontRendererObj.drawStringWithShadow("§7Value Type: §b" + valueClass.getName(), 30, y, -1);
                y += 10;

                if (currentValue != null) {
                    mc.fontRendererObj.drawStringWithShadow("§7Current Value: §b" + currentValue, 30, y, -1);
                    y += 10;

                    if (valueClass.isEnum()) {
                        Enum<?> enumValue = (Enum<?>) currentValue;
                        mc.fontRendererObj.drawStringWithShadow("§7Current Enum Name: §b" + enumValue.name(), 30, y, -1);
                        y += 10;
                        mc.fontRendererObj.drawStringWithShadow("§7Current Enum Ordinal: §b" + enumValue.ordinal(), 30, y, -1);
                        y += 10;
                    }
                }

                mc.fontRendererObj.drawStringWithShadow("§7Allowed Values: §b" + allowedValues, 30, y, -1);
                y += 10;

                if (valueClass.isEnum()) {
                    Object[] enumConstants = valueClass.getEnumConstants();
                    if (Arrays.toString(enumConstants).equals(allowedValues.toString())) {
                        break;
                    }
                    mc.fontRendererObj.drawStringWithShadow("§7Enum Constants: §b" + Arrays.toString(enumConstants), 30, y, -1);
                    y += 10;
                }

                y += 5;
            }
        }
        if (debugMob.isToggled()) {
            MovingObjectPosition movingObjectPosition = mc.objectMouseOver;
            if (movingObjectPosition == null || movingObjectPosition.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || movingObjectPosition.entityHit == null || !(movingObjectPosition.entityHit instanceof EntityCreature)) {
                return;
            }
            int xPos = 30;
            int yPos = 20;
            EntityCreature mob = (EntityCreature) movingObjectPosition.entityHit;
            String mobName = mob.getName();
            String attackerName = mob.getAttackTarget() == null ? "&7null" : mob.getAttackTarget().getDisplayName().getFormattedText();
            List<String> info = Arrays.asList(mobName, attackerName);
            for (String data : info) {
                mc.fontRendererObj.drawStringWithShadow(data, xPos, yPos, -1);
                yPos += mc.fontRendererObj.FONT_HEIGHT + 3;
            }
        }
    }

    @SubscribeEvent
    public void onAllPacketSent(SendAllPacketsEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer) {
            lastC03 = System.currentTimeMillis();
        }
    }
}
