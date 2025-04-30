package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.PlayerState;
import keystrokesmod.script.packet.clientbound.SPacket;
import keystrokesmod.script.packet.serverbound.CPacket;
import keystrokesmod.script.packet.serverbound.PacketHandler;
import keystrokesmod.utility.Utils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class ScriptEvents {
    public Module module;

    public ScriptEvents(Module module) {
        this.module = module;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Utils.nullCheck()) {
            return;
        }
        if (Utils.stripColor(e.message.getUnformattedText()).isEmpty()) {
            return;
        }
        if (Raven.scriptManager.invokeBoolean("onChat", module, e.message.getUnformattedText()) == 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent e) {
        if (e.isCanceled() || e.getPacket() == null) {
            return;
        }
        if (e.getPacket().getClass().getSimpleName().startsWith("S")) {
            return;
        }
        CPacket packet = PacketHandler.convertServerBound(e.getPacket());
        if (packet != null && Raven.scriptManager.invokeBoolean("onPacketSent", module, packet) == 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.isCanceled() || e.getPacket() == null) {
            return;
        }
        SPacket packet = PacketHandler.convertClientBound(e.getPacket());
        if (packet != null && Raven.scriptManager.invokeBoolean("onPacketReceived", module, packet) == 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEvent e) {
        if (e.isCanceled()) {
            return;
        }
        Entity target = Entity.convert(e.target);
        Entity attacker = Entity.convert(e.attacker);
        if (Raven.scriptManager.invokeBoolean("onAttackEntity", module, target, attacker) == 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientRotations(ClientRotationEvent e) {
        Float[] rotations = Raven.scriptManager.invokeFloatArray("getRotations", module);
        if (rotations == null || rotations.length == 0 || rotations.length > 2) {
            return;
        }
        if (rotations[0] != null) {
            e.yaw = rotations[0];
        }
        if (rotations.length == 2 && rotations[1] != null) {
            e.pitch = rotations[1];
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyTyped(KeyPressEvent e) {
        if (e.isCanceled()) {
            return;
        }
        if (Raven.scriptManager.invokeBoolean("onKeyPress", module, e.typedChar, e.keyCode) == 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Raven.scriptManager.invoke("onRenderWorld", module, e.partialTicks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreUpdate(PreUpdateEvent e) {
        Raven.scriptManager.invoke("onPreUpdate", module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostUpdate(PostUpdateEvent e) {
        Raven.scriptManager.invoke("onPostUpdate", module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }
        Raven.scriptManager.invoke("onRenderTick", module, e.renderTickTime);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAntiCheatFlag(AntiCheatFlagEvent e) {
        Raven.scriptManager.invoke("onAntiCheatFlag", module, e.flag, Entity.convert(e.entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiUpdate(GuiUpdateEvent e) {
        if (e.guiScreen == null) {
            return;
        }
        Raven.scriptManager.invoke("onGuiUpdate", module, e.guiScreen.getClass().getSimpleName(), e.opened);
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        Raven.scriptManager.invoke("onDisconnect", module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreMotion(PreMotionEvent e) {
        PlayerState playerState = new PlayerState(e, (byte) 0);
        Raven.scriptManager.invoke("onPreMotion", module, playerState);
        if (e.isEquals(playerState)) {
            return;
        }
        if (e.getYaw() != playerState.yaw) {
            e.setYaw(playerState.yaw);
        }
        e.setPitch(playerState.pitch);
        e.setPosX(playerState.x);
        e.setPosY(playerState.y);
        e.setPosZ(playerState.z);
        e.setOnGround(playerState.onGround);
        e.setSprinting(playerState.isSprinting);
        e.setSneaking(playerState.isSneaking);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == null) {
            return;
        }
        Raven.scriptManager.invoke("onWorldJoin", module, Entity.convert(e.entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostInput(PostPlayerInputEvent e) {
        Raven.scriptManager.invoke("onPostPlayerInput", module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostMotion(PostMotionEvent e) {
        Raven.scriptManager.invoke("onPostMotion", module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (Raven.scriptManager.invokeBoolean("onMouse", module, e.button, e.buttonstate) == 0) {
            e.setCanceled(true);
        }
    }
}