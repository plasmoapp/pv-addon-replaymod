package su.plo.replayvoice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.replayvoice.ReplayVoicechat;
import su.plo.voice.client.sound.AbstractSoundQueue;
import su.plo.voice.common.packets.udp.VoiceServerPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = AbstractSoundQueue.class, remap = false)
public class MixinAbstractSoundQueue {
    @Inject(method = "addQueue", at = @At("HEAD"))
    public void addQueue(VoiceServerPacket packet, CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isReplayModActive()) return;
        ReplayVoicechat.record(packet);
    }
}
