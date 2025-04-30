package keystrokesmod.script.model;

import keystrokesmod.utility.Utils;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import java.util.ArrayList;
import java.util.List;

public class Message {
    public ChatComponentText component;

    public Message(String message) {
        this.component = new ChatComponentText(message);
    }

    public void appendStyle(String style, String action, String styleMessage, String message) {
        ChatStyle chatStyle = new ChatStyle();
        if (style.equals("HOVER")) {
            chatStyle.setChatHoverEvent(new HoverEvent(Utils.getEnum(HoverEvent.Action.class, action), new ChatComponentText(styleMessage)));
        }
        else if (style.equals("CLICK")) {
            chatStyle.setChatClickEvent(new ClickEvent(Utils.getEnum(ClickEvent.Action.class, action), styleMessage));
        }
        component.appendSibling(new ChatComponentText(message).setChatStyle(chatStyle));
    }

    public void append(String append) {
        component.appendSibling(new ChatComponentText(append));
    }

    public List<Message> getSiblings() {
        List<Message> siblings = new ArrayList<>();
        for (IChatComponent sibling : this.component.getSiblings()) {
            siblings.add(new Message(sibling.getUnformattedTextForChat()));
        }
        return siblings;
    }

    public String getStyle() {
        return this.component.getChatStyle().toString();
    }

    public String getText() {
        return this.component.getUnformattedTextForChat();
    }

    @Override
    public String toString() {
        return "TextComponent{text='" + this.component.getUnformattedTextForChat() + '\'' + ", siblings=" + this.component.getSiblings() + ", style=" + this.component.getChatStyle() + '}';
    }
}