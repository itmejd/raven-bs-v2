package keystrokesmod.script.model;

public class Response {
    private int responseCode;
    private String contents;

    public Response(int responseCode, String contents) {
        this.responseCode = responseCode;
        this.contents = contents;
    }

    public int code() {
        return this.responseCode;
    }

    public String string() {
        return this.contents;
    }

    public Json json() {
        return (this.contents == null) ? null : Json.parse(this.contents);
    }

    @Override
    public String toString() {
        return "Response(" + this.responseCode + ")";
    }
}
