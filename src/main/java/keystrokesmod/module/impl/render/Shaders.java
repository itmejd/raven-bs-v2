package keystrokesmod.module.impl.render;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Shaders extends Module {
    private SliderSetting shader;
    private String[] shaderNames;
    private ResourceLocation[] shaderLocations;
    private boolean resetShader;

    public Shaders() {
        super("Shaders", category.render);
        try {
            shaderLocations = (ResourceLocation[]) Reflection.shaderResourceLocations.get(mc.entityRenderer);
            shaderNames = new String[shaderLocations.length];
            for (int i = 0; i < shaderLocations.length; ++i) {
                shaderNames[i] = ((String[]) shaderLocations[i].getResourcePath().replaceFirst("shaders/post/", "").split("\\.json"))[0].toUpperCase();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        this.registerSetting(shader = new SliderSetting("Shader", 0, shaderNames));
    }

    public void onUpdate() {
        if (resetShader) {
            mc.entityRenderer.stopUseShader();
            //Utils.print("Reset shader");
            resetShader = false;
        }
        if (!Utils.nullCheck() || mc.entityRenderer == null || shaderLocations == null) {
            return;
        }
        try {
            if (Reflection.shaderIndex.getInt(mc.entityRenderer) != (int) shader.getInput()) {
                Reflection.shaderIndex.setInt(mc.entityRenderer, (int) shader.getInput());
                Reflection.loadShader.invoke(mc.entityRenderer, shaderLocations[(int) shader.getInput()]);
            }
            else if (!Reflection.useShader.getBoolean(mc.entityRenderer)) {
                Reflection.useShader.setBoolean(mc.entityRenderer, true);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            Utils.sendMessage("&cError loading shader.");
            this.disable();
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (e.getPacket() instanceof S08PacketPlayerPosLook && mc.thePlayer != null) {
            resetShader = true;
        }
    }

    public void onDisable() {
        mc.entityRenderer.stopUseShader();
    }

    public void onEnable() {
        if (!OpenGlHelper.shadersSupported) {
            Utils.sendMessage("&cShaders not supported.");
            this.disable();
        }
    }
}
