package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class QList extends Command {
    public QList() {
        super("QList");
    }

    @Override
    public void onExecute(String[] args) {
            chat(" &7-------------------------------------&r");
            chat(" &7Q List:");
            chat(" &7 ");
            chat(" &7p - Bedwars Practice");
            chat(" &71 - Bedwars Solos");
            chat(" &72 - Bedwars Doubles");
            chat(" &73 - Bedwars Threes");
            chat(" &74 - Bedwars Fours");
            chat(" &74v4 - Bedwars 4v4");
            chat(" &72t - Bedwars Doubles Tourney");
            chat(" &72un - Bedwars Doubles Tower Underworld");
            chat(" &74un - Bedwars Fours Tower Underworld");
            chat(" &72r - Bedwars Doubles Rush");
            chat(" &74r - Bedwars Fours Rush");
            chat(" &7pit - The Pit");
            chat(" &7swsn - Skywars Solo Normal");
            chat(" &7swsi - Skywars Solo Insane");
            chat(" &7swtn - Skywars Teams Normal");
            chat(" &7swti - Skywars Teams Insane");
            chat(" &7bowduel - Bow Duel");
            chat(" &7classicduel - Classic Duel");
            chat(" &7opduel - OP Duel");
            chat(" &7uhcduel - UHC Duel");
            chat(" &7bridgeduel - Bridge Duel");
            chat(" &7uhc - UHC Solos");
            chat(" &7uhcteams - UHC Teams");
            chat(" &7grinch - Grinch Simulator");
            chat(" &7grinchtourney - Grinch Simulator Tournament");
            chat(" &7mm - Murder Mystery Classic");
            chat(" &7castle - Bedwars Castle");
            chat(" &7ww - Wool Wars");
            chat(" &7ctw - Capture The Wool");
            chat(" &7-------------------------------------");
    }
}
