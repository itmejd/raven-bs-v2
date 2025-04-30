package keystrokesmod.utility.command.impl;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.command.CommandManager;

import java.util.Arrays;

public class StopSpammer extends Command {
    public StopSpammer() { super("Stop Spammer"); }

    @Override
    public void onExecute(String[] args) {
        if (args.length >= 1) {
            ModuleManager.spammer.reset();
            Utils.print("§cPaused spammer");
        }
        else {
            syntaxError();
        }
    }
}
