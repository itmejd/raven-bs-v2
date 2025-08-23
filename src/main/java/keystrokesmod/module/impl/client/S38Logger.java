package keystrokesmod.module.impl.client;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;

public class S38Logger extends Module {
    public S38Logger() {
        super("S38Logger", category.client, 54);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S38PacketPlayerListItem) {
            S38PacketPlayerListItem s38 = (S38PacketPlayerListItem) e.getPacket();
            Utils.print(s38);
        }
    }
}
