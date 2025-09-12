package keystrokesmod.module.impl.combat;

import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityArrow;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlinkHandler;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;

public class LagRange extends Module {

    public SliderSetting latency;
    private SliderSetting activationDist;
    private SliderSetting hurttime;
    private ButtonSetting ignoreTeammates, weaponOnly, forceRelease, releaseOnRod, forwardOnly, flushNearRange;
    public ButtonSetting renderTimer, initialPosition, initialPosition2;

    public ButtonSetting players, jumpPot, arrows;

    private int disableTicks;
    private Vec3 lastPosition;
    private double closest;
    private double startFallHeight;
    public boolean blink;
    private long delay;
    public boolean function, functionJumpPot, functionArrows;
    private boolean disabledJumpPot, disabledArrows;
    private boolean beganJump, beganJumpBlink, goingUp;
    private int jumpPotTimeout, arrowsTimeout, fallTicks;
    private Vec3 arrowPos;
    private boolean nr;
    private boolean nRange;

    public LagRange() {
        super("LagRange", category.combat, 0);

        this.registerSetting(players = new ButtonSetting("Players", true));
        this.registerSetting(latency = new SliderSetting("Latency", "ms", 300, 10, 500, 10));
        this.registerSetting(activationDist = new SliderSetting("Activation Distance", " blocks", 7, 0, 20, 1));
        //this.registerSetting(hurttime = new SliderSetting("Hurttime", 2, 0, 10, 1));
        this.registerSetting(flushNearRange = new ButtonSetting("Flush near range", false));
        //this.registerSetting(releaseOnRod = new ButtonSetting("Release on rod", false));
        this.registerSetting(initialPosition = new ButtonSetting("Show initial position", true));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", false));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        this.registerSetting(forwardOnly = new ButtonSetting("Only while forward", false));
        this.registerSetting(new DescriptionSetting("Other:"));
        this.registerSetting(jumpPot = new ButtonSetting("Jump potion", false));
        this.registerSetting(arrows = new ButtonSetting("Arrows", false));
        this.registerSetting(initialPosition2 = new ButtonSetting("Other initial position", true));
    }

    @Override
    public String getInfo() {
        return (int) latency.getInput() + "ms";
    }

    @Override
    public void onDisable() {
        resetLR();
        resetJumpPot();
        resetArrows();
        disabledJumpPot = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreUpdate(PreUpdateEvent e) {
        disableTicks--;
        double boxSize = activationDist.getInput();

        Vec3 myPosition = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        boolean onGround = mc.thePlayer.onGround;

        disabledJumpPot = disabledArrows = false;

        if (jumpPot.isToggled() && Utils.hasJump() && !ModuleManager.killAura.attacking && jumpPotTimeout <= 26 && ModuleUtils.noJumpTicks <= 2 && fallTicks <= 16) {
            if (!mc.thePlayer.onGround && mc.thePlayer.motionY >= 0.8232) {
                beganJump = true;
            }
            if (beganJumpBlink) {
                jumpPotTimeout++;
                functionJumpPot = true;
            }
            if (beganJump) {
                if (mc.thePlayer.motionY < 0) fallTicks++;
                if (!mc.thePlayer.onGround) {
                    if (mc.thePlayer.motionY > 0.11533923042387646) {
                        goingUp = true;
                        fallTicks = 0;
                    } else if (goingUp) {
                        beganJumpBlink = true;
                        functionJumpPot = false;
                        jumpPotTimeout = 0;
                        goingUp = false;
                    }
                }
            }
        }
        else {
            resetJumpPot();
        }

        if (arrows.isToggled() && arrowsTimeout <= 8 && !ModuleManager.killAura.targeting ) {
            try {
                for (Entity en : mc.theWorld.loadedEntityList) {
                    if (en == null || en == mc.thePlayer) {
                        resetArrows();
                        continue;
                    }
                    if (en instanceof EntityArrow) {
                        if (((IAccessorEntityArrow) en).getInGround()) {
                            resetArrows();
                            continue;
                        }
                        if (mc.thePlayer.getDistanceSqToEntity(en) > 50) {
                            resetArrows();
                            continue;
                        }
                        if (!functionArrows) {
                            arrowPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                        }
                        functionArrows = true;
                        arrowsTimeout++;
                        if (Utils.getDistanceSqToEntity(arrowPos, en) < 4) {
                            resetArrows();
                        }
                    }
                }
            }
            catch (Exception ignored) {}
        }
        else {
            resetArrows();
        }

        if (mc.thePlayer.hurtTime == 9) {
            resetJumpPot();
            resetArrows();
        }

        if (ModuleUtils.isBreaking) {
            disableTicks = 1;
            function = false;
            resetJumpPot();
            resetArrows();
        }

        if (forwardOnly.isToggled() && mc.thePlayer.moveForward < 0.5) {
            disableTicks = 1;
            function = false;
        }

        if (Utils.getHorizontalSpeed() > 0.4) {
            disableTicks = 5;
            function = false;
        }

        if (Utils.holdingFishingRod() && ModuleUtils.rcTick == 1 || ModuleUtils.isPlacing) {
            disableTicks = 1;
            function = false;
            resetJumpPot();
            resetArrows();
        }

        if (Utils.isReplay() || Utils.isLobby()) {
            disableTicks = 1;
            function = false;
            resetJumpPot();
            resetArrows();
        }

        if (mc.thePlayer.motionX == 0.0D && mc.thePlayer.motionY == -0.0784000015258789D && mc.thePlayer.motionZ == 0.0D && !Utils.isMoving()) {
            disableTicks = 1;
            function = false;
        }

        a(boxSize, myPosition);

        if (flushNearRange.isToggled() && nr) {
            if (!nRange) {
                disableTicks = 1;
                function = false;
                nRange = true;
            }
        }
        else {
            nRange = false;
        }

        boolean correctHeldItem = !weaponOnly.isToggled();
        if (!correctHeldItem) {
            boolean holdingWeapon = false;
            holdingWeapon = Utils.holdingWeapon(); // Weapon check
            correctHeldItem = holdingWeapon;
        }

        function = players.isToggled() && correctHeldItem && disableTicks < 0 && closest != -1 && closest < boxSize * boxSize;

        if (lastPosition != null && !onGround && lastPosition.yCoord > myPosition.yCoord && myPosition.yCoord > startFallHeight) {
            startFallHeight = myPosition.yCoord;
        } else if (onGround && myPosition.yCoord < startFallHeight) {
            if (startFallHeight - myPosition.yCoord > 3 && !mc.thePlayer.capabilities.allowFlying) { // Check if you took fall damage
                disableTicks = 5;
                function = false;
            }
            startFallHeight = -Double.MAX_VALUE;
        }
        lastPosition = myPosition;




        if ((function || functionJumpPot || functionArrows) && !(jumpPot.isToggled() && disabledJumpPot || arrows.isToggled() && disabledArrows)) {
            if (functionJumpPot || functionArrows) {
                delay = -1;
            }
            if (delay == -1) {
                delay = Utils.time();
                blink = true;
            }
            if (delay > 0 && (Utils.time() - delay) >= latency.getInput() && ModuleManager.killAura.isBlocked()) {
                delay = -1;
                if (blink) {
                    BlinkHandler.release();
                }
            }
        }
        else {
            if (blink && ModuleManager.killAura.isBlocked()) {
                blink = false;
                BlinkHandler.release();
            }
            delay = -1;
        }
    }

    private void a(double boxSize, Vec3 myPosition) {
        closest = -1;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == null || entity == mc.thePlayer || entity.isDead) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                if (Utils.isFriended((EntityPlayer) entity)) {
                    continue;
                }
                if (((EntityPlayer) entity).deathTime != 0) {
                    continue;
                }
                if (AntiBot.isBot(entity) || (Utils.isTeammate(entity) && ignoreTeammates.isToggled())) {
                    continue;
                }
            }
            else {
                continue;
            }
            float maxRange = (float) activationDist.getInput();
            nr = mc.thePlayer.getDistanceToEntity(entity) <= 4.0F + 4.0F / 3;
            if (mc.thePlayer.getDistanceToEntity(entity) < maxRange + maxRange / 3) { // simple distance check
                Vec3 position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                double distanceSq = position.distanceTo(myPosition);
                if (closest == -1 || distanceSq < closest) {
                    closest = distanceSq;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (e.getPacket() instanceof C0EPacketClickWindow) {
            disableTicks = 1;
            function = false;
            resetJumpPot();
            resetArrows();
        }
        if (e.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity) e.getPacket();
            //int enemyHT = Utils.getHurttime(c02.getEntityFromWorld(mc.theWorld));
            if (Objects.equals(String.valueOf(c02.getAction()), "ATTACK")) {
                if (blink) {
                    BlinkHandler.release();
                }
                /*Vec3 ps = new Vec3(c02.getEntityFromWorld(mc.theWorld).posX, c02.getEntityFromWorld(mc.theWorld).posY, c02.getEntityFromWorld(mc.theWorld).posZ);
                Vec3 p2 = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
                if (checkDistance.isToggled() && ps.distanceToSq(p2)) {

                }*/
            }
        }
    }

    private void resetLR() {
        disableTicks = 0;
        blink = false;
        function = false;
    }

    private void resetJumpPot() {
        if (functionJumpPot) {
            disabledJumpPot = true;
        }
        functionJumpPot = beganJump = beganJumpBlink = goingUp = false;
        jumpPotTimeout = fallTicks = 0;
    }

    private void resetArrows() {
        if (functionArrows) {
            disabledArrows = true;
        }
        functionArrows = false;
        arrowsTimeout = 0;
    }

}