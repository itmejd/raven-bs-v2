package keystrokesmod.module.impl.player;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiObby extends Module {

    public AntiObby() {
        super("AntiObby", category.player);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || e.isCanceled()) {
            return;
        }
        if (e.getPacket() instanceof S23PacketBlockChange) {
            if (BlockUtils.isObsidian(mc.objectMouseOver)) {
                e.setCanceled(true);
            }
        }
    }
}