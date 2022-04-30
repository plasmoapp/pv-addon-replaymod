package su.plo.replayvoice.mixin;

import org.lwjgl.openal.AL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.client.sound.openal.CustomSource;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = CustomSource.class, remap = false)
public abstract class MixinCustomSource {
    @Shadow
    @Final
    protected int pointer;

    @Inject(method = "play", at = @At("HEAD"))
    public void play(CallbackInfo ci) {
        if (ReplayInterface.INSTANCE.isInReplayEditor) {
            AL11.alSourcef(pointer, AL11.AL_PITCH, (float) ReplayInterface.getCurrentSpeed());
        }
    }
}
