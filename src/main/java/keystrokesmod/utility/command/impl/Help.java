package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class Help extends Command {
    public Help() {
        super("help");
    }

    @Override
    public void onExecute(String[] args) {
        chat("&7[&fhelp&7] Chat commands - &dGeneral");
        chat(" &b.(ign/name) &7Copy your username.");
        chat(" &b.ping &7Estimate your ping.");
        chat(" &b.q [gamemode] &7Queue a mode");
        chat(" &b.qlist &7List queueable modes");
        chat("&7---------------------------");
        chat("&7[&fhelp&7] Chat commands - &dModules");
        chat(" &b.cname [name] &7Set name hider name.");
        chat(" &b.binds (key) &7List module binds.");
        chat(" &b.friend [name] &7Add/Remove somebody as a friend");
        chat(" &b.enemy [name] &7Add/Remove somebody as a enemy");
    }
}
