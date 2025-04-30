package keystrokesmod.utility;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.RunGameLoopEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Queue;

public class KnockBackHelper {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final Queue<Packet> packets = new ArrayDeque<>();

    private boolean cancel = false;

    private boolean releasePackets = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity || e.getPacket() instanceof S27PacketExplosion) {
            if (e.getPacket() instanceof S12PacketEntityVelocity &&
                    ((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId() &&
                    cancel)
            {
                this.packets.add(e.getPacket());
                e.setCanceled(true);
            }
            else if (e.getPacket() instanceof S27PacketExplosion && cancel) {
                this.packets.add(e.getPacket());
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onGameLoop(RunGameLoopEvent e) {
        if (!Utils.nullCheck() || !this.releasePackets) {
            return;
        }
        while (!this.packets.isEmpty()) {
            Packet p = this.packets.poll();
            if (p != null) p.processPacket(mc.getNetHandler());
        }
        this.releasePackets = false;
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            this.packets.clear(); // Clears the buffered packets when you join a new world.
        }
    }

    /**
     * Starts capturing packets, buffering them until {@link #releasePackets()} is called.
     */
    public void capturePackets() {
        this.cancel = true;
    }

    /**
     * Releases the buffered packets and sends them to your client.
     */
    public void releasePackets() {
        this.cancel = false;
        this.releasePackets = true;
    }

}