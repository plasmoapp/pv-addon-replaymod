package su.plo.replayvoice.mixin;

import com.replaymod.recording.packet.PacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PacketListener.class)
public interface ReplayPacketListenerAccessor {

    @Invoker("encodeMcPacket")
    static com.replaymod.replaystudio.protocol.Packet invokeEncodeMcPacket(ConnectionProtocol connectionState, Packet<?> packet) throws Exception {
        throw new AssertionError();
    }
}
