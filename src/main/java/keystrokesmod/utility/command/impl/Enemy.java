package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Enemy extends Command {
    public Enemy() {
        super("Enemy");
    }

    private String ign;

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            ign = args[1];
            if (Utils.isEnemy(ign)) {
                Utils.removeEnemy(ign);
            }
            else Utils.addEnemy(ign);
        }
        else {
            syntaxError();
        }
    }
}
