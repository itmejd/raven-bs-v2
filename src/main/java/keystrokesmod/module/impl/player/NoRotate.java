package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoRotate extends Module {
    private S08Info s08PacketData;
    private int receivedTick;

    private Packet sentPacket;
    private Packet recievedPacket;

    public NoRotate() {
        super("NoRotate", category.player);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook && mc.thePlayer != null && ((S08PacketPlayerPosLook) e.getPacket()).getPitch() != 0) {
            if (e.getPacket() == recievedPacket) {
                return;
            }
            e.setCanceled(true);
            S08PacketPlayerPosLook p = (S08PacketPlayerPosLook) e.getPacket();
            recievedPacket = (new S08PacketPlayerPosLook(p.getX(), p.getY(), p.getZ(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, p.func_179834_f()));
            //((S08PacketPlayerPosLook) recievedPacket).processPacket(mc.getNetHandler());
            checkThreadAndEnqueue(recievedPacket);
            s08PacketData = getPosAndRotation(p);
            receivedTick = mc.thePlayer.ticksExisted;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (s08PacketData != null && mc.thePlayer != null && e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            if (Utils.timeBetween(mc.thePlayer.ticksExisted, receivedTick) >= 2) {
                s08PacketData = null;
                return;
            }
            if (e.getPacket() == sentPacket) {
                return;
            }
            C03PacketPlayer.C06PacketPlayerPosLook p = (C03PacketPlayer.C06PacketPlayerPosLook) e.getPacket();
            e.setCanceled(true);
            sentPacket = (new C03PacketPlayer.C06PacketPlayerPosLook(p.getPositionX(), p.getPositionY(), p.getPositionZ(), s08PacketData.yaw, s08PacketData.pitch, p.isOnGround()));
            mc.getNetHandler().addToSendQueue(sentPacket);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7spoofing c06 immediately.");
            }
        }
    }

    public S08Info getPosAndRotation(final S08PacketPlayerPosLook packet) {
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();
        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X)) {
            x += mc.thePlayer.posX;
        }
        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
            y += mc.thePlayer.posY;
        }
        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
            z += mc.thePlayer.posZ;
        }
        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
            pitch += mc.thePlayer.rotationPitch;
        }
        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
            yaw += mc.thePlayer.rotationYaw;
        }
        return new S08Info(new Vec3(x, y, z), yaw, pitch);
    }

    public <T extends INetHandler> void checkThreadAndEnqueue(Packet<T> packet) {
        if (!mc.isCallingFromMinecraftThread()) {
            mc.addScheduledTask(() -> packet.processPacket((T) mc.getNetHandler()));
        }
    }

    class S08Info {
        Vec3 position;
        float yaw, pitch;

        public S08Info(Vec3 position, float yaw, float pitch) {
            this.position = position;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public boolean samePos(Vec3 position) {
            return this.position.xCoord == position.xCoord && this.position.yCoord == position.yCoord && this.position.zCoord == position.zCoord;
        }
    }
}