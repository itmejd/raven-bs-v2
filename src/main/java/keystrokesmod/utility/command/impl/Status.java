package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Status extends Command {
    public Status() {
        super("status");
    }

    public static String ign, modeString;

    public static int currentMode, lastMode, cooldown, displayNumber;
    public static boolean start;

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            ign = args[1];

            if (cooldown != 0) {
                Utils.print("§dcurrently on cooldown for " + cooldown + "s");
            }
            else {
                ++currentMode;
                getModeString();
                String msg = "/tip " + ign + modeString;
                mc.thePlayer.sendChatMessage(msg);
                lastMode = currentMode;
                displayNumber = lastMode + 1;
                start = true;

                cooldown = 7;
            }
        }
    }

    private void getModeString() {
        if (currentMode > 9) currentMode = 0;
        if (currentMode == 0) {
            modeString = " skywars";
        }
        else if (currentMode == 1) {
            modeString = " tnt";
        }
        else if (currentMode == 2) {
            modeString = " classic";
        }
        else if (currentMode == 3) {
            modeString = " blitz";
        }
        else if (currentMode == 4) {
            modeString = " mega";
        }
        else if (currentMode == 5) {
            modeString = " uhc";
        }
        else if (currentMode == 6) {
            modeString = " arcade";
        }
        else if (currentMode == 7) {
            modeString = " warlords";
        }
        else if (currentMode == 8) {
            modeString = " smash";
        }
        else if (currentMode == 9) {
            modeString = " cops";
        }
    }


}
