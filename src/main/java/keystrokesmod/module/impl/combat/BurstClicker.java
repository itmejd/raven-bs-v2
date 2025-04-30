package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class BurstClicker extends Module {
    private SliderSetting clicks;
    private SliderSetting delay;
    private ButtonSetting delayRandomizer;
    private ButtonSetting placeWhenBlock;

    private boolean startClick = false;
    private boolean stopClick = false;

    public BurstClicker() {
        super("BurstClicker", category.combat, 0);
        this.registerSetting(new DescriptionSetting("Artificial dragclicking."));
        this.registerSetting(clicks = new SliderSetting("Clicks", 0.0D, 0.0D, 50.0D, 1.0D));
        this.registerSetting(delay = new SliderSetting("Delay", "ms", 5.0D, 1.0D, 40.0D, 1.0D));
        this.registerSetting(delayRandomizer = new ButtonSetting("Delay randomizer", true));
        this.registerSetting(placeWhenBlock = new ButtonSetting("Place when block", false));
    }

    @Override
    public void onEnable() {
        if (clicks.getInput() != 0.0D && mc.currentScreen == null && mc.inGameHasFocus) {
            Raven.getScheduledExecutor().execute(() -> {
                try {
                    int cl = (int) clicks.getInput();
                    int del = (int) delay.getInput();

                    for (int i = 0; i < cl * 2 && this.isEnabled() && Utils.nullCheck() && mc.currentScreen == null && mc.inGameHasFocus; ++i) {
                        if (i % 2 == 0) {
                            this.startClick = true;
                            if (del != 0) {
                                int realDel = del;
                                if (delayRandomizer.isToggled()) {
                                    realDel = del + Utils.getRandom().nextInt(25) * (Utils.getRandom().nextBoolean() ? -1 : 1);
                                    if (realDel <= 0) {
                                        realDel = del / 3 - realDel;
                                    }
                                }

                                Thread.sleep(realDel);
                            }
                        } else {
                            this.stopClick = true;
                        }
                    }

                    this.disable();
                } catch (InterruptedException var5) {
                }

            });
        }
        else {
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        this.startClick = false;
        this.stopClick = false;
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent e) {
        if (Utils.nullCheck()) {
            if (this.startClick) {
                this.setClicking(true);
                this.startClick = false;
            }
            else if (this.stopClick) {
                this.setClicking(false);
                this.stopClick = false;
            }
        }

    }

    private void setClicking(boolean state) {
        boolean conditionsMet = placeWhenBlock.isToggled() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock;
        if (conditionsMet) {
            ((IAccessorMinecraft) mc).callRightClickMouse();
        }
        else {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(key, state);
            if (state) {
                KeyBinding.onTick(key);
            }
        }
        ReflectionUtils.setButton(conditionsMet ? 1 : 0, state);
    }
}
