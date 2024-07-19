package su.plo.replayvoice.network;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

//#if MC>=12100
//$$ import net.minecraft.network.protocol.common.ClientCommonPacketListener;
//#else
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
//#endif

@UtilityClass
public final class NetworkHelper {

    //#if MC>=12100
    //$$ public static Packet<ClientCommonPacketListener> createS2CPacket(@NotNull ResourceLocation resourceLocation, byte[] data) {
    //$$     //#if MC>=12100
    //$$     ByteArrayCodec codec = CodecManager.getCodec(resourceLocation);
    //$$
    //$$     return ServerPlayNetworking.createS2CPacket(new ByteArrayPayload(codec.getType(), data));
    //$$ }
    //#else
    public static Packet<ClientGamePacketListener> createS2CPacket(@NotNull ResourceLocation resourceLocation, byte[] data) {
        return ServerPlayNetworking.createS2CPacket(
                resourceLocation,
                new FriendlyByteBuf(Unpooled.wrappedBuffer(data))
        );
    }
    //#endif
}
