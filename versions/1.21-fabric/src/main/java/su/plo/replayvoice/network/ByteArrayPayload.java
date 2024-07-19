package su.plo.replayvoice.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ByteArrayPayload(CustomPacketPayload.Type<ByteArrayPayload> type, byte[] data) implements CustomPacketPayload {

}