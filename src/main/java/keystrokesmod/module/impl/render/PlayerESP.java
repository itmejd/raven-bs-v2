package keystrokesmod.module.impl.render;

import keystrokesmod.Raven;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerESP extends Module {
    public SliderSetting red;
    public SliderSetting green;
    public SliderSetting blue;
    public ButtonSetting teamColor;
    public ButtonSetting rainbow;
    private ButtonSetting twoD;
    private ButtonSetting box;
    private ButtonSetting healthBar;
    public ButtonSetting outline;
    private ButtonSetting shaded;
    private ButtonSetting ring;
    public ButtonSetting redOnDamage;
    public ButtonSetting renderSelf;
    public ButtonSetting showInvis;
    private int rgb_c = 0;
    private Map<EntityLivingBase, Integer> renderAsTwoD = new HashMap<>(); // entity with its rgb
    // none, outline, box, shaded, 2d, ring

    public PlayerESP() {
        super("PlayerESP", category.render, 0);
        this.registerSetting(red = new SliderSetting("Red", 0.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(green = new SliderSetting("Green", 255.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(blue = new SliderSetting("Blue", 0.0D, 0.0D, 255.0D, 1.0D));
        this.registerSetting(rainbow = new ButtonSetting("Rainbow", false));
        this.registerSetting(teamColor = new ButtonSetting("Team color", false));
        this.registerSetting(new DescriptionSetting("ESP Types"));
        this.registerSetting(twoD = new ButtonSetting("2D", false));
        this.registerSetting(box = new ButtonSetting("Box", false));
        this.registerSetting(healthBar = new ButtonSetting("Health bar", true));
        this.registerSetting(outline = new ButtonSetting("Outline", false));
        this.registerSetting(ring = new ButtonSetting("Ring", false));
        this.registerSetting(shaded = new ButtonSetting("Shaded", false));
        this.registerSetting(redOnDamage = new ButtonSetting("Red on damage", true));
        this.registerSetting(renderSelf = new ButtonSetting("Render self", false));
        this.registerSetting(showInvis = new ButtonSetting("Show invis", true));
    }

    public void onDisable() {
        RenderUtils.ring_c = false;
    }

    public void guiUpdate() {
        this.rgb_c = (new Color((int) red.getInput(), (int) green.getInput(), (int) blue.getInput())).getRGB();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent e) {
        this.renderAsTwoD.clear();
        if (Utils.nullCheck()) {
            int rgb = rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
            if (Raven.debug) {
                for (final Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                        if (teamColor.isToggled()) {
                            rgb = getColorFromTags(entity);
                        }
                        this.render(entity, rgb);
                        this.renderAsTwoD.put((EntityLivingBase) entity, rgb);
                    }
                }
                return;
            }
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player != mc.thePlayer || (renderSelf.isToggled() && mc.gameSettings.thirdPersonView > 0)) {
                    if (player.deathTime != 0) {
                        continue;
                    }
                    if (!showInvis.isToggled() && player.isInvisible()) {
                        continue;
                    }
                    if (mc.thePlayer != player && AntiBot.isBot(player)) {
                        continue;
                    }
                    if (teamColor.isToggled()) {
                        rgb = getColorFromTags(player);
                    }
                    this.render(player, rgb);
                    this.renderAsTwoD.put(player, rgb);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTwo2D(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !twoD.isToggled()) {
            return;
        }
        for (Map.Entry<EntityLivingBase, Integer> entry : renderAsTwoD.entrySet()) {
            this.renderTwoD(entry.getKey(), entry.getValue(), 0, e.partialTicks);
        }
    }

    private void render(Entity en, int rgb) {
        if (box.isToggled()) {
            RenderUtils.renderEntity(en, 1, 0, 0, rgb, redOnDamage.isToggled());
        }

        if (shaded.isToggled()) {
            if (ModuleManager.murderMystery == null || !ModuleManager.murderMystery.isEnabled() || ModuleManager.murderMystery.isEmpty()) {
                RenderUtils.renderEntity(en, 2, 0, 0, rgb, redOnDamage.isToggled());
            }
        }

        if (healthBar.isToggled()) {
            RenderUtils.renderEntity(en, 4, 0, 0, rgb, redOnDamage.isToggled());
        }

        if (ring.isToggled()) {
            RenderUtils.renderEntity(en, 6, 0, 0, rgb, redOnDamage.isToggled());
        }
    }

    public int getColorFromTags(Entity entity) {
        if (entity instanceof EntityPlayer) {
            ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)((EntityLivingBase) entity).getTeam();
            if (scoreplayerteam != null) {
                String s = FontRenderer.getFormatFromString(scoreplayerteam.getColorPrefix());
                if (s.length() >= 2) {
                    return mc.getRenderManager().getFontRenderer().getColorCode(s.charAt(1));
                }
            }
        }
        String displayName = entity.getDisplayName().getFormattedText();
        displayName = Utils.removeFormatCodes(displayName);
        if (displayName.isEmpty() || !displayName.startsWith("§") || displayName.charAt(1) == 'f') {
            return -1;
        }
        switch (displayName.charAt(1)) {
            case '0':
                return -16777216;
            case '1':
                return -16777046;
            case '2':
                return -16733696;
            case '3':
                return -16733526;
            case '4':
                return -5636096;
            case '5':
                return -5635926;
            case '6':
                return -22016;
            case '7':
                return -5592406;
            case '8':
                return -11184811;
            case '9':
                return -11184641;
            case 'a':
                return -11141291;
            case 'b':
                return -11141121;
            case 'c':
                return -43691;
            case 'd':
                return -43521;
            case 'e':
                return -171;
        }
        return -1;
    }

    public void renderTwoD(EntityLivingBase en, int rgb, double expand, float partialTicks) {
        if (!RenderUtils.isInViewFrustum(en)) {
            return;
        }
        ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().renderPartialTicks, 0);

        ScaledResolution scaledResolution = new ScaledResolution(mc);

        double playerX = en.lastTickPosX + (en.posX - en.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
        double playerY = en.lastTickPosY + (en.posY - en.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
        double playerZ = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bbox = en.getEntityBoundingBox().expand(0.1D + expand, 0.1D + expand, 0.1D + expand);
        AxisAlignedBB axis = new AxisAlignedBB(
                bbox.minX - en.posX + playerX,
                bbox.minY - en.posY + playerY,
                bbox.minZ - en.posZ + playerZ,
                bbox.maxX - en.posX + playerX,
                bbox.maxY - en.posY + playerY,
                bbox.maxZ - en.posZ + playerZ
        );

        Vec3[] corners = new Vec3[8];
        corners[0] = new Vec3(axis.minX, axis.minY, axis.minZ);
        corners[1] = new Vec3(axis.minX, axis.minY, axis.maxZ);
        corners[2] = new Vec3(axis.minX, axis.maxY, axis.minZ);
        corners[3] = new Vec3(axis.minX, axis.maxY, axis.maxZ);
        corners[4] = new Vec3(axis.maxX, axis.minY, axis.minZ);
        corners[5] = new Vec3(axis.maxX, axis.minY, axis.maxZ);
        corners[6] = new Vec3(axis.maxX, axis.maxY, axis.minZ);
        corners[7] = new Vec3(axis.maxX, axis.maxY, axis.maxZ);

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        boolean isInView = false;

        for (Vec3 corner : corners) {
            double x = corner.xCoord;
            double y = corner.yCoord;
            double z = corner.zCoord;

            Vec3 screenVec = RenderUtils.convertTo2D(scaledResolution.getScaleFactor(), x, y, z);
            if (screenVec != null) {
                if (screenVec.zCoord >= 1.0003684 || screenVec.zCoord <= 0) {
                    continue;
                }

                isInView = true;

                double screenX = screenVec.xCoord;
                double screenY = screenVec.yCoord;

                if (screenX < minX) minX = screenX;
                if (screenY < minY) minY = screenY;
                if (screenX > maxX) maxX = screenX;
                if (screenY > maxY) maxY = screenY;
            }
        }

        if (!isInView) {
            return;
        }

        mc.entityRenderer.setupOverlayRendering();

        ScaledResolution res = new ScaledResolution(mc);
        int screenWidth = res.getScaledWidth();
        int screenHeight = res.getScaledHeight();

        minX = Math.max(0, minX);
        minY = Math.max(0, minY);
        maxX = Math.min(screenWidth, maxX);
        maxY = Math.min(screenHeight, maxY);

        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = ( rgb & 0xFF) / 255.0F;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0F);

        // background outline
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX, minY);
        GL11.glVertex2d(maxX, minY);
        GL11.glVertex2d(maxX, maxY);
        GL11.glVertex2d(minX, maxY);
        GL11.glEnd();

        // second background
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX + 1.0, minY + 1.0);
        GL11.glVertex2d(maxX - 1.0, minY + 1.0);
        GL11.glVertex2d(maxX - 1.0, maxY - 1.0);
        GL11.glVertex2d(minX + 1.0, maxY - 1.0);
        GL11.glEnd();

        // main outline
        GL11.glColor4f(red, green, blue, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(minX + 0.5, minY + 0.5);
        GL11.glVertex2d(maxX - 0.5, minY + 0.5);
        GL11.glVertex2d(maxX - 0.5, maxY - 0.5);
        GL11.glVertex2d(minX + 0.5, maxY - 0.5);
        GL11.glEnd();

        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glPopMatrix();
    }
}