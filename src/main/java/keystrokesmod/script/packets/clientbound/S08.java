package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class S08 extends SPacket {
    public Vec3 position;
    public float yaw;
    public float pitch;

    public S08(S08PacketPlayerPosLook packet) {
        super(packet);
        this.position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        this.yaw = packet.getYaw();
        this.pitch = packet.getPitch();
    }
}
