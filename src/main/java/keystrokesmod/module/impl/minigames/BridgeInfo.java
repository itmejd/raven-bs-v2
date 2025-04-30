package keystrokesmod.module.impl.minigames;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

import java.awt.*;
import java.io.IOException;
import java.util.Iterator;

public class BridgeInfo extends Module {
    private final int textRGB = new Color(0, 200, 200).getRGB();
    // message types
    private final String bridge = "the brid";
    private final String start = "Defend!";
    private final String start2 = "Jump in to score!";
    private final String qt = "First player to score 5 goals wins";
    private final String enemyText = "Enemy: ";
    private final String distance = "Distance to goal: ";
    private final String enemyDistance = "Enemy distance to goal: ";
    private final String blocks = "Blocks: ";

    private static int hudX = 5;
    private static int hudY = 70;

    private String enemyName = "";
    private BlockPos g1p = null;
    private BlockPos g2p = null;

    private boolean q = false;
    private double d1 = 0.0D;
    private double d2 = 0.0D;
    private int blc = 0;

    public BridgeInfo() {
        super("Bridge Info", category.minigames, 0);
        this.registerSetting(new DescriptionSetting(new String("Only for solos.")));
        this.registerSetting(new ButtonSetting("Edit position", () -> {
            mc.displayGuiScreen(new EditScreen());
        }));
    }

    @Override
    public void onDisable() {
        this.reset();
    }

    public void onUpdate() {
        if (!this.enemyName.isEmpty() && this.isBridge()) {
            EntityPlayer enem = null;
            Iterator var2 = mc.theWorld.loadedEntityList.iterator();

            while (var2.hasNext()) {
                Entity e = (Entity) var2.next();
                if (e instanceof EntityPlayer) {
                    if (e.getName().equals(this.enemyName)) {
                        enem = (EntityPlayer) e;
                    }
                } else if (e instanceof EntityArmorStand) {
                    if (e.getName().contains(this.start)) {
                        this.g1p = e.getPosition();
                    }
                    else if (e.getName().contains(this.start2)) {
                        this.g2p = e.getPosition();
                    }
                }
            }

            if (this.g1p != null && this.g2p != null) {
                this.d1 = Utils.round(mc.thePlayer.getDistance((double) this.g2p.getX(), (double) this.g2p.getY(), (double) this.g2p.getZ()) - 1.4D, 1);
                if (this.d1 < 0.0D) {
                    this.d1 = 0.0D;
                }

                this.d2 = enem == null ? 0.0D : Utils.round(enem.getDistance((double) this.g1p.getX(), (double) this.g1p.getY(), (double) this.g1p.getZ()) - 1.4D, 1);
                if (this.d2 < 0.0D) {
                    this.d2 = 0.0D;
                }
            }

            int blc2 = 0;

            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).block.equals(Blocks.stained_hardened_clay)) {
                    blc2 += stack.stackSize;
                }
            }

            this.blc = blc2;
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (ev.phase == Phase.END && Utils.nullCheck() && this.isBridge()) {
            if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) {
                return;
            }

            mc.fontRendererObj.drawString(this.enemyText + this.enemyName, (float) hudX, (float) hudY, textRGB, true);
            mc.fontRendererObj.drawString(this.distance + this.d1, (float) hudX, (float) (hudY + 11), textRGB, true);
            mc.fontRendererObj.drawString(this.enemyDistance + this.d2, (float) hudX, (float) (hudY + 22), textRGB, true);
            mc.fontRendererObj.drawString(this.blocks + this.blc, (float) hudX, (float) (hudY + 33), textRGB, true);
        }

    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent c) {
        if (Utils.nullCheck()) {
            String s = Utils.stripColor(c.message.getUnformattedText());
            if (s.startsWith(" ")) {
                if (s.contains(this.qt)) {
                    this.q = true;
                }
                else if (this.q && s.contains("Opponent:")) {
                    String n = s.split(":")[1].trim();
                    if (n.contains("[")) {
                        n = n.split("] ")[1];
                    }
                    this.enemyName = n;
                    this.q = false;
                }
            }
        }

    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            this.reset();
        }
    }

    private boolean isBridge() {
        if (Utils.isHypixel()) {
            Iterator var1 = Utils.getScoreBoardOld().iterator();

            while (var1.hasNext()) {
                String s = (String) var1.next();
                String s2 = s.toLowerCase();
                if (s2.contains("mode") && s2.contains(bridge)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void reset() {
        this.enemyName = "";
        this.q = false;
        this.g1p = null;
        this.g2p = null;
        this.d1 = 0.0D;
        this.d2 = 0.0D;
        this.blc = 0;
    }

    private class EditScreen extends GuiScreen {
        String example = new String("Enemy: Player123-Distance to goal: 17.2-Enemy distance to goal: 16.3-Blocks: 98");
        GuiButtonExt resetPosition;
        boolean d = false;
        int miX = 0;
        int miY = 0;
        int maX = 0;
        int maY = 0;
        int aX = 5;
        int aY = 70;
        int laX = 0;
        int laY = 0;
        int lmX = 0;
        int lmY = 0;

        public void initGui() {
            super.initGui();
            this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, 5, 85, 20, new String("Reset position")));
            this.aX = BridgeInfo.hudX;
            this.aY = BridgeInfo.hudY;
        }

        public void drawScreen(int mX, int mY, float pt) {
            drawRect(0, 0, this.width, this.height, -1308622848);
            int miX = this.aX;
            int miY = this.aY;
            int maX = miX + 140;
            int maY = miY + 41;
            this.d(this.mc.fontRendererObj, this.example);
            this.miX = miX;
            this.miY = miY;
            this.maX = maX;
            this.maY = maY;
            BridgeInfo.hudX = miX;
            BridgeInfo.hudY = miY;
            ScaledResolution res = new ScaledResolution(this.mc);
            int x = res.getScaledWidth() / 2 - 84;
            int y = res.getScaledHeight() / 2 - 20;
            RenderUtils.drawColoredString("Edit the HUD position by dragging.", '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

            try {
                this.handleInput();
            } catch (IOException var12) {
            }

            super.drawScreen(mX, mY, pt);
        }

        private void d(FontRenderer fr, String t) {
            int x = this.miX;
            int y = this.miY;
            String[] var5 = t.split("-");
            int var6 = var5.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                String s = var5[var7];
                fr.drawString(s, (float) x, (float) y, textRGB, true);
                y += fr.FONT_HEIGHT + 2;
            }

        }

        protected void mouseClickMove(int mX, int mY, int b, long t) {
            super.mouseClickMove(mX, mY, b, t);
            if (b == 0) {
                if (this.d) {
                    this.aX = this.laX + (mX - this.lmX);
                    this.aY = this.laY + (mY - this.lmY);
                } else if (mX > this.miX && mX < this.maX && mY > this.miY && mY < this.maY) {
                    this.d = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = this.aX;
                    this.laY = this.aY;
                }

            }
        }

        protected void mouseReleased(int mX, int mY, int s) {
            super.mouseReleased(mX, mY, s);
            if (s == 0) {
                this.d = false;
            }

        }

        public void actionPerformed(GuiButton b) {
            if (b == this.resetPosition) {
                this.aX = BridgeInfo.hudX = 5;
                this.aY = BridgeInfo.hudY = 70;
            }

        }

        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
