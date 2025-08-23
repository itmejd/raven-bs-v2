package keystrokesmod.utility.command.impl;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

import java.util.Arrays;

public class SpammerUtil extends Command {
    public SpammerUtil() { super("Spammer"); }

    @Override
    public void onExecute(String[] args) {
        if (args.length >= 2) {

            String message = Arrays.toString(args);
            String[] spl = message.split("spammer, ");

            ModuleManager.spammer.message = spl[1];

            Utils.modulePrint("§dSet spammer message as: " + ModuleManager.spammer.message);
        }
        else {
            syntaxError();
        }
    }
}
