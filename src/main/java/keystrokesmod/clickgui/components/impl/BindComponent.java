package keystrokesmod.clickgui.components.impl;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComponent extends Component {
    public boolean isBinding;
    public ModuleComponent moduleComponent;
    public int o;
    private int x;
    private int y;
    public KeySetting keySetting;

    public BindComponent(ModuleComponent moduleComponent, int o) {
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.o = o;
    }

    public BindComponent(ModuleComponent moduleComponent, KeySetting keySetting, int o) {
        this.moduleComponent = moduleComponent;
        this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
        this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
        this.keySetting = keySetting;
        this.o = o;
    }

    public void updateHeight(int n) {
        this.o = n;
    }

    public void render() {
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        if (keySetting == null) {
            this.drawString(!this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.script == null ? "Module cannot be bound." : this.isBinding ? "Press a key..." : "Current bind: '§e" + (this.moduleComponent.mod.getKeycode() >= 1000 ? "M" + (this.moduleComponent.mod.getKeycode() - 1000) : Keyboard.getKeyName(this.moduleComponent.mod.getKeycode())) + "§r'");
        }
        else {
            this.drawString(this.isBinding ? "Press a key..." : this.keySetting.getName() + ": '§e" + (this.keySetting.getKey() >= 1000 ? "M" + (this.keySetting.getKey() - 1000) : Keyboard.getKeyName(this.keySetting.getKey())) + "§r'");
        }
        GL11.glPopMatrix();
    }

    public void drawScreen(int x, int y) {
        this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
        this.x = this.moduleComponent.categoryComponent.getX();
    }

    public boolean onClick(int x, int y, int button) {
        if (this.i(x, y) && this.moduleComponent.isOpened && this.moduleComponent.mod.canBeEnabled()) {
            if (button == 0) {
                this.isBinding = !this.isBinding;
            }
            else if (button == 1 && this.moduleComponent.mod.moduleCategory() != Module.category.profiles) {
                this.moduleComponent.mod.setHidden(!this.moduleComponent.mod.isHidden());
                if (Raven.currentProfile != null) {
                    ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                }
            }
            else if (button > 1) {
                if (this.isBinding) {
                    if (this.keySetting != null) {
                        this.keySetting.setKey(button + 1000);
                    }
                    else {
                        this.moduleComponent.mod.setBind(button + 1000);
                    }
                    if (Raven.currentProfile != null) {
                        ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                    }
                    this.isBinding = false;
                }
            }
        }
        return false;
    }

    public void keyTyped(char t, int keybind) {
        if (this.isBinding) {
            if (keybind == Keyboard.KEY_0 || keybind == Keyboard.KEY_ESCAPE) {
                if (this.moduleComponent.mod instanceof Gui) {
                    this.moduleComponent.mod.setBind(54);
                }
                else {
                    if (this.keySetting != null) {
                        this.keySetting.setKey(0);
                    }
                    else {
                        this.moduleComponent.mod.setBind(0);
                    }
                }
                if (Raven.currentProfile != null) {
                    ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                }
            }
            else {
                if (Raven.currentProfile != null) {
                    ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                }
                if (this.keySetting != null) {
                    this.keySetting.setKey(keybind);
                }
                else {
                    this.moduleComponent.mod.setBind(keybind);
                }
            }

            this.isBinding = false;
        }
    }

    public boolean i(int x, int y) {
        return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y - 1 && y < this.y + 12;
    }

    public int getHeight() {
        if (this.keySetting != null) {
            return 0;
        }
        return 16;
    }

    private void drawString(String s) {
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(s, (float) ((this.moduleComponent.categoryComponent.getX() + 4) * 2), (float) ((this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3 : 4)) * 2), !this.moduleComponent.mod.hidden ? Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0) : Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0));
    }

    public void onGuiClosed() {
        this.isBinding = false;
    }
}