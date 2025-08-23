package keystrokesmod.mixin.impl.accessor;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface IAccessorEntityLivingBase {
    @Accessor("jumpTicks")
    void setJumpTicks(int ticks);

    @Accessor("jumpTicks")
    int getJumpTicks();
}