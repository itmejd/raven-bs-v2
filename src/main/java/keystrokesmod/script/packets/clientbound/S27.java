package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S27PacketExplosion;

public class S27 extends SPacket {
    public float strength;
    public Vec3 position;
    public Vec3 motion;

    public S27(S27PacketExplosion packet) {
        super(packet);
        this.strength = packet.getStrength();
        this.position = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        this.motion = new Vec3(packet.func_149149_c(), packet.func_149144_d(), packet.func_149147_e());
    }
}
