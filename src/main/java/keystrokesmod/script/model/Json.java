package keystrokesmod.script.model;

import com.google.gson.*;
import java.util.*;

public class Json {
    public enum Type { OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL }

    private final JsonElement element;
    private final Type type;

    private Json(JsonElement element) {
        this.element = element;
        if (element.isJsonObject()) {
            this.type = Type.OBJECT;
        }
        else if (element.isJsonArray()) {
            this.type = Type.ARRAY;
        }
        else if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                this.type = Type.BOOLEAN;
            }
            else if (prim.isNumber()) {
                this.type = Type.NUMBER;
            }
            else {
                this.type = Type.STRING;
            }
        }
        else if (element.isJsonNull()) {
            this.type = Type.NULL;
        }
        else {
            this.type = Type.NULL;
        }
    }

    public static Json parse(String jsonString) {
        return new Json(new JsonParser().parse(jsonString));
    }

    public static Json object() {
        return new Json(new JsonObject());
    }

    public static Json array() {
        return new Json(new JsonArray());
    }

    public static Json string(String value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json number(Number value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json booleanValue(boolean value) {
        return new Json(new JsonPrimitive(value));
    }

    public static Json nullValue() {
        return new Json(JsonNull.INSTANCE);
    }

    public Type type() {
        return type;
    }

    private void ensureObject() {
        if (type != Type.OBJECT) {
            throw new IllegalStateException("Not a JSON object: " + type);
        }
    }

    public Json add(String key, Json value) {
        ensureObject();
        this.element.getAsJsonObject().add(key, value.element);
        return this;
    }

    public Json add(String key, String  val) {
        return add(key, Json.string(val));
    }

    public Json add(String key, Number  val) {
        return add(key, Json.number(val));
    }

    public Json add(String key, boolean val) {
        return add(key, Json.booleanValue(val));
    }

    public Json get(String key) {
        ensureObject();
        JsonElement child = this.element.getAsJsonObject().get(key);
        return child == null ? Json.nullValue() : new Json(child);
    }

    public boolean has(String key) {
        ensureObject();
        return this.element.getAsJsonObject().has(key);
    }

    private void ensureArray() {
        if (type != Type.ARRAY) {
            throw new IllegalStateException("Not a JSON array: " + type);
        }
    }

    public Json add(Json value) {
        ensureArray();
        this.element.getAsJsonArray().add(value.element);
        return this;
    }

    public Json add(String  val) {
        return add(Json.string(val));
    }

    public Json add(Number  val) {
        return add(Json.number(val));
    }

    public Json add(boolean val) {
        return add(Json.booleanValue(val));
    }

    public List<Json> asArray() {
        ensureArray();
        List<Json> list = new ArrayList<>();
        for (JsonElement el : this.element.getAsJsonArray()) {
            list.add(new Json(el));
        }
        return list;
    }

    private void ensurePrimitive() {
        if (type != Type.STRING && type != Type.NUMBER && type != Type.BOOLEAN) {
            throw new IllegalStateException("Not a primitive: " + type);
        }
    }

    public String asString() {
        ensurePrimitive();
        return element.getAsString();
    }

    public int asInt() {
        ensurePrimitive();
        return element.getAsInt();
    }

    public double asDouble() {
        ensurePrimitive();
        return element.getAsDouble();
    }

    public long asLong() {
        ensurePrimitive();
        return element.getAsLong();
    }

    public float asFloat() {
        ensurePrimitive();
        return element.getAsFloat();
    }

    public boolean asBoolean() {
        ensurePrimitive();
        return element.getAsBoolean();
    }

    public LinkedHashSet<String> keys() {
        ensureObject();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> e : element.getAsJsonObject().entrySet()) {
            out.add(e.getKey());
        }
        return out;
    }

    @Override
    public String toString() {
        return element.toString();
    }
}