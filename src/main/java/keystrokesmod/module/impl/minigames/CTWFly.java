package keystrokesmod.module.impl.minigames;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.CommandManager;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.*;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Objects;

public class CTWFly extends Module {
    public static SliderSetting horizontalSpeed;
    private SliderSetting verticalSpeed;
    private SliderSetting maxFlyTicks;
    private boolean d;
    private boolean a = false;

    private boolean begin, placed;
    private int flyTicks, ticks321;

    private int percent, percentDisplay;

    private static int widthOffset = 55;
    private static String get321 = "Waiting for explosion...";

    private int color = new Color(0, 187, 255, 255).getRGB();

    public CTWFly() {
        super("CTW Fly", category.minigames);
        this.registerSetting(new DescriptionSetting("Use TNT to fly"));
        this.registerSetting(new DescriptionSetting("(High speed values will dog)"));
        this.registerSetting(horizontalSpeed = new SliderSetting("Horizontal speed", 4.0, 1.0, 9.0, 0.1));
        this.registerSetting(verticalSpeed = new SliderSetting("Vertical speed", 2.0, 1.0, 9.0, 0.1));
        this.registerSetting(maxFlyTicks = new SliderSetting("Max fly ticks", 40, 1, 80, 1));

    }

    public void onDisable() {
        if (begin || placed) {
            disabled();
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();

        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect s29 = (S29PacketSoundEffect) packet;
            if (!Objects.equals(String.valueOf(s29.getSoundName()), "random.explode")) {
               return;
            }
            if (mc.thePlayer.getPosition().distanceSq(s29.getX(), s29.getY(), s29.getZ()) <= 30) {
                begin = true;
                placed = false;
                flyTicks = 0;
            }
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingTNT()) {
            placed = true;
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        String stripped = Utils.stripColor(e.message.getUnformattedText());

        if (stripped.contains("You cannot place blocks here!") && placed) {
            disabled();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) // called last in order to apply fix
    public void onMoveInput(PrePlayerInputEvent e) {
        if (!placed) {
            return;
        }
        e.setForward(0);
        e.setStrafe(0);
        Utils.setSpeed(0);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (placed) {
            if (ticks321 >= 45) {
                ticks321 = 0;
            }
            ++ticks321;
        }
        if (!begin) {
            return;
        }
        this.d = mc.thePlayer.capabilities.isFlying;
        if (++flyTicks >= maxFlyTicks.getInput() + 1) {
            disabled();
        }

        double percentCalc = 1000 / maxFlyTicks.getInput();
        if (percent < 1000) percent = percent + (int) percentCalc;
        percentDisplay = percent / 10;
        if (flyTicks >= maxFlyTicks.getInput()) percentDisplay = 100;


    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck() || !begin && !placed) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        color = Theme.getGradient((int) HUD.theme.getInput(), 0);
        if (!placed) {
            widthOffset = percentDisplay < 10 ? 8 : percentDisplay < 100 ? 12 : 14;
        } else {
            switch (ticks321) {
                case 15:
                    get321 = "Waiting for explosion.";
                    widthOffset = 51;
                    break;
                case 30:
                    get321 = "Waiting for explosion..";
                    widthOffset = 53;
                    break;
                case 45:
                    get321 = "Waiting for explosion...";
                    widthOffset = 55;
                    break;
            }
        }
        String text = placed ? get321 : (percentDisplay + "%");
        int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        int[] display = {scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, color, true);
    }

    public void onUpdate() {
        if (!begin) {
            return;
        }
        if (mc.currentScreen == null) {
            if (Utils.jumpDown()) {
                mc.thePlayer.motionY = 0.3 * verticalSpeed.getInput();
            }
            else if (Utils.sneakDown()) {
                mc.thePlayer.motionY = -0.3 * verticalSpeed.getInput();
            }
            else {
                mc.thePlayer.motionY = 0.0;
            }
        }
        else {
            mc.thePlayer.motionY = 0.0;
        }
        mc.thePlayer.capabilities.setFlySpeed(0.2f);
        mc.thePlayer.capabilities.isFlying = true;
        setSpeed(0.85 * horizontalSpeed.getInput());
    }



    public static void setSpeed(final double n) {
        if (n == 0.0) {
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionX = 0;
            return;
        }
        double n3 = mc.thePlayer.movementInput.moveForward;
        double n4 = mc.thePlayer.movementInput.moveStrafe;
        float rotationYaw = mc.thePlayer.rotationYaw;
        if (n3 == 0.0 && n4 == 0.0) {
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionX = 0;
        }
        else {
            if (n3 != 0.0) {
                if (n4 > 0.0) {
                    rotationYaw += ((n3 > 0.0) ? -45 : 45);
                }
                else if (n4 < 0.0) {
                    rotationYaw += ((n3 > 0.0) ? 45 : -45);
                }
                n4 = 0.0;
                if (n3 > 0.0) {
                    n3 = 1.0;
                }
                else if (n3 < 0.0) {
                    n3 = -1.0;
                }
            }
            final double radians = Math.toRadians(rotationYaw + 90.0f);
            final double sin = Math.sin(radians);
            final double cos = Math.cos(radians);
            mc.thePlayer.motionX = n3 * n * cos + n4 * n * sin;
            mc.thePlayer.motionZ = n3 * n * sin - n4 * n * cos;
        }
    }

    private void disabled() {
        begin = placed = false;
        if (mc.thePlayer.capabilities.allowFlying) {
            mc.thePlayer.capabilities.isFlying = this.d;
        }
        else {
            mc.thePlayer.capabilities.isFlying = false;
        }
        this.d = false;
        if (flyTicks > 0) {
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionZ = 0;
        }
        flyTicks = percent = ticks321 = 0;

        get321 = "Waiting for explosion...";
        widthOffset = 55;
    }
}
