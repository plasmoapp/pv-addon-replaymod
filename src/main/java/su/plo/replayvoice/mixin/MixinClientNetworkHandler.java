package su.plo.replayvoice.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.replayvoice.network.EmptySocketConnection;
import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.config.ServerSettings;
import su.plo.voice.client.network.ClientNetworkHandler;
import su.plo.voice.common.packets.tcp.ConfigPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = ClientNetworkHandler.class, remap = false)
public abstract class MixinClientNetworkHandler {
    @Inject(method = "handle(Lsu/plo/voice/common/packets/tcp/ConfigPacket;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void handle(ConfigPacket packet, CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        if (VoiceClient.getServerConfig() == null) {
            VoiceClient.setServerConfig(new ServerSettings(
                    "",
                    "127.0.0.1",
                    25565,
                    false
            ));

            if (!VoiceClient.getSoundEngine().initialized) {
                VoiceClient.getSoundEngine().start();
            }
        }

        VoiceClient.getServerConfig().update(packet);
        VoiceClient.socketUDP = new EmptySocketConnection();
        VoiceClient.socketUDP.start();

        ci.cancel();
    }
}
