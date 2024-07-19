package su.plo.replayvoice.network;

import com.google.common.collect.Maps;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.LogManager;
import su.plo.replayvoice.CameraUtil;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.source.ClientSelfSourceInfo;
import su.plo.voice.api.util.CircularBuffer;
import su.plo.voice.proto.data.audio.source.DirectSourceInfo;
import su.plo.voice.proto.packets.Packet;
import su.plo.voice.proto.packets.udp.clientbound.SelfAudioInfoPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class ClientNetworkHandler {

    private static final int MAX_PACKETS = 50;

    private final PlasmoVoiceClient voiceClient;

    private final Map<Long, Integer> packetIndex = Maps.newHashMap();
    private final List<PlayerAudioPacket> packets = Lists.newArrayList();

    private int currentPacketIndex = 0;

    public void handleKeyPairPacket(byte[] data) {
        ByteArrayDataInput buf = ByteStreams.newDataInput(data);

        byte[] publicKey = new byte[buf.readInt()];
        buf.readFully(publicKey);
        byte[] privateKey = new byte[buf.readInt()];
        buf.readFully(privateKey);

        voiceClient.getServerConnection().ifPresent((connection) -> {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
                EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);

                connection.setKeyPair(new KeyPair(
                        keyFactory.generatePublic(publicKeySpec),
                        keyFactory.generatePrivate(privateKeySpec)
                ));
            } catch (Exception e) {
                LogManager.getLogger().error("Failed to generate RSA public key: {}", e.toString());
                e.printStackTrace();
            }
        });
    }

    public void handleSelfAudioPacket(byte[] data) {
        if (ReplayInterface.INSTANCE.skipping) return;

        PlayerAudioPacket packet;
        try {
            packet = getPacket(data, PlayerAudioPacket.class);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return;
        }

        PlayerAudioPacket currentPacket = packets.size() > currentPacketIndex ? packets.get(currentPacketIndex) : null;
        if (currentPacket != null) {
            packetIndex.remove(currentPacket.getSequenceNumber());
        }

        if (packets.size() > currentPacketIndex) {
            packets.set(currentPacketIndex, packet);
        } else {
            packets.add(packet);
        }
        packetIndex.put(packet.getSequenceNumber(), currentPacketIndex);

        currentPacketIndex = (currentPacketIndex + 1) % MAX_PACKETS;
    }

    public void handleSelfAudioInfoPacket(byte[] data) {
        if (ReplayInterface.INSTANCE.skipping) return;

        SelfAudioInfoPacket packet;
        try {
            packet = getPacket(data, SelfAudioInfoPacket.class);
        } catch (Exception ignored) {
            return;
        }

        boolean shouldPlay = voiceClient.getSourceManager().getSelfSourceInfo(packet.getSourceId()).map((selfSourceInfo) -> {
            if (!CameraUtil.isReplayRecorder() && selfSourceInfo.getSelfSourceInfo().getSourceInfo() instanceof DirectSourceInfo) return false;

            if (voiceClient.getSourceManager().getSourceById(packet.getSourceId(), false).isPresent()) return true;

            voiceClient.getSourceManager().update(selfSourceInfo.getSelfSourceInfo().getSourceInfo());
            return true;
        }).orElse(true);

        voiceClient.getSourceManager().getSourceById(packet.getSourceId(), false).ifPresent((source) -> {
            if (source.isActivated() && !shouldPlay) {
                source.closeAsync();
            } else if (!shouldPlay) return;

            Optional<byte[]> packetData = packet.getData();
            if (!packetData.isPresent()) {
                if (packet.getSequenceNumber() == 0) return;

                int index = packetIndex.getOrDefault(packet.getSequenceNumber(), -1);
                if (index == -1) return;

                packetData = Optional.of(packets.get(index).getData());
            }

            packetData.ifPresent((bytes) -> source.process(new SourceAudioPacket(
                    packet.getSequenceNumber(),
                    source.getSourceInfo().getState(),
                    bytes,
                    packet.getSourceId(),
                    packet.getDistance()
            )));
        });
    }

    public void handleSourceAudioPacket(byte[] data) {
        if (ReplayInterface.INSTANCE.skipping) return;

        SourceAudioPacket packet;
        try {
            packet = getPacket(data, SourceAudioPacket.class);
        } catch (Exception ignored) {
            return;
        }

        voiceClient.getSourceManager().getSourceById(packet.getSourceId())
                .ifPresent(source -> {
                    if (source.getSourceInfo().getState() != packet.getSourceState()) return;
                    source.process(packet);
                });
    }

    private <T extends Packet<?>> T getPacket(byte[] data, Class<?> packetClass) throws Exception {
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        T packet = (T) packetClass.getConstructor().newInstance();
        packet.read(in);

        return packet;
    }
}
