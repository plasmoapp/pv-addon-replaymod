package xyz.breadloaf.replaymodinterface.mixin;

import com.replaymod.render.rendering.Pipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Pipeline.class,remap = false)
public class VideoRendererMixin {


    @Shadow private int consumerNextFrame;

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/ExecutorService;submit(Ljava/lang/Runnable;)Ljava/util/concurrent/Future;"))
    public void injected(CallbackInfo ci) {
        if (this.consumerNextFrame == 0) {
//            VoicechatVoiceRenderer.onStartRendering();
        }
    }
}
