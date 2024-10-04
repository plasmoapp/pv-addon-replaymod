package su.plo.replayvoice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.replayvoice.render.VoiceAudioRender;

@Mixin(targets = "com.replaymod.render.rendering.Pipeline$ProcessTask", remap = false)
public final class MixinProcessTask {

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lcom/replaymod/render/rendering/FrameConsumer;consume(Ljava/util/Map;)V", shift = At.Shift.BEFORE))
    public void run(CallbackInfo ci) {
        VoiceAudioRender.AUDIO_RENDER.render();
    }
}
