package su.plo.replayvoice.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.replayvoice.ReplayVoicechat;
import su.plo.voice.client.socket.SocketClientUDP;
import su.plo.voice.common.packets.Packet;
import su.plo.voice.common.packets.udp.VoiceClientPacket;
import su.plo.voice.common.packets.udp.VoiceEndClientPacket;
import su.plo.voice.common.packets.udp.VoiceServerPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

import java.util.UUID;

@Mixin(value = SocketClientUDP.class, remap = false)
public class MixinSoundClientUDP {
    private static final Minecraft minecraft = Minecraft.getInstance();

    @Inject(method = "send", at = @At("HEAD"))
    public void send(Packet packet, CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isReplayModActive() || minecraft.player == null) return;
        UUID ownUUID = minecraft.player.getUUID();

        if (packet instanceof VoiceClientPacket voicePacket) {
            ReplayVoicechat.record(
                    new VoiceServerPacket(
                            voicePacket.getData(),
                            ownUUID,
                            voicePacket.getSequenceNumber(),
                            voicePacket.getDistance()
                    )
            );
        } else if (packet instanceof VoiceEndClientPacket voicePacket) {
            ReplayVoicechat.record(
                    new VoiceServerPacket(
                            new byte[0],
                            ownUUID,
                            0L,
                            voicePacket.getDistance()
                    )
            );
        }
    }
}
