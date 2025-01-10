package keystrokesmod.script.classes;

import keystrokesmod.utility.BlockUtils;
import net.minecraft.util.BlockPos;

public class Block {
    public String type;
    public String name;
    public boolean interactable;
    public int variant;
    public double height;
    public double width;
    public double length;
    public double x;
    public double y;
    public double z;

    public Block(net.minecraft.block.Block block, BlockPos blockPos) {
        this.type = block.getClass().getSimpleName();
        this.name = block.getRegistryName().replace("minecraft:", "");
        this.interactable = BlockUtils.isInteractable(block);
        this.variant = block.getMetaFromState(BlockUtils.getBlockState(blockPos));
        this.height = block.getBlockBoundsMaxY() - block.getBlockBoundsMinY();
        this.width = block.getBlockBoundsMaxX() - block.getBlockBoundsMinX();
        this.length = block.getBlockBoundsMaxZ() - block.getBlockBoundsMinZ();
        this.x = blockPos.getX();
        this.y = blockPos.getY();
        this.z = blockPos.getZ();
    }

    public Block(int x, int y, int z) {
        this(BlockUtils.getBlock(x, y, z), new BlockPos(x, y, z));
    }

    public Block(Vec3 position) {
        this(BlockUtils.getBlock(position.x, position.y, position.z), new BlockPos(position.x, position.y, position.z));
    }
}
