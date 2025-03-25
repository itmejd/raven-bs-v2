package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

import java.util.HashMap;
import java.util.Map;

public class Q extends Command {
    public Q() {
        super("Q");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length > 1) {
            playCommand = hypixelPlayCommands.get(args[1].trim());
            if (playCommand != null) {
                mc.thePlayer.sendChatMessage("/play " + playCommand);
            } else {
                Utils.print("&cQueue failed. Invalid gamemode.");
            }
        }
    }

    Map<String, String> hypixelPlayCommands = new HashMap<>();
    {hypixelPlayCommands.put("p", "bedwars_practice");
        hypixelPlayCommands.put("1", "bedwars_eight_one");
        hypixelPlayCommands.put("2", "bedwars_eight_two");
        hypixelPlayCommands.put("3", "bedwars_four_three");
        hypixelPlayCommands.put("4", "bedwars_four_four");
        hypixelPlayCommands.put("4v4", "bedwars_two_four");
        hypixelPlayCommands.put("2t", "bedwars_eight_two_tourney");
        hypixelPlayCommands.put("2un", "bedwars_eight_two_towerUnderworld");
        hypixelPlayCommands.put("4un", "bedwars_four_four_towerUnderworld");
        hypixelPlayCommands.put("2r", "bedwars_eight_two_rush");
        hypixelPlayCommands.put("4r", "bedwars_four_four_rush");
        hypixelPlayCommands.put("pit", "pit");
        hypixelPlayCommands.put("swsn", "solo_normal");
        hypixelPlayCommands.put("swsi", "solo_insane");
        hypixelPlayCommands.put("swtn", "teams_normal");
        hypixelPlayCommands.put("swti", "teams_insane");
        hypixelPlayCommands.put("bowduel", "duels_bow_duel");
        hypixelPlayCommands.put("classicduel", "duels_classic_duel");
        hypixelPlayCommands.put("opduel", "duels_op_duel");
        hypixelPlayCommands.put("uhcduel", "duels_uhc_duel");
        hypixelPlayCommands.put("bridgeduel", "duels_bridge_duel");
        hypixelPlayCommands.put("uhc", "uhc_solo");
        hypixelPlayCommands.put("uhcteams", "uhc_teams");
        hypixelPlayCommands.put("grinch", "arcade_grinch_simulator_v2");
        hypixelPlayCommands.put("grinchtourney", "arcade_grinch_simulator_v2_tourney");
        hypixelPlayCommands.put("mm", "murder_classic");
        hypixelPlayCommands.put("castle", "bedwars_castle");
        // wool games
        hypixelPlayCommands.put("ww", "wool_wool_wars_two_four");
        hypixelPlayCommands.put("ctw", "wool_capture_the_wool_two_twenty");
    }
    String playCommand = "", gameMode = "";

}
