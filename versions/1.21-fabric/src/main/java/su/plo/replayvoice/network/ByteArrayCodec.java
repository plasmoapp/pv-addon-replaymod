package su.plo.replayvoice.network;

import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Getter
public class ByteArrayCodec implements StreamCodec<RegistryFriendlyByteBuf, ByteArrayPayload> {

    private final CustomPacketPayload.Type<ByteArrayPayload> type;

    public ByteArrayCodec(@NotNull ResourceLocation channelKey) {
        this.type = new CustomPacketPayload.Type<>(channelKey);
    }

    @Override
    public ByteArrayPayload decode(RegistryFriendlyByteBuf buf) {
        int length = buf.readableBytes();

        byte[] data = new byte[length];
        buf.readBytes(data);

        return new ByteArrayPayload(type, data);
    }

    @Override
    public void encode(RegistryFriendlyByteBuf buf, ByteArrayPayload payload) {
        buf.writeBytes(payload.data());
    }
}
