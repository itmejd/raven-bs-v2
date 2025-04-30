package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class Help extends Command {
    public Help() {
        super("help");
    }

    @Override
    public void onExecute(String[] args) {
        chatWithPrefix("&7Chat commands - &dGeneral");
        chat(" &b.ign/name &7Copy your username.");
        chat(" &b.ping &7Estimate your ping.");
        chat(" &b.friend/enemy [name/clear] &7Adds as enemy/friend.");
        chat(" &b.q [mode] &7Queues a gamemode.");
        chat(" &b.qlist &7Shows the modes you can queue with the \".q\" command");
        chat(" &b.status [player] &7Checks if a player is online or not. (Bypasses filters)");
        chatWithPrefix("&7Chat commands - &dModules");
        chat(" &b.cname [name] &7Set name hider name.");
        chat(" &b.binds (key) &7List module binds.");
        chat(" &b.spammer <message> &7Set spammer message");
        chat(" &b.stop spammer &7Stops spammer");
        chatWithPrefix("&7Chat commands - &dProfiles");
        chat(" &b.profiles &7List loaded profiles.");
        chat(" &b.profiles save (name) &7Save current settings as a profile.");
        chat(" &b.profiles load [name] &7Load a profile.");
        chat(" &b.profiles delete [name] &7Delete a profile.");
        chat(" &b.profiles rename [oldname] [newname] &7Rename a profile.");
    }
}
