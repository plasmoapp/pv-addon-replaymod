package su.plo.replayvoice.network;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.event.connection.ServerInfoInitializedEvent;
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent;
import su.plo.voice.api.client.event.socket.UdpClientConnectedEvent;
import su.plo.voice.api.client.socket.UdpClient;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.proto.packets.Packet;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class DummyUdpClient implements UdpClient {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 1);

    private final PlasmoVoiceClient voiceClient;
    private final UUID secret;

    @Getter
    private boolean connected;

    @Override
    public void connect(String ip, int port) {
    }

    @Override
    public void close(UdpClientClosedEvent.@NotNull Reason reason) {
    }

    @Override
    public void sendPacket(Packet<?> packet) {
    }

    @Override
    public @NotNull UUID getSecret() {
        return secret;
    }

    @Override
    public Optional<InetSocketAddress> getRemoteAddress() {
        return Optional.of(ADDRESS);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isTimedOut() {
        return false;
    }

    @EventSubscribe
    public void onServerInfoUpdate(@NotNull ServerInfoInitializedEvent event) {
        if (connected) return;

        LOGGER.info("Connected to fake UDP client");
        this.connected = true;

        voiceClient.getEventBus().call(new UdpClientConnectedEvent(this));
    }
}
