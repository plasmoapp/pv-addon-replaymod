package su.plo.replayvoice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.client.sound.Recorder;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = Recorder.class, remap = false)
public class MixinRecorder {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    public void start(CallbackInfo ci) {
        if (ReplayInterface.INSTANCE.isInReplayEditor) ci.cancel();
    }
}
