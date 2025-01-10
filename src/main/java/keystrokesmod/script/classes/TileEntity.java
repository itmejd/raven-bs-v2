package keystrokesmod.script.classes;

import net.minecraft.tileentity.TileEntitySkull;

public class TileEntity {
    private net.minecraft.tileentity.TileEntity tileEntity;
    private Vec3 position;

    public String type;
    public String name;

    public TileEntity(net.minecraft.tileentity.TileEntity tileEntity) {
        this.tileEntity = tileEntity;
        this.position = new Vec3(tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
        this.type = tileEntity.getBlockType().getClass().getSimpleName();
        this.name = tileEntity.getBlockType().getRegistryName().replace("minecraft:", "");
    }

    public Vec3 getPosition() {
        return position;
    }

    public Object[] getSkullData() {
        if (!(this.tileEntity instanceof TileEntitySkull)) {
            return null;
        }
        TileEntitySkull skull = (TileEntitySkull) this.tileEntity;
        String name = "";
        String uuid = "";
        if (skull.getPlayerProfile() != null) {
            name = skull.getPlayerProfile().getName();
            uuid = skull.getPlayerProfile().getId().toString();
        }
        return new Object[] { skull.getSkullType(), skull.getSkullRotation(), name, uuid };
    }
}
