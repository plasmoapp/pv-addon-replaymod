package su.plo.replayvoice.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.socket.SocketClientUDPQueue;
import su.plo.voice.client.sound.AbstractSoundQueue;
import su.plo.voice.client.sound.openal.OpenALPlayerQueue;
import su.plo.voice.common.packets.udp.VoiceServerPacket;

import java.io.IOException;

public class ClientNetworkHandler {
    public void handle(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender sender) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        VoiceServerPacket packet = new VoiceServerPacket();
        try {
            packet.read(in);
        } catch (IOException ignored) {
            return;
        }

        if (packet.getData().length > 0) {
            SocketClientUDPQueue.talking.put(packet.getFrom(), packet.getDistance() > VoiceClient.getServerConfig().getMaxDistance());
        } else {
            SocketClientUDPQueue.talking.remove(packet.getFrom());
        }

        // SocketClientUDPQueue.queuePacket
        AbstractSoundQueue ch = SocketClientUDPQueue.audioChannels.get(packet.getFrom());
        if (ch != null && !ch.isClosed()) {
            ch.addQueue(packet);
        } else {
            VoiceClient.getServerConfig().getClients().add(packet.getFrom());
            ch = new OpenALPlayerQueue(packet.getFrom());
            ch.addQueue(packet);

            SocketClientUDPQueue.audioChannels.put(packet.getFrom(), ch);
        }
    }
}
