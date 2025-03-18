package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class Criticals extends Module {

    private SliderSetting mode;
    private String[] modes = new String[] { "Packet", "Offset" };

    private int ticks0, ticks1, timeout;
    private boolean canOffset;

    public Criticals() {
        super("Criticals", category.combat);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof C02PacketUseEntity) {
            canOffset = true;
            timeout = 8;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {

        if (timeout > 0) {
            --timeout;
        }
        if (NoSlow.noSlowing) {
            timeout = 0;
        }


        if (mode.getInput() == 0 && timeout > 0) {
            ++ticks0;
            if (ticks0 == 1) {
                e.setOnGround(false);
                if (Raven.debug) {
                    Utils.sendModuleMessage(this, "ground = false");
                }
            }
            else if (ticks0 >= 2) {
                ticks0 = 0;
            }
        }
        else {
            ticks0 = 0;
        }

        if (mode.getInput() == 1 && timeout > 0 && !mc.thePlayer.onGround) {
            ++ticks1;
            if (ticks1 == 1) {
                e.setPosY(e.getPosY() - 0.0001);
                if (Raven.debug) {
                    Utils.sendModuleMessage(this, "-offset");
                }
            }
            else if (ticks1 >= 2) {
                e.setPosY(e.getPosY() + 0.0001);
                ticks1 = 0;
                if (Raven.debug) {
                    Utils.sendModuleMessage(this, "+offset");
                }
            }
        }
        else {
            ticks1 = 0;
        }

    }
}