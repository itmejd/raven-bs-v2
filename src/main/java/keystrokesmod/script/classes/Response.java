package keystrokesmod.script.classes;

import com.google.gson.JsonParser;
import keystrokesmod.utility.NetworkUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

public class Response {
    private HttpURLConnection connection;

    protected Response(HttpURLConnection connection) {
        this.connection = connection;
    }

    public int code() {
        try {
            return this.connection.getResponseCode();
        }
        catch (IOException e) {
            return 0;
        }
    }

    public String string() {
        return NetworkUtils.getTextFromConnection(this.connection, false);
    }

    public Json json() {
        return new Json((new JsonParser()).parse(NetworkUtils.getTextFromConnection(this.connection, false)).getAsJsonObject(), (byte) 0);
    }

    @Override
    public String toString() {
        return "Response(" + this.code() + ")";
    }
}
