package su.plo.replayvoice.mixin;

import com.replaymod.replay.FullReplaySender;
import com.replaymod.replay.ReplaySender;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.replayvoice.ReplayVoiceAddon;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = FullReplaySender.class, remap = false)
public class MixinFullReplaySender {

    @Inject(method = "processPacket", at = @At("HEAD"), cancellable = true)
    public void processPacket(Packet p, CallbackInfoReturnable<Packet> cir) {
        if (!(p instanceof ClientboundCustomPayloadPacket)) return;

        ClientboundCustomPayloadPacket packet = (ClientboundCustomPayloadPacket) p;
        //#if MC>=12100
        //$$ ResourceLocation packetId = packet.payload().type().id();
        //#else
        ResourceLocation packetId = packet.getIdentifier();
        //#endif

        if (!packetId.equals(ReplayVoiceAddon.SELF_AUDIO_PACKET) &&
                !packetId.equals(ReplayVoiceAddon.SELF_AUDIO_INFO_PACKET) &&
                !packetId.equals(ReplayVoiceAddon.SOURCE_AUDIO_PACKET)
        ) return;

        boolean hurrying = false;
        ReplaySender replaySender = ReplayInterface.INSTANCE.replayHandler.getReplaySender();
        if (replaySender instanceof FullReplaySender) {
            FullReplaySender fullReplaySender = (FullReplaySender) replaySender;
            hurrying = fullReplaySender.isHurrying();
        }

        if (!ReplayInterface.INSTANCE.skipping && !hurrying) return;

        cir.setReturnValue(null);
    }
}
