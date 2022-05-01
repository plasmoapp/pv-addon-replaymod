package su.plo.replayvoice.network;

import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.socket.SocketClientUDPQueue;
import su.plo.voice.client.socket.SocketConnection;
import su.plo.voice.client.sound.AbstractSoundQueue;
import su.plo.voice.common.packets.Packet;

import java.io.IOException;

public class EmptySocketConnection extends Thread implements SocketConnection {
    @Override
    public void close() {
    }

    @Override
    public void send(Packet packet) throws IOException {
    }

    @Override
    public void run() {
        while (true) {
            SocketClientUDPQueue.audioChannels
                    .values()
                    .stream()
                    .filter(AbstractSoundQueue::canKill)
                    .forEach(AbstractSoundQueue::closeAndKill);
            SocketClientUDPQueue.audioChannels.entrySet().removeIf(entry -> entry.getValue().isClosed());

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
                break;
            }
        }
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
}
