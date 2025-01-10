package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderComponent extends Component {
    private SliderSetting sliderSetting;
    private ModuleComponent moduleComponent;
    private int o;
    private int x;
    private int y;
    private boolean heldDown = false;
    private double w;

    public SliderComponent(SliderSetting sliderSetting, ModuleComponent moduleComponent, int o) {
        this.sliderSetting = sliderSetting;
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.o = o;
    }

    public void render() {
        RenderUtils.drawRoundedRectangle(this.moduleComponent.categoryComponent.getX() + 4, this.moduleComponent.categoryComponent.getY() + this.o + 11, this.moduleComponent.categoryComponent.getX() + 4 + this.moduleComponent.categoryComponent.getWidth() - 8, this.moduleComponent.categoryComponent.getY() + this.o + 15, 4, -12302777);
        int l = this.moduleComponent.categoryComponent.getX() + 4;
        int r = this.moduleComponent.categoryComponent.getX() + 4 + (int) this.w;
        if (r - l > 84) {
            r = l + 84;
        }

        RenderUtils.drawRoundedRectangle(l, this.moduleComponent.categoryComponent.getY() + this.o + 11, r, this.moduleComponent.categoryComponent.getY() + this.o + 15, 4, Color.getHSBColor((float) (System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F).getRGB());
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        String value;
        double input = this.sliderSetting.getInput();
        String suffix = this.sliderSetting.getSuffix();
        if (input == -1 && this.sliderSetting.canBeDisabled) {
            value = "§cDisabled";
            suffix = "";
        }
        else {
            if (input != 1 && (suffix.equals(" second") || suffix.equals(" block") || suffix.equals(" tick")) && this.moduleComponent.mod.moduleCategory() != Module.category.scripts) {
                suffix += "s";
            }
            if (this.sliderSetting.isString) {
                value = this.sliderSetting.getOptions()[(int) this.sliderSetting.getInput()];
            }
            else {
                value = Utils.isWholeNumber(input) ? (int) input + "" : String.valueOf(input);
            }
        }
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(this.sliderSetting.getName() + ": " + (this.sliderSetting.isString ? "§e" : "§b") +value + suffix, (float) ((int) ((float) (this.moduleComponent.categoryComponent.getX() + 4) * 2.0F)), (float) ((int) ((float) (this.moduleComponent.categoryComponent.getY() + this.o + 3) * 2.0F)), -1);
        GL11.glPopMatrix();
    }

    public void updateHeight(int n) {
        this.o = n;
    }

    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
        double d = Math.min(this.moduleComponent.categoryComponent.getWidth() - 8, Math.max(0, x - this.x));

        if (this.heldDown) {
            this.moduleComponent.mod.onSlide(this.sliderSetting);
            if (d == 0.0D && this.sliderSetting.canBeDisabled) {
                this.sliderSetting.setValueRaw(-1);
            }
            else {
                double n = roundToInterval(d / (double) (this.moduleComponent.categoryComponent.getWidth() - 8) * (this.sliderSetting.getMax() - this.sliderSetting.getMin()) + this.sliderSetting.getMin(), 4);
                this.sliderSetting.setValue(n);
            }

            if (this.sliderSetting.getInput() != this.sliderSetting.getMin() && ModuleManager.hud != null && ModuleManager.hud.isEnabled() && !ModuleManager.organizedModules.isEmpty()) {
                ModuleManager.sort();
            }

            if (Raven.currentProfile != null) {
                ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
            }
        }

        this.w = this.sliderSetting.getInput() == -1 ? 0 : (double) (this.moduleComponent.categoryComponent.getWidth() - 8) * (this.sliderSetting.getInput() - this.sliderSetting.getMin()) / (this.sliderSetting.getMax() - this.sliderSetting.getMin());
    }

    private static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    public boolean onClick(int x, int y, int b) {
        if ((this.u(x, y) || this.i(x, y)) && b == 0 && this.moduleComponent.isOpened) {
            this.heldDown = true;
        }
        return false;
    }

    public void mouseReleased(int x, int y, int m) {
        this.heldDown = false;
    }

    public boolean u(int x, int y) {
        return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() / 2 + 1 && y > this.y && y < this.y + 16;
    }

    public boolean i(int x, int y) {
        return x > this.x + this.moduleComponent.categoryComponent.getWidth() / 2 && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y && y < this.y + 16;
    }

    @Override
    public void onGuiClosed() {
        this.heldDown = false;
    }
}
