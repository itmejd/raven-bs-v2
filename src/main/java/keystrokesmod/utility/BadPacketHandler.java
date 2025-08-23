package keystrokesmod.utility;

import keystrokesmod.event.*;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class BadPacketHandler {
    private final Minecraft mc;

    public BadPacketHandler(Minecraft mc) {
        this.mc = mc;
    }

    private static int c08Tick;
    private static int c07Tick;
    private static int c02Tick;
    private static int c02ITick;
    private static AtomicInteger playerSlot = new AtomicInteger(-1);

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.entity == mc.thePlayer) {
            playerSlot.set(-1);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientRotation(ClientRotationEvent e) {
        if (e.getPitch() > 90F) {
            Utils.sendMessage("&7bad packet detected (>90 PITCH)");
            e.setPitch(90F);
        }
        if (e.getPitch() < -90F) {
            Utils.sendMessage("&7bad packet detected (<-90 PITCH)");
            e.setPitch(-90F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.isCanceled()) {
            return;
        }
        Packet p = e.getPacket();

        if (p instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity) p;

            if (Objects.equals(String.valueOf(c02.getAction()), "ATTACK")) {
                c02Tick = mc.thePlayer.ticksExisted;
            }
            if (Objects.equals(String.valueOf(c02.getAction()), "INTERACT") || Objects.equals(String.valueOf(c02.getAction()), "INTERACT_AT")) {
                c02ITick = mc.thePlayer.ticksExisted;
            }

            if (mc.thePlayer.ticksExisted == c08Tick) {
                Utils.sendMessage("&7bad packet detected (block / attack (before attack))");
                e.setCanceled(true);
            }
            if (mc.thePlayer.ticksExisted == c07Tick) {
                Utils.sendMessage("&7bad packet detected (dig / attack)");
                e.setCanceled(true);
            }
        }
        if (p instanceof C08PacketPlayerBlockPlacement) {
            c08Tick = mc.thePlayer.ticksExisted;

            if (mc.thePlayer.ticksExisted == c07Tick) {
                Utils.sendMessage("&7bad packet detected (dig / block)");
                e.setCanceled(true);
            }
            if (mc.thePlayer.ticksExisted == c02Tick && mc.thePlayer.ticksExisted != c02ITick) {
                Utils.sendMessage("&7bad packet detected (attack / block (no interact))");
                e.setCanceled(true);
            }
        }
        if (p instanceof C07PacketPlayerDigging) {
            c07Tick = mc.thePlayer.ticksExisted;

            if (mc.thePlayer.ticksExisted == c08Tick) {
                Utils.sendMessage("&7bad packet detected (block / dig)");
                e.setCanceled(true);
            }
            if (mc.thePlayer.ticksExisted == c02Tick) {
                Utils.sendMessage("&7bad packet detected (attack / dig)");
                e.setCanceled(true);
            }
        }
        if (p instanceof C09PacketHeldItemChange) {
            C09PacketHeldItemChange c09 = (C09PacketHeldItemChange) p;
            if (c09.getSlotId() == playerSlot.get()) {
                Utils.sendMessage("&7bad packet detected (double slot)");
                e.setCanceled(true);
            }
            playerSlot.set(c09.getSlotId());
        }
    }
}