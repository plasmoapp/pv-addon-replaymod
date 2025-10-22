package su.plo.replayvoice.mixin;

import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.replayvoice.render.VoiceAudioRender;
import su.plo.voice.client.audio.device.AlOutputDevice;

import java.nio.Buffer;
import java.nio.IntBuffer;

@Mixin(value = AlOutputDevice.class, remap = false)
public final class MixinAlOutputDevice {

    @Inject(method = "openDevice", at = @At("HEAD"), cancellable = true)
    private void openDevice(String deviceName, CallbackInfoReturnable<Long> cir) {
        if (!VoiceAudioRender.isRendering()) return;

        long devicePointer = SOFTLoopback.alcLoopbackOpenDeviceSOFT((String) null);
        cir.setReturnValue(devicePointer);
    }

    @Redirect(method = "openSync", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/ALC11;alcCreateContext(JLjava/nio/IntBuffer;)J"))
    private long createContext(long devicePointer, IntBuffer attrList) {
        if (!VoiceAudioRender.isRendering()) {
            return ALC11.alcCreateContext(devicePointer, attrList);
        }

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer intBuffer = memoryStack.callocInt(7)
                    .put(ALC10.ALC_FREQUENCY).put(48000)
                    .put(SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT).put(SOFTLoopback.ALC_STEREO_SOFT)
                    .put(SOFTLoopback.ALC_FORMAT_TYPE_SOFT).put(SOFTLoopback.ALC_SHORT_SOFT)
                    .put(0);
            ((Buffer) intBuffer).flip();
            return ALC10.alcCreateContext(devicePointer, intBuffer);
        }
    }
}
