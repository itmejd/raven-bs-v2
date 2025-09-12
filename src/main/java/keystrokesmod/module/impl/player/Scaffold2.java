package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.module.Module;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Scaffold2 extends Module {

    private PlaceObjective tempObjective;
    private MovingObjectPosition targetPlacement;

    private float[] previousRotations;

    public Scaffold2() {
        super("Scaffold2", category.player);
    }

    @SubscribeEvent
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck() || !isHoldingBlocks()) {
            return;
        }
        PlaceObjective placeObjective = getPlaceObjective();
        tempObjective = null;
        if (placeObjective == null) {
            if (this.previousRotations != null) {
                e.setYaw(this.previousRotations[0]);
                e.setPitch(this.previousRotations[1]);
            }
            return;
        }
        tempObjective = placeObjective;
        e.setYaw(placeObjective.yaw);
        e.setPitch(placeObjective.pitch);

        this.previousRotations = new float[] { placeObjective.yaw, placeObjective.pitch };

        MovingObjectPosition rayCastWithRotations = RotationUtils.raycastBlock(mc.playerController.getBlockReachDistance(), placeObjective.yaw, placeObjective.pitch);

        if (rayCastWithRotations != null && rayCastWithRotations.sideHit == placeObjective.rayCasted.sideHit && rayCastWithRotations.getBlockPos().equals(placeObjective.rayCasted.getBlockPos())) {
            targetPlacement = rayCastWithRotations;
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (this.targetPlacement == null) {
            return;
        }
        this.place(this.targetPlacement);
        this.targetPlacement = null;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (tempObjective != null && Utils.nullCheck()) {
            RenderUtils.renderBlock(tempObjective.rayCasted.getBlockPos(), new Color(0, 255, 0).getRGB(), false, true);
        }
    }

    private void place(MovingObjectPosition mop) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, mop.getBlockPos(), mop.sideHit, mop.hitVec)) {
            mc.thePlayer.swingItem();
        }
    }

    private boolean isHoldingBlocks() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return false;
        }
        return true;
    }

    private PlaceObjective getPlaceObjective() {
        Vec3 position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        BlockPos positionBelow = new BlockPos((int) Math.floor(position.xCoord), (int) Math.floor(position.yCoord) - 1, (int) Math.floor(position.zCoord));

        if (canPlaceOn(positionBelow)) {
            return null;
        }

        Vec3 eyePositon = position.addVector(0, mc.thePlayer.getEyeHeight(), 0);

        double reach = mc.playerController.getBlockReachDistance();

        double reachSqr = reach * reach;
        double reachPlusOneSqr = (reach + 1) * (reach + 1);

        int minX = (int) Math.floor(eyePositon.xCoord - reach);
        int minY = (int) Math.floor(eyePositon.yCoord - reach - 1.0);
        int minZ = (int) Math.floor(eyePositon.zCoord - reach);

        int maxX = (int) Math.floor(eyePositon.xCoord + reach);
        int maxY = (int) Math.floor(position.yCoord) - 1;
        int maxZ = (int) Math.floor(eyePositon.zCoord + reach);

        ArrayList<PotentialBlock> candidates = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = (x + 0.5) - eyePositon.xCoord;
                    double dy = (y + 0.5) - eyePositon.yCoord;
                    double dz = (z + 0.5) - eyePositon.zCoord;

                    if (dx * dx + dy * dy + dz * dz > reachPlusOneSqr) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    if (!canPlaceOn(pos)) {
                        continue;
                    }

                    double distance = distanceToPointAABB(eyePositon, BlockUtils.getAABB(pos));
                    if (distance > reachSqr) {
                        continue;
                    }

                    candidates.add(new PotentialBlock(distance, pos));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(a -> a.distance));

        if (!candidates.isEmpty()) {
            BlockPos b = candidates.get(0).blockPos;
            PlaceObjective res = getBestRotationsToBlock(mc.thePlayer.getHeldItem(), b, eyePositon, reach);
            if (res != null) {
                return res;
            }
        }

        return null;
    }

    private PlaceObjective getBestRotationsToBlock(ItemStack held, BlockPos blockPos, Vec3 eyePosition, double reach) {
        double INSET = 0.05, STEP = 0.2, JIT = STEP * 0.2;
        boolean faceUP = Math.abs(eyePosition.yCoord - (blockPos.getY() + 1)) < Math.abs(eyePosition.yCoord - blockPos.getY());
        boolean faceSOUTH = Math.abs(eyePosition.zCoord - (blockPos.getZ() + 1)) < Math.abs(eyePosition.zCoord - blockPos.getZ());
        boolean faceEAST = Math.abs(eyePosition.xCoord - (blockPos.getX() + 1)) < Math.abs(eyePosition.xCoord - blockPos.getX());
        float baseYaw = normYaw(((IAccessorEntityPlayerSP) mc.thePlayer).getLastReportedYaw());
        float basePitch = ((IAccessorEntityPlayerSP) mc.thePlayer).getLastReportedPitch();
        int n = (int) Math.round(1 / STEP);

        ArrayList<Object[]> cands = new ArrayList<>((n + 1) * (n + 1) * 3 + 1);
        cands.add(new Object[]{ 0D, baseYaw, basePitch });

        for (int r = 0; r <= n; r++) {
            double v = r * STEP + Utils.randomizeDouble(-JIT, JIT);
            if (v < 0) v = 0; else if (v > 1) v = 1;

            for (int c = 0; c <= n; c++) {
                double u = c * STEP + Utils.randomizeDouble(-JIT, JIT);
                if (u < 0) u = 0; else if (u > 1) u = 1;

                float[] rV = getRotationsWrapped(eyePosition, blockPos.getX() + u, faceUP ? blockPos.getY() + 1 - INSET : blockPos.getY() + INSET, blockPos.getZ() + v);
                double costV = Math.abs((double) wrapYawDelta(baseYaw, rV[0])) + Math.abs((double) (rV[1] - basePitch));
                cands.add(new Object[]{ costV, rV[0], rV[1] });

                float[] rZ = getRotationsWrapped(eyePosition, blockPos.getX() + u, blockPos.getY() + v, faceSOUTH ? blockPos.getZ() + 1 - INSET : blockPos.getZ() + INSET);
                double costZ = Math.abs((double) wrapYawDelta(baseYaw, rZ[0])) + Math.abs((double) (rZ[1] - basePitch));
                cands.add(new Object[]{ costZ, rZ[0], rZ[1] });

                float[] rX = getRotationsWrapped(eyePosition, faceEAST ? blockPos.getX() + 1 - INSET : blockPos.getX() + INSET, blockPos.getY() + v, blockPos.getZ() + u);
                double costX = Math.abs((double) wrapYawDelta(baseYaw, rX[0])) + Math.abs((double) (rX[1] - basePitch));
                cands.add(new Object[]{ costX, rX[0], rX[1] });
            }
        }

        cands.sort(Comparator.comparingDouble(a -> ((Number) a[0]).doubleValue()));

        MovingObjectPosition best = null;
        float bestYaw = 0f, bestPit = 0f;

        for (int i = 0; i < cands.size(); i++) {
            float yawW = unwrapYaw(((Number) cands.get(i)[1]).floatValue(), ((IAccessorEntityPlayerSP) mc.thePlayer).getLastReportedYaw());
            float pit = ((Number) cands.get(i)[2]).floatValue();

            MovingObjectPosition rayCastResult = RotationUtils.raycastBlock(reach, yawW, pit);
            if (rayCastResult == null) {
                continue;
            }

            BlockPos hit = rayCastResult.getBlockPos();
            EnumFacing face = rayCastResult.sideHit;

            int hitY = hit.getY();

            if (hit.equals(blockPos) && hitY <= Math.floor(mc.thePlayer.posY) - 1 && canPlaceBlock(held, hit, face)) {
                best = rayCastResult;
                bestYaw = yawW;
                bestPit = pit;
                break;
            }
        }

        return best != null ? new PlaceObjective(best, bestYaw, bestPit) : null;
    }

    private boolean canPlaceOn(BlockPos blockPos) {
        return !BlockUtils.replaceable(blockPos) && !BlockUtils.isInteractable(BlockUtils.getBlock(blockPos));
    }

    private boolean canPlaceBlock(ItemStack stack, BlockPos pos, EnumFacing side) {
        if (stack == null || stack.getItem() == null || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        return ((ItemBlock) stack.getItem()).canPlaceBlockOnSide(mc.theWorld, pos, side, mc.thePlayer, stack);
    }

    private double distanceToPointAABB(Vec3 pos, AxisAlignedBB box) {
        double dx = pos.xCoord - Math.max(box.minX, Math.min(pos.xCoord, box.maxX));
        double dy = pos.yCoord - Math.max(box.minY, Math.min(pos.yCoord, box.maxY));
        double dz = pos.zCoord - Math.max(box.minZ, Math.min(pos.zCoord, box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    private float[] getRotationsWrapped(Vec3 eye, double tx, double ty, double tz) {
        double dx = tx - eye.xCoord;
        double dy = ty - eye.yCoord;
        double dz = tz - eye.zCoord;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        yaw = normYaw(yaw);

        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDist));

        return new float[]{ yaw, pitch };
    }

    private float wrapYawDelta(float base, float target) {
        float delta = target - base;
        while (delta <= -180f) delta += 360f;
        while (delta > 180f) delta -= 360f;
        return delta;
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private float normYaw(float yaw) {
        yaw = (yaw % 360f + 360f) % 360f;
        if (yaw > 180f) {
            yaw -= 360f;
        }
        return yaw;
    }

    public class PlaceObjective {
        public MovingObjectPosition rayCasted;
        public float yaw;
        public float pitch;

        public PlaceObjective(MovingObjectPosition rayCasted, float yaw, float pitch) {
            this.rayCasted = rayCasted;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    public class PotentialBlock {
        public double distance;
        public BlockPos blockPos;

        public PotentialBlock(double distance, BlockPos blockPos) {
            this.distance = distance;
            this.blockPos = blockPos;
        }
    }
}