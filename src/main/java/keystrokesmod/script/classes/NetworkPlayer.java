package keystrokesmod.script.classes;

import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class NetworkPlayer {
    private NetworkPlayerInfo networkPlayerInfo;
    public NetworkPlayer(NetworkPlayerInfo networkPlayerInfo) {
        this.networkPlayerInfo = networkPlayerInfo;
    }

    public String getCape() {
        return networkPlayerInfo.getLocationCape().getResourcePath();
    }

    public String getDisplayName() {
        if (networkPlayerInfo == null) {
            return "";
        }
        return networkPlayerInfo.getDisplayName() != null ? networkPlayerInfo.getDisplayName().getFormattedText() : ScorePlayerTeam.formatPlayerName(networkPlayerInfo.getPlayerTeam(), networkPlayerInfo.getGameProfile().getName());
    }

    public String getName() {
        if (networkPlayerInfo == null) {
            return "";
        }
        return networkPlayerInfo.getGameProfile().getName();
    }

    public int getPing() {
        if (networkPlayerInfo == null) {
            return 0;
        }
        return networkPlayerInfo.getResponseTime();
    }

    public String getSkinData() {
        final Property texture = (Property) Iterables.getFirst(networkPlayerInfo.getGameProfile().getProperties().get("textures"), (Object)null);
        if (texture == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(texture.getValue().getBytes(StandardCharsets.UTF_8)));
    }

    public String getUUID() {
        if (networkPlayerInfo == null) {
            return "";
        }
        return networkPlayerInfo.getGameProfile().getId().toString();
    }
}
