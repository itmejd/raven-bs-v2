package keystrokesmod.module.impl.minigames;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.*;

public class SkyWars extends Module {
    public ButtonSetting strengthIndicator;
    public Map<EntityPlayer, Long> strengthPlayers = new HashMap<>();
    private int strengthColor = new Color(255, 0, 0).getRGB();
    private String[] killMessages = new String[] {" by ", " to ", " with ", " of ", " from ", " knight ", " for "};
    public SkyWars() {
        super("Sky Wars", category.minigames);
        this.registerSetting(strengthIndicator = new ButtonSetting("Strength indicator", true));
    }

    @Override
    public void onDisable() {
        strengthPlayers.clear();
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!strengthIndicator.isToggled() || !Utils.nullCheck() || strengthPlayers.isEmpty() || Utils.getSkyWarsStatus() != 2) {
            return;
        }
        ArrayList<EntityPlayer> keysList = new ArrayList<>(strengthPlayers.keySet());
        for (EntityPlayer entityPlayer : keysList) {
            long storedTime = strengthPlayers.get(entityPlayer);
            long timePassed = System.currentTimeMillis() - storedTime;
            if (timePassed < 5000 && !AntiBot.isBot(entityPlayer)) {
                continue;
            }
            strengthPlayers.remove(entityPlayer);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type == 2 || !Utils.nullCheck() || !strengthIndicator.isToggled() || Utils.getSkyWarsStatus() != 2) {
            return;
        }
        String stripped = Utils.stripColor(e.message.getUnformattedText());
        if (stripped.isEmpty()) {
            return;
        }
        if (stripped.endsWith(".") && Arrays.stream(killMessages).anyMatch(stripped::contains)) {
            String[] parts = stripped.split(" ");
            for (String part : parts) {
                if (!part.endsWith(".")) {
                    continue;
                }
                String name = part.substring(0, part.length() - 1);
                for (EntityPlayer entity : mc.theWorld.playerEntities) {
                    if (!entity.getName().trim().equals(name) || entity == mc.thePlayer) {
                        continue;
                    }
                    strengthPlayers.put(entity, System.currentTimeMillis());
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
        if (!strengthIndicator.isToggled() || !Utils.nullCheck() || strengthPlayers.isEmpty() || Utils.getSkyWarsStatus() != 2) {
            return;
        }
        for (EntityPlayer entityPlayer : strengthPlayers.keySet()) {
            if (AntiBot.isBot(entityPlayer)) {
                continue;
            }
            RenderUtils.renderEntity(entityPlayer, 2, 0, 0, strengthColor, false);
        }
    }
}