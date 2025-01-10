package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ModuleComponent extends Component {
    private int originalHoverAlpha = 120;
    private final int c2 = (new Color(154, 2, 255)).getRGB();
    private final int hoverColor = (new Color(0, 0, 0, originalHoverAlpha)).getRGB();
    private final int unsavedColor = new Color(114, 188, 250).getRGB();
    private final int invalidColor = new Color(255, 80, 80).getRGB();
    private final int enabledColor = new Color(24, 154, 255).getRGB();
    private final int disabledColor = new Color(192, 192, 192).getRGB();
    public Module mod;
    public CategoryComponent categoryComponent;
    public int yPos;
    public ArrayList<Component> settings;
    public boolean isOpened;
    private boolean hovering;
    private Timer hoverTimer;
    private boolean hoverStarted;
    private Timer smoothTimer;
    private int smoothingY = 16;

    public ModuleComponent(Module mod, CategoryComponent p, int yPos) {
        this.mod = mod;
        this.categoryComponent = p;
        this.yPos = yPos;
        this.settings = new ArrayList();
        this.isOpened = false;
        int y = yPos + 12;
        if (mod != null && !mod.getSettings().isEmpty()) {
            for (Setting v : mod.getSettings()) {
                if (v instanceof SliderSetting) {
                    SliderSetting n = (SliderSetting) v;
                    SliderComponent s = new SliderComponent(n, this, y);
                    this.settings.add(s);
                    y += 12;
                } else if (v instanceof ButtonSetting) {
                    ButtonSetting b = (ButtonSetting) v;
                    ButtonComponent c = new ButtonComponent(mod, b, this, y);
                    this.settings.add(c);
                    y += 12;
                } else if (v instanceof DescriptionSetting) {
                    DescriptionSetting d = (DescriptionSetting) v;
                    DescriptionComponent m = new DescriptionComponent(d, this, y);
                    this.settings.add(m);
                    y += 12;
                }
            }
        }
        this.settings.add(new BindComponent(this, y));
    }

    public void updateHeight(int newY) {
        this.yPos = newY;
        int y = this.yPos + 16;
        Iterator var3 = this.settings.iterator();

        while (true) {
            while (var3.hasNext()) {
                Component co = (Component) var3.next();
                co.updateHeight(y);
                if (co instanceof SliderComponent) {
                    y += 16;
                } else if (co instanceof ButtonComponent || co instanceof BindComponent || co instanceof DescriptionComponent) {
                    y += 12;
                }
            }

            return;
        }
    }

    public static void e() {
        GL11.glDisable(2929);
        GL11.glEnable(3042);
        GL11.glDisable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glDepthMask(true);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        GL11.glHint(3155, 4354);
    }

    public static void f() {
        GL11.glEnable(3553);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDisable(2848);
        GL11.glHint(3154, 4352);
        GL11.glHint(3155, 4352);
        GL11.glEdgeFlag(true);
    }

    public static void g() {
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static void v(float x, float y, float x1, float y1, int t, int b) {
        e();
        GL11.glShadeModel(7425);
        GL11.glBegin(7);
        g();
        GL11.glVertex2f(x, y1);
        GL11.glVertex2f(x1, y1);
        g();
        GL11.glVertex2f(x1, y);
        GL11.glVertex2f(x, y);
        GL11.glEnd();
        GL11.glShadeModel(7424);
        f();
    }

    public void render() {
        if (hovering || hoverTimer != null) {
            double hoverAlpha = (hovering && hoverTimer != null) ? hoverTimer.getValueFloat(0, originalHoverAlpha, 1) : (hoverTimer != null && !hovering) ? originalHoverAlpha - hoverTimer.getValueFloat(0, originalHoverAlpha, 1) : originalHoverAlpha;
            if (hoverAlpha == 0) {
                hoverTimer = null;
            }
            RenderUtils.drawRoundedRectangle(this.categoryComponent.getX(), this.categoryComponent.getY() + yPos, this.categoryComponent.getX() + this.categoryComponent.getWidth(), this.categoryComponent.getY() + 16 + this.yPos, 8, Utils.mergeAlpha(hoverColor, (int) hoverAlpha));
        }

        v((float) this.categoryComponent.getX(), (float) (this.categoryComponent.getY() + this.yPos), (float) (this.categoryComponent.getX() + this.categoryComponent.getWidth()), (float) (this.categoryComponent.getY() + 15 + this.yPos), this.mod.isEnabled() ? this.c2 : -12829381, this.mod.isEnabled() ? this.c2 : -12302777);
        GL11.glPushMatrix();

        int button_rgb = this.mod.isEnabled() ? enabledColor : disabledColor;
        if (this.mod.script != null && this.mod.script.error) {
            button_rgb = invalidColor;
        }
        if (this.mod.moduleCategory() == Module.category.profiles && !(this.mod instanceof Manager) && !((ProfileModule) this.mod).saved && Raven.currentProfile.getModule() == this.mod) {
            button_rgb = unsavedColor;
        }

        if (smoothTimer != null && System.currentTimeMillis() - smoothTimer.last >= 300) {
            smoothTimer = null;
        }
        if (smoothTimer != null) {
            int height = getModuleHeight();
            if (isOpened) {
                smoothingY = smoothTimer.getValueInt(16, height, 1);
                if (smoothingY == height) {
                    smoothTimer = null;
                }
            }
            else {
                smoothingY = smoothTimer.getValueInt(height, 16, 1);
                if (smoothingY == 16) {
                    smoothTimer = null;
                }
            }
            this.categoryComponent.updateHeight();
        }

        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.mod.getName(), (float) (this.categoryComponent.getX() + this.categoryComponent.getWidth() / 2 - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.mod.getName()) / 2), (float) (this.categoryComponent.getY() + this.yPos + 4), button_rgb);

        GL11.glPopMatrix();
        boolean scissorRequired = smoothTimer != null;
        if (scissorRequired) {
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtils.scissor(this.categoryComponent.getX() - 2, this.categoryComponent.getY() + this.yPos + 4, this.categoryComponent.getWidth() + 4, smoothingY + 4);
        }

        if (this.isOpened || smoothTimer != null) {
            for (Component settingComponent : this.settings) {
                settingComponent.render();
            }
        }

        if (scissorRequired) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glPopMatrix();
        }
    }

    public int getHeight() {
        if (smoothTimer != null) {
            return smoothingY;
        }
        if (!this.isOpened) {
            return 16;
        }
        else {
            int h = 16;
            Iterator var2 = this.settings.iterator();

            while (true) {
                while (var2.hasNext()) {
                    Component c = (Component) var2.next();
                    if (c instanceof SliderComponent) {
                        h += 16;
                    }
                    else if (c instanceof ButtonComponent || c instanceof BindComponent || c instanceof DescriptionComponent) {
                        h += 12;
                    }
                }

                return h;
            }
        }
    }

    public int getModuleHeight() {
        int h = 16;
        Iterator var2 = this.settings.iterator();

        while (true) {
            while (var2.hasNext()) {
                Component c = (Component) var2.next();
                if (c instanceof SliderComponent) {
                    h += 16;
                }
                else if (c instanceof ButtonComponent || c instanceof BindComponent || c instanceof DescriptionComponent) {
                    h += 12;
                }
            }

            return h;
        }
    }

    public void drawScreen(int x, int y) {
        if (!this.settings.isEmpty()) {
            for (Component c : this.settings) {
                c.drawScreen(x, y);
            }
        }
        if (overModuleName(x, y) && this.categoryComponent.opened) {
            hovering = true;
            if (hoverTimer == null) {
                (hoverTimer = new Timer(75)).start();
                hoverStarted = true;
            }
        }
        else {
            if (hovering && hoverStarted) {
                (hoverTimer = new Timer(75)).start();
            }
            hoverStarted = false;
            hovering = false;
        }
    }

    public String getName() {
        return mod.getName();
    }

    public boolean onClick(int x, int y, int mouse) {
        if (this.overModuleName(x, y) && mouse == 0 && this.mod.canBeEnabled()) {
            this.mod.toggle();
            if (this.mod.moduleCategory() != Module.category.profiles) {
                if (Raven.currentProfile != null) {
                    ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                }
            }
        }

        if (this.overModuleName(x, y) && mouse == 1) {
            this.isOpened = !this.isOpened;
            (this.smoothTimer = new Timer(200)).start();
            this.categoryComponent.updateHeight();
            return true;
        }

        for (Component settingComponent : this.settings) {
            settingComponent.onClick(x, y, mouse);
        }
        return false;
    }

    public void mouseReleased(int x, int y, int m) {
        for (Component c : this.settings) {
            c.mouseReleased(x, y, m);
        }

    }

    public void keyTyped(char t, int k) {
        for (Component c : this.settings) {
            c.keyTyped(t, k);
        }
    }

    public void onGuiClosed() {
        for (Component c : this.settings) {
            c.onGuiClosed();
        }
        smoothTimer = null;
        hoverTimer = null;
        smoothingY = getHeight();
    }

    public boolean overModuleName(int x, int y) {
        return x > this.categoryComponent.getX() && x < this.categoryComponent.getX() + this.categoryComponent.getWidth() && y > this.categoryComponent.getModuleY() + this.yPos && y < this.categoryComponent.getModuleY() + 16 + this.yPos;
    }
}
