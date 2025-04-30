package keystrokesmod.utility;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class BlockUtils {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isSamePos(BlockPos blockPos, BlockPos blockPos2) {
        return blockPos == blockPos2 || (blockPos.getX() == blockPos2.getX() && blockPos.getY() == blockPos2.getY() && blockPos.getZ() == blockPos2.getZ());
    }

    public static boolean notFull(Block block) {
        return block instanceof BlockFenceGate || block instanceof BlockLadder || block instanceof BlockFlowerPot || block instanceof BlockBasePressurePlate || isFluid(block) || block instanceof BlockFence || block instanceof BlockAnvil || block instanceof BlockEnchantmentTable || block instanceof BlockChest;
    }

    public static boolean isNormalBlock(final Block block) {
        return block == Blocks.glass || (block.isFullBlock() && block != Blocks.gravel && block != Blocks.sand && block != Blocks.soul_sand && block != Blocks.tnt && block != Blocks.crafting_table && block != Blocks.furnace && block != Blocks.dispenser && block != Blocks.dropper && block != Blocks.noteblock && block != Blocks.command_block);
    }


    public static BlockPos pos(final double x, final double y, final double z) {
        return new BlockPos(x, y, z);
    }

    public static boolean isBlockPosEqual(final BlockPos pos1, final BlockPos pos2) {
        return pos1 == pos2 || (pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ());
    }

    public static BlockPos offsetPos(MovingObjectPosition mop) {
        return mop.getBlockPos().offset(mop.sideHit);
    }

    public static boolean isFluid(Block block) {
        return block.getMaterial() == Material.lava || block.getMaterial() == Material.water;
    }

    public static boolean isInteractable(Block block) {
        return block instanceof BlockFurnace || block instanceof BlockTrapDoor || block instanceof BlockDoor || block instanceof BlockContainer || block instanceof BlockJukebox || block instanceof BlockFenceGate || block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockEnchantmentTable || block instanceof BlockBrewingStand || block instanceof BlockBed || block instanceof BlockDropper || block instanceof BlockDispenser || block instanceof BlockHopper || block instanceof BlockAnvil || block instanceof BlockNote || block instanceof BlockWorkbench;
    }

    public static boolean isInteractable(MovingObjectPosition mv) {
        if (mv == null || mv.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || mv.getBlockPos() == null) {
            return false;
        }
        if (!mc.thePlayer.isSneaking() || mc.thePlayer.getHeldItem() == null) {
            return isInteractable(BlockUtils.getBlock(mv.getBlockPos()));
        }
        return false;
    }

    public static float getBlockHardness(final Block block, final ItemStack itemStack, boolean ignoreSlow, boolean ignoreGround) {
        final float getBlockHardness = block.getBlockHardness(mc.theWorld, null);
        if (getBlockHardness < 0.0f) {
            return 0.0f;
        }
        return (block.getMaterial().isToolNotRequired() || (itemStack != null && itemStack.canHarvestBlock(block))) ? (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 30.0f) : (getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 100.0f);
    }

    public static float getToolDigEfficiency(ItemStack itemStack, Block block, boolean ignoreSlow, boolean ignoreGround) {
        float n = (itemStack == null) ? 1.0f : itemStack.getItem().getStrVsBlock(itemStack, block);
        if (n > 1.0f) {
            final int getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
            if (getEnchantmentLevel > 0 && itemStack != null) {
                n += getEnchantmentLevel * getEnchantmentLevel + 1;
            }
        }
        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            n *= 1.0f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }
        if (!ignoreSlow) {
            if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
                float n2;
                switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                    case 0: {
                        n2 = 0.3f;
                        break;
                    }
                    case 1: {
                        n2 = 0.09f;
                        break;
                    }
                    case 2: {
                        n2 = 0.0027f;
                        break;
                    }
                    default: {
                        n2 = 8.1E-4f;
                        break;
                    }
                }
                n *= n2;
            }
            if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
                n /= 5.0f;
            }
            if (!mc.thePlayer.onGround && !ignoreGround) {
                n /= 5.0f;
            }
        }
        return n;
    }

    public static Block getBlock(BlockPos blockPos) {
        return getBlockState(blockPos).getBlock();
    }

    public static Block getBlock(double x, double y, double z) {
        return getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos);
    }

    public static boolean check(final BlockPos blockPos, final Block block) {
        return getBlock(blockPos) == block;
    }

    public static boolean replaceable(BlockPos blockPos) {
        if (!Utils.nullCheck()) {
            return true;
        }
        return getBlock(blockPos).isReplaceable(mc.theWorld, blockPos);
    }

    public static boolean canSeeVecBlock(final BlockPos pos, final Vec3 vecPlayer, final Vec3 vecBlockPoint) {
        final MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vecPlayer, vecBlockPoint, false, false, false);
        if (mop == null) {
            return true;
        }
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            final BlockPos mopPos = mop.getBlockPos();
            if (mopPos.getX() == pos.getX() && mopPos.getY() == pos.getY() && mopPos.getZ() == pos.getZ()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canBlockBeSeen(final BlockPos pos) {
        final Vec3 vecPlayer = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        for (double offsetY = 0.0; offsetY <= 0.5; offsetY += 0.5) {
            final double y = pos.getY() + offsetY;
            Vec3 vecBlockPoint = new Vec3(pos.getX() + 1, y, pos.getZ() + 0.5);
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.getX(), y, pos.getZ() + 0.5);
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.getX() + 0.5, y, (double)(pos.getZ() + 1));
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
            vecBlockPoint = new Vec3(pos.getX() + 0.5, y, (double)pos.getZ());
            if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
                return true;
            }
        }
        return false;
    }

    public static EnumDyeColor getWoolColor(final IBlockState state) {
        return (EnumDyeColor)state.getProperties().get(BlockColored.COLOR);
    }
}
