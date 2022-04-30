package su.plo.replayvoice.network;

import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.socket.SocketConnection;
import su.plo.voice.common.packets.Packet;

import java.io.IOException;

public class EmptySocketConnection implements SocketConnection {
    @Override
    public void close() {
    }

    @Override
    public void send(Packet packet) throws IOException {
    }

    @Override
    public void start() {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isTimedOut() {
        return false;
    }

    @Override
    public boolean isAuthorized() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return VoiceClient.getServerConfig() != null;
    }

    @Override
    public void run() {
    }
}
