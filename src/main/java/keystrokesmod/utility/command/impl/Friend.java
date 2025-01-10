package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Friend extends Command {
    public Friend() {
        super("Friend");
    }

    private String ign;

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            ign = args[1];
            if (Utils.isFriended(ign)) {
                Utils.removeFriend(ign);
            }
            else Utils.addFriend(ign);
        }
        else {
            syntaxError();
        }
    }
}
