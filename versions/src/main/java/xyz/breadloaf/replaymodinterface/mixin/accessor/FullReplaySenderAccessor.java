package xyz.breadloaf.replaymodinterface.mixin.accessor;

import com.replaymod.replay.FullReplaySender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FullReplaySender.class, remap = false)
public interface FullReplaySenderAccessor {
    @Accessor("lastTimeStamp")
    int getLastTimeStamp();
}
