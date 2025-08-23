package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.script.model.Block;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.ItemStack;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class AutoBlockIn extends Module {

    Map<String,Integer> BLOCK_SCORE = new HashMap<>();
    {
        BLOCK_SCORE.put("obsidian", 0);
        BLOCK_SCORE.put("end_stone", 1);
        BLOCK_SCORE.put("planks", 2);
        BLOCK_SCORE.put("log", 2);
        BLOCK_SCORE.put("glass", 3);
        BLOCK_SCORE.put("stained_glass", 3);
        BLOCK_SCORE.put("hardened_clay", 4);
        BLOCK_SCORE.put("stained_hardened_clay", 4);
        BLOCK_SCORE.put("wool", 5);
    }

    Set<String> placeThrough = new HashSet<>(Arrays.asList(
            "air",
            "water",
            "lava",
            "fire"
    ));

    private float serverYaw, serverPitch;
    private double filled;
    private Vec3 placePos, hitPos;
    private String face = "";
    private boolean pendingPlace;
    private boolean placingActive;
    private boolean skipTick;
    public int origSlot = -1;
    private int plannedPlaceSlot = -1;
    private int leftUnpressed;
    private int rightUnpressed;
    private boolean swapped;

    public boolean active;

    private SliderSetting range;
    private SliderSetting speed;
    private SliderSetting tolerance;
    private KeySetting keybind;
    public ButtonSetting spoofItem;
    public ButtonSetting highlightBlocks;
    public Map<Vec3, Timer> highlight = new HashMap<>();
    public boolean canBlockFade;

    public AutoBlockIn() {
        super("AutoBlockIn", category.player);

        this.registerSetting(range = new SliderSetting("Range", " blocks", 4.5, 0.5, 4.5, 0.1));
        this.registerSetting(speed = new SliderSetting("Speed", 8, 0, 100, 1));
        this.registerSetting(tolerance = new SliderSetting("Rotation Tolerance", "\u00B0", 25, 20, 100, 1));
        this.registerSetting(spoofItem = new ButtonSetting("Spoof item", false));
        //this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", false));
        this.registerSetting(keybind = new KeySetting("Keybind", 0));
    }

    @Override
    public void onEnable() {
        serverYaw = mc.thePlayer.rotationYaw;
        serverPitch = mc.thePlayer.rotationPitch;
    }

    @Override
    public void onDisable() {
        disablePlacing(true);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                return;
            }
        }
        if (filled <= 0) return;

        int[] size = ScriptDefaults.client.getDisplaySize();
        int cenX = size[0] / 2;
        int cenY = size[1] / 2;

        int percent = (int) ((filled / 9) * 100);
        String msg = percent + "%";
        int w = ScriptDefaults.render.getFontWidth(msg);

        int x = cenX - (w / 2);
        int y = cenY + ScriptDefaults.render.getFontHeight() + 1;

        int color = percent == 100 ? 0x00FF00 : 0xFFFFFF;

        ScriptDefaults.render.text2d(msg, x, y, 1f, color, true);
    }

    @SubscribeEvent
    public void onPacketSend(SendPacketEvent e) {
        if (e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook || e.getPacket() instanceof C03PacketPlayer.C06PacketPlayerPosLook) {
            C03PacketPlayer c03 = (C03PacketPlayer) e.getPacket();
            serverYaw = c03.getYaw();
            serverPitch = c03.getPitch();
        }
    }

    public void onMouse(MouseEvent e) {
        if (placingActive && e.button > -1) {
            e.setCanceled(true);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientRotation(ClientRotationEvent e) {
        boolean pressed = keybind.isPressed();
        leftUnpressed = Utils.keybinds.isMouseDown(0) ? 0 : leftUnpressed + 1;
        rightUnpressed = Utils.keybinds.isMouseDown(1) ? 0 : rightUnpressed + 1;

        if (ModuleManager.noFall.used) {
            disablePlacing(false);
            return;
        }
        if (ModuleManager.scaffold.isEnabled) {
            disablePlacing(false);
            return;
        }

        if (!pressed || mc.currentScreen != null) {
            disablePlacing(true);
            return;
        }

        plannedPlaceSlot = -1;
        int bestScore = Integer.MAX_VALUE;
        int currentSlot = ScriptDefaults.inventory.getSlot();

        for (int slot = 8; slot >= 0; --slot) {
            ItemStack s = ScriptDefaults.inventory.getStackInSlot(slot);
            if (s == null || s.stackSize == 0) continue;

            Integer score = BLOCK_SCORE.get(s.name);
            if (score != null && score < bestScore) {
                bestScore = score;
                plannedPlaceSlot = slot;
                if (score == 0) break;
            }
        }

        if (plannedPlaceSlot == -1) {
            disablePlacing(true);
            return;
        }

        Object[] res = roofAim();
        if (res == null) res = sidesAim();

        if (res == null) {
            disablePlacing(true);
            return;
        }

        if (!placingActive) {
            if (enablePlacing()) return;
        }

        if (skipTick) {
            skipTick = false;
            return;
        }

        if (plannedPlaceSlot != -1 && plannedPlaceSlot != currentSlot) {
            ScriptDefaults.inventory.setSlot(plannedPlaceSlot);
            swapped = true;
        }

        Object[] ray = (Object[]) res[0];
        Vec3  hit0  = (Vec3)  ray[0];
        String face0 = (String) ray[2];

        float aimYaw   = (float) res[1];
        float aimPitch = (float) res[2];
        Float[] sm = getRotationsSmoothed(aimYaw, aimPitch);

        double reach = range.getInput();
        Object[] chk = ScriptDefaults.client.raycastBlock(reach, sm[0], sm[1]);

        if (chk != null) {
            Vec3 hit1   = (Vec3)  chk[0];
            String face1 = (String) chk[2];
            if (hit1.equals(hit0) && face1.equals(face0)) {
                double tol = tolerance.getInput();
                if (Math.abs(sm[0] - serverYaw) <= tol && Math.abs(sm[1] - serverPitch) <= tol) {
                    hitPos = hit1;
                    face = face1;
                    placePos = ((Vec3) chk[1]).offset(hit1.x, hit1.y, hit1.z);
                    pendingPlace = true;
                }
            }
        }
        e.setRotations(sm[0], sm[1]);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (spoofItem.isToggled() && active) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        if (pendingPlace) {
            pendingPlace = false;
            if (ScriptDefaults.client.placeBlock(hitPos, face, placePos)) {
                mc.thePlayer.swingItem();
            }
        }

        filled = 0;
        boolean pressed = keybind.isPressed();
        if (pressed && mc.currentScreen == null) {
            Vec3 feet = ScriptDefaults.client.getPlayer().getPosition().floor();
            if (!canPlaceThrough(ScriptDefaults.world.getBlockAt(feet.offset(0, 2, 0)).name)) filled++;
            int[][] dirs = { { 1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1} };
            for (int[] d : dirs) {
                Vec3 posFeet = feet.offset(d[0], 0, d[2]);
                if (!canPlaceThrough(ScriptDefaults.world.getBlockAt(posFeet).name)) filled++;
                Vec3 posHead = feet.offset(d[0], 1, d[2]);
                if (!canPlaceThrough(ScriptDefaults.world.getBlockAt(posHead).name)) filled++;
            }
        }
    }

    private boolean enablePlacing() {
        if (placingActive) return false;
        placingActive = true;
        if (leftUnpressed < 2 || rightUnpressed < 2) skipTick = true;
        swapped = false;
        active = true;
        origSlot = ScriptDefaults.inventory.getSlot();

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        return true;
    }

    private void disablePlacing(boolean resetSlot) {
        if (!placingActive) return;

        if (resetSlot && swapped && origSlot != -1 && origSlot != ScriptDefaults.inventory.getSlot()) {
            ScriptDefaults.inventory.setSlot(origSlot);
            if (spoofItem.isToggled() && active) {
                ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
            }
        }

        placingActive = false;
        swapped = false;
        skipTick = false;
        origSlot = -1;
        plannedPlaceSlot = -1;
        active = false;
    }

    private Float[] getRotationsSmoothed(float targetYaw, float targetPitch) {
        float curYaw = serverYaw;
        float curPitch = serverPitch;

        float dYaw = targetYaw - curYaw;
        float dPit = targetPitch - curPitch;

        if (Math.abs(dYaw) < 0.1f) curYaw = targetYaw;
        if (Math.abs(dPit) < 0.1f) curPitch = targetPitch;
        if (curYaw == targetYaw && curPitch == targetPitch)
            return new Float[] { curYaw, curPitch };

        float maxStep = (float) speed.getInput();
        float random = 20;

        if (random > 0f) {
            float factor = 1f - (float) Utils.randomizeDouble(0, random / 100f);
            maxStep *= factor;
        }

        float stepYaw = Math.max(-maxStep, Math.min(maxStep, dYaw));
        float stepPit = Math.max(-maxStep, Math.min(maxStep, dPit));

        curYaw += stepYaw;
        curPitch += stepPit;

        if (Math.signum(targetYaw - curYaw) != Math.signum(dYaw)) curYaw = targetYaw;
        if (Math.signum(targetPitch - curPitch) != Math.signum(dPit)) curPitch = targetPitch;

        return new Float[] { curYaw, curPitch };
    }

    private Object[] roofAim() {
        Entity me = ScriptDefaults.client.getPlayer();

        Vec3 p = me.getPosition();
        if (!canPlaceThrough(ScriptDefaults.world.getBlockAt((int)Math.floor(p.x), (int)Math.floor(p.y) + 2, (int)Math.floor(p.z)).name)) return null;

        ItemStack held = ScriptDefaults.inventory.getStackInSlot(plannedPlaceSlot);
        double r = range.getInput();
        Vec3 eye = p.offset(0, me.getEyeHeight(), 0);
        double r2 = r * r, rp12 = (r + 1) * (r + 1);

        int minY = (int) Math.floor(eye.y) + 1, maxY = (int) Math.floor(eye.y + r);
        int minX = (int) Math.floor(eye.x - r), maxX = (int) Math.floor(eye.x + r);
        int minZ = (int) Math.floor(eye.z - r), maxZ = (int) Math.floor(eye.z + r);

        ArrayList<Object[]> cands = new ArrayList<>();
        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = (x + 0.5) - eye.x, dy = (y + 0.5) - eye.y, dz = (z + 0.5) - eye.z;
                    if (dx*dx + dy*dy + dz*dz > rp12) continue;

                    Block b = ScriptDefaults.world.getBlockAt(x, y, z);
                    if (canPlaceThrough(b.name)) continue;

                    double d2 = dist2PointAABB(eye, b);
                    if (d2 > r2) continue;

                    cands.add(new Object[]{ d2, b });
                }

        cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

        for (int i = 0; i < cands.size(); i++) {
            Block b = (Block) cands.get(i)[1];
            Object[] res = getBestRotationsToBlock(held, b, eye, r, minY);
            if (res != null) return res;
        }
        return null;
    }

    private Object[] getBestRotationsToBlock(ItemStack held, Block b, Vec3 eye, double reach, int minY) {
        double INSET = 0.05, STEP = 0.2, JIT = STEP * 0.2;
        boolean faceUP = Math.abs(eye.y - (b.y + 1)) < Math.abs(eye.y - b.y);
        boolean faceSOUTH = Math.abs(eye.z - (b.z + 1)) < Math.abs(eye.z - b.z);
        boolean faceEAST = Math.abs(eye.x - (b.x + 1)) < Math.abs(eye.x - b.x);
        float baseYaw = normYaw(serverYaw);
        float basePit = serverPitch;
        int n = (int) Math.round(1 / STEP);

        ArrayList<Object[]> cands = new ArrayList<>((n + 1) * (n + 1) * 3 + 1);
        cands.add(new Object[]{ 0D, baseYaw, basePit });

        for (int r = 0; r <= n; r++) {
            double v = r * STEP + Utils.randomizeDouble(-JIT, JIT);
            if (v < 0) v = 0; else if (v > 1) v = 1;

            for (int c = 0; c <= n; c++) {
                double u = c * STEP + Utils.randomizeDouble(-JIT, JIT);
                if (u < 0) u = 0; else if (u > 1) u = 1;

                float[] rV = getRotationsWrapped(eye, b.x + u, faceUP ? b.y + 1 - INSET : b.y + INSET, b.z + v);
                double costV = Math.abs((double) wrapYawDelta(baseYaw, rV[0])) + Math.abs((double) (rV[1] - basePit));
                cands.add(new Object[]{ costV, rV[0], rV[1] });

                float[] rZ = getRotationsWrapped(eye, b.x + u, b.y + v, faceSOUTH ? b.z + 1 - INSET : b.z + INSET);
                double costZ = Math.abs((double) wrapYawDelta(baseYaw, rZ[0])) + Math.abs((double) (rZ[1] - basePit));
                cands.add(new Object[]{ costZ, rZ[0], rZ[1] });

                float[] rX = getRotationsWrapped(eye, faceEAST ? b.x + 1 - INSET : b.x + INSET, b.y + v, b.z + u);
                double costX = Math.abs((double) wrapYawDelta(baseYaw, rX[0])) + Math.abs((double) (rX[1] - basePit));
                cands.add(new Object[]{ costX, rX[0], rX[1] });
            }
        }

        cands.sort((a, b2) -> Double.compare(((Number) a[0]).doubleValue(), ((Number) b2[0]).doubleValue()));

        Object[] best = null;
        float bestYaw = 0f, bestPit = 0f;
        Vec3 targetCell = new Vec3(b.x, b.y, b.z);

        for (int i = 0; i < cands.size(); i++) {
            float yawW = unwrapYaw(((Number) cands.get(i)[1]).floatValue(), serverYaw);
            float pit = ((Number) cands.get(i)[2]).floatValue();

            Object[] ray = ScriptDefaults.client.raycastBlock(reach, yawW, pit);
            if (ray == null) continue;

            Vec3 hit = (Vec3) ray[0];
            String face = (String) ray[2];

            Vec3 hitCell = hit.floor();
            int hitY = (int) hitCell.y;
            int byY = (int) targetCell.y;

            if (hitCell.equals(targetCell) && hitY >= minY && !("DOWN".equals(face) && byY == minY) && ScriptDefaults.client.canPlaceBlock(held, hit, face)) {
                best = ray; bestYaw = yawW; bestPit = pit;
                break;
            }
        }

        return best != null ? new Object[]{ best, bestYaw, bestPit } : null;
    }

    private double clamp(double v, double lo, double hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private double dist2PointAABB(Vec3 p, Block b) {
        double minX = b.x, maxX = b.x + 1;
        double minY = b.y, maxY = b.y + 1;
        double minZ = b.z, maxZ = b.z + 1;

        double cx = clamp(p.x, minX, maxX);
        double cy = clamp(p.y, minY, maxY);
        double cz = clamp(p.z, minZ, maxZ);

        double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
        return dx*dx + dy*dy + dz*dz;
    }

    private boolean canPlaceThrough(String name) {
        return placeThrough.contains(name);
    }

    private float normYaw(float yaw) { yaw = ((yaw % 360f) + 360f) % 360f; return (yaw > 180f) ? (yaw - 360f) : yaw; }
    private float wrapYawDelta(float base, float target) { float d = target - base; while (d <= -180f) d += 360f; while (d > 180f) d -= 360f; return d; }
    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }
    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.x, dy = ty - eye.y, dz = tz - eye.z;
        double hd = Math.sqrt(dx*dx + dz*dz);
        float yawWrapped = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        yawWrapped = normYaw(yawWrapped);
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, hd));
        return new float[]{ yawWrapped, pitch };
    }

    private Object[] sidesAim() {
        Entity me = ScriptDefaults.client.getPlayer();
        if (me == null) return null;

        Vec3 feet = me.getPosition().floor();
        Vec3 head = feet.offset(0, 1, 0);

        ArrayList<Vec3> primaryGoals = new ArrayList<>();
        ArrayList<Vec3> scaffoldGoals = new ArrayList<>();

        int[][] dirs = { {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1} };

        for (int i = 0; i < dirs.length; i++) {
            int[] d = dirs[i];
            Vec3 g1 = feet.offset(d[0], 0, d[2]);
            Vec3 g2 = head.offset(d[0], 0, d[2]);
            if (canPlaceThrough(ScriptDefaults.world.getBlockAt(g1).name)) primaryGoals.add(g1);
            if (canPlaceThrough(ScriptDefaults.world.getBlockAt(g2).name)) primaryGoals.add(g2);
        }
        if (primaryGoals.isEmpty()) return null;

        for (int i = 0; i < primaryGoals.size(); i++) {
            Vec3 g = primaryGoals.get(i);

            Vec3 down = offsetByFace(g, "DOWN");
            if (canPlaceThrough(ScriptDefaults.world.getBlockAt(down).name)) {
                Vec3 below = down.offset(0, -1, 0);
                if (!canPlaceThrough(ScriptDefaults.world.getBlockAt(below).name)) {
                    scaffoldGoals.add(down);
                    continue;
                }
            }
            String[] others = { "UP", "NORTH", "SOUTH", "EAST", "WEST" };
            for (int j = 0; j < others.length; j++) {
                Vec3 s = offsetByFace(g, others[j]);
                if (canPlaceThrough(ScriptDefaults.world.getBlockAt(s).name)) scaffoldGoals.add(s);
            }
        }

        double reach = range.getInput();
        Vec3 eye = me.getPosition().offset(0, me.getEyeHeight(), 0);

        Object[] res = findBestForGoals(primaryGoals, reach, eye);
        if (res != null) return res;

        return findBestForGoals(scaffoldGoals, reach, eye);
    }

    private Object[] findBestForGoals(List<Vec3> goals, double reach, Vec3 eye) {
        if (goals == null || goals.isEmpty()) return null;

        ItemStack held = ScriptDefaults.inventory.getStackInSlot(plannedPlaceSlot);
        double INSET = 0.05, STEP = 0.2, JIT = 0.2, insetTop = 1 - INSET - 1e-3, insetBot = INSET + 1e-3;
        int GRID = (int) Math.round(1 / STEP);
        String[] opp = { "DOWN", "UP", "SOUTH", "NORTH", "WEST", "EAST" };
        int[] dx = { 0, 0, 0, 0, 1, -1 };
        int[] dy = { 1, -1, 0, 0, 0, 0 };
        int[] dz = { 0, 0, -1, 1, 0, 0 };

        float curYawW = normYaw(serverYaw), curPitch = serverPitch;

        Object[] now = ScriptDefaults.client.raycastBlock(reach, curYawW, curPitch);
        if (now != null) {
            Vec3 hg = (Vec3) now[0]; String face = (String) now[2];
            if (!canPlaceThrough(ScriptDefaults.world.getBlockAt(hg).name) && ScriptDefaults.client.canPlaceBlock(held, hg, face)) {
                Vec3 plc = offsetByFace(hg, face);
                for (int i = 0; i < goals.size(); i++) {
                    Vec3 g = goals.get(i);
                    if (plc.floor().equals(g.floor()))
                        return new Object[]{ now, serverYaw, serverPitch };
                }
            }
        }

        int cells = (GRID + 1) * (GRID + 1);
        ArrayList<Object[]> cands = new ArrayList<>(Math.max(16, goals.size() * 6 * cells));

        for (int gi = 0; gi < goals.size(); gi++) {
            Vec3 g = goals.get(gi);

            for (int i = 0; i < 6; i++) {
                Vec3 support = new Vec3(g.x + dx[i], g.y + dy[i], g.z + dz[i]);
                String face = opp[i];

                String supportName = ScriptDefaults.world.getBlockAt(support).name;
                if (canPlaceThrough(supportName)) continue;
                if (!ScriptDefaults.client.canPlaceBlock(held, support, face)) continue;

                for (int rr = 0; rr <= GRID; rr++) {
                    boolean ltr = (rr & 1) == 0;
                    double v = rr * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT);
                    if (v < 0) v = 0; else if (v > 1) v = 1;

                    for (int cc = 0; cc <= GRID; cc++) {
                        double cu = cc * STEP + Utils.randomizeDouble(-STEP * JIT, STEP * JIT);
                        if (cu < 0) cu = 0; else if (cu > 1) cu = 1;
                        double u = ltr ? cu : 1 - cu;

                        double px, py, pz;
                        if (i < 2) {
                            px = support.x + u;
                            pz = support.z + v;
                            py = support.y + (i == 1 ? insetTop : insetBot);
                        } else if (i < 4) {
                            px = support.x + u;
                            py = support.y + v;
                            pz = support.z + (i == 2 ? insetTop : insetBot);
                        } else {
                            pz = support.z + u;
                            py = support.y + v;
                            px = support.x + (i == 5 ? insetTop : insetBot);
                        }

                        float[] rot = getRotationsWrapped(eye, px, py, pz);
                        float yawW = rot[0], pit = rot[1];

                        float dYaw = Math.abs(wrapYawDelta(curYawW, yawW));
                        float dPit = Math.abs(pit - curPitch);
                        if (dYaw < 0.1f && dPit < 0.1f) continue;

                        double cost = dYaw + dPit + (i == 1 ? -0.25 : 0);
                        cands.add(new Object[]{ cost, yawW, pit, support, face, g });
                    }
                }
            }
        }

        if (cands.isEmpty()) return null;

        cands.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));

        for (int i = 0; i < cands.size(); i++) {
            float yawW = (Float) cands.get(i)[1];
            float pit = (Float) cands.get(i)[2];
            Vec3 support = (Vec3) cands.get(i)[3];
            String face = (String) cands.get(i)[4];
            Vec3 goal = (Vec3) cands.get(i)[5];

            float yawUnwrapped = unwrapYaw(yawW, serverYaw);
            Object[] ray = ScriptDefaults.client.raycastBlock(reach, yawUnwrapped, pit);
            if (ray == null) continue;

            Vec3 hitGrid = (Vec3) ray[0];
            String faceHit = (String) ray[2];

            if (!hitGrid.floor().equals(support.floor())) continue;
            if (!face.equals(faceHit)) continue;

            Vec3 plc = offsetByFace(hitGrid, faceHit);
            if (!plc.floor().equals(goal.floor())) continue;

            return new Object[]{ ray, yawUnwrapped, pit };
        }

        return null;
    }

    private Vec3 offsetByFace(Vec3 pos, String face) {
        switch (face) {
            case "UP":
                return pos.offset(0, 1, 0);
            case "DOWN":
                return pos.offset(0, -1, 0);
            case "NORTH":
                return pos.offset(0, 0, -1);
            case "SOUTH":
                return pos.offset(0, 0, 1);
            case "EAST":
                return pos.offset(1, 0, 0);
            case "WEST":
                return pos.offset(-1, 0, 0);
            default:
                return pos;
        }
    }

}
