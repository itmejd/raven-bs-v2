package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ClientLookEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AimAssist extends Module {

    private SliderSetting mode;
    private SliderSetting speed;
    private SliderSetting fov;
    private SliderSetting distance;

    private ButtonSetting clickAim;
    private ButtonSetting disableWhileMining;
    private ButtonSetting weaponOnly;
    private ButtonSetting aimInvis;
    private ButtonSetting blatantMode;
    private ButtonSetting ignoreTeammates;

    private String[] aimModes = new String[] { "Normal", "Silent" };

    private Float[] lookingAt = null;

    public AimAssist() {
        super("AimAssist", category.combat, 0);
        this.registerSetting(mode = new SliderSetting("Mode", 0, aimModes));
        this.registerSetting(speed = new SliderSetting("Speed", 45.0D, 1.0D, 100.0D, 1.0D));
        this.registerSetting(fov = new SliderSetting("FOV", 90.0D, 15.0D, 360.0D, 1.0D));
        this.registerSetting(distance = new SliderSetting("Distance", 4.5D, 1.0D, 10.0D, 0.5D));
        this.registerSetting(clickAim = new ButtonSetting("Click aim", true));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(disableWhileMining = new ButtonSetting("Disable while mining", false));
        this.registerSetting(aimInvis = new ButtonSetting("Aim invis", false));
        this.registerSetting(blatantMode = new ButtonSetting("Blatant mode", false));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", false));
    }

    @Override
    public String getInfo() {
        String info;
        info = aimModes[(int) mode.getInput()];
        return info;
    }


    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        this.lookingAt = null;
        if (mc.currentScreen != null || !mc.inGameHasFocus) {
            return;
        }
        if (weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            return;
        }
        if (clickAim.isToggled() && !Utils.isClicking()) {
            return;
        }
        if (disableWhileMining.isToggled() && Utils.isMining()) {
            return;
        }

        Entity en = this.getEnemy();

        if (en == null) {
            return;
        }

        if (blatantMode.isToggled()) {
            float[] rotations = RotationUtils.getRotations(en);
            if (rotations != null) {
                float yaw = rotations[0];
                float pitch = MathHelper.clamp_float(rotations[1] + 4.0F, -90, 90);
                RotationHelper.get().setRotations(yaw, pitch);
                lookingAt = new Float[] { yaw, pitch };
            }
        }
        else {
            double diff = Utils.aimDifference(en, this.mode.getInput() == 1);
            float val = (float) ( -( diff / (101.0D - speed.getInput()) ) ) * 1.2F;
            float yaw = RotationUtils.serverRotations[0] + val;
            RotationHelper.get().setYaw(yaw);
            lookingAt = new Float[] { yaw };
        }
    }

    @SubscribeEvent
    public void onClientLook(ClientLookEvent e) {
        if (this.lookingAt == null) {
            return;
        }
        if (this.lookingAt.length == 2) {
            if (this.lookingAt[1] != null) {
                e.pitch = this.lookingAt[1];
            }
        }
        if (this.lookingAt[0] != null) {
            e.yaw = this.lookingAt[0];
        }
    }

    private Entity getEnemy() {
        final int n = (int)fov.getInput();
        for (final EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
            if (entityPlayer != mc.thePlayer && entityPlayer.deathTime == 0) {
                if (Utils.isFriended(entityPlayer)) {
                    continue;
                }
                if (ignoreTeammates.isToggled() && Utils.isTeammate(entityPlayer)) {
                    continue;
                }
                if (!aimInvis.isToggled() && entityPlayer.isInvisible()) {
                    continue;
                }
                if (mc.thePlayer.getDistanceToEntity(entityPlayer) > distance.getInput()) {
                    continue;
                }
                if (AntiBot.isBot(entityPlayer)) {
                    continue;
                }
                if (!blatantMode.isToggled() && n != 360 && !Utils.inFov((float)n, entityPlayer)) {
                    continue;
                }
                return entityPlayer;
            }
        }
        return null;
    }

}