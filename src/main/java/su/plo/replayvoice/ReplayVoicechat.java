package su.plo.replayvoice;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.plo.replayvoice.network.ClientNetworkHandler;
import su.plo.voice.common.packets.udp.VoiceServerPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

import java.io.IOException;

public class ReplayVoicechat implements ClientModInitializer {
    private final static ResourceLocation VOICE_PACKET_ID = new ResourceLocation("plasmo:voice/voice_packet");

    public static final String MOD_ID = "replayvoicechat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientNetworkHandler network = new ClientNetworkHandler();
        ClientPlayNetworking.registerGlobalReceiver(VOICE_PACKET_ID, network::handle);
    }

    public static void record(VoiceServerPacket packet) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            packet.write(out);

            FriendlyByteBuf buf = new FriendlyByteBuf(
                    Unpooled.wrappedBuffer(out.toByteArray())
            );
            ReplayInterface.INSTANCE.sendFakePacket(VOICE_PACKET_ID, buf);
        } catch (IOException e) {
            LOGGER.warn("Failed to serialize packet: {}", e.getMessage());
        }
    }
}
