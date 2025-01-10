package keystrokesmod.script.packets.clientbound;

import keystrokesmod.script.classes.Vec3;
import net.minecraft.network.play.server.S2APacketParticles;

public class S2A extends SPacket {
    public String type;
    public Vec3 position;
    public Vec3 offset;
    public float speed;
    public int count;
    public int[] args;

    public S2A(S2APacketParticles packet) {
        super(packet);
        this.type = packet.getParticleType().name();
        this.position = new Vec3(packet.getXCoordinate(), packet.getYCoordinate(), packet.getZCoordinate());
        this.offset = new Vec3(packet.getXOffset(), packet.getYOffset(), packet.getZOffset());
        this.speed = packet.getParticleSpeed();
        this.count = packet.getParticleCount();
        this.args = packet.getParticleArgs();
    }
}
