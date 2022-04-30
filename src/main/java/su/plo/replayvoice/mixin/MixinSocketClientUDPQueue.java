package su.plo.replayvoice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.replayvoice.ReplayVoicechat;
import su.plo.voice.client.socket.SocketClientUDPQueue;
import su.plo.voice.common.packets.udp.VoiceServerPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

import java.util.UUID;

@Mixin(value = SocketClientUDPQueue.class, remap = false)
public abstract class MixinSocketClientUDPQueue {
    @Inject(method = "queuePacket", at = @At(value = "HEAD"))
    public void queuePacket(VoiceServerPacket packet, UUID uuid, CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isReplayModActive()) return;
        ReplayVoicechat.record(packet);
    }
}
