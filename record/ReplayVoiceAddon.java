package su.plo.replayvoice;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.replaymod.recording.ReplayModRecording;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.SOFTLoopback;
import org.lwjgl.system.MemoryUtil;
import su.plo.replayvoice.audio.AlLoopbackOutputDevice;
import su.plo.replayvoice.network.ClientNetworkHandler;
import su.plo.replayvoice.network.DummyUdpClient;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.device.AlAudioDevice;
import su.plo.voice.api.client.audio.device.DeviceException;
import su.plo.voice.api.client.audio.device.DeviceFactory;
import su.plo.voice.api.client.audio.device.OutputDevice;
import su.plo.voice.api.client.audio.device.source.AlSource;
import su.plo.voice.api.client.audio.source.ClientAudioSource;
import su.plo.voice.api.client.event.VoiceClientInitializedEvent;
import su.plo.voice.api.client.event.audio.capture.AudioCaptureInitializeEvent;
import su.plo.voice.api.client.event.audio.capture.AudioCaptureStartEvent;
import su.plo.voice.api.client.event.audio.device.DeviceClosedEvent;
import su.plo.voice.api.client.event.audio.device.DeviceOpenEvent;
import su.plo.voice.api.client.event.audio.device.source.AlSourceBufferQueuedEvent;
import su.plo.voice.api.client.event.audio.device.source.AlSourceBufferUnqueuedEvent;
import su.plo.voice.api.client.event.audio.source.AudioSourceInitializedEvent;
import su.plo.voice.api.client.event.connection.ConnectionKeyPairGenerateEvent;
import su.plo.voice.api.client.event.connection.UdpClientPacketReceivedEvent;
import su.plo.voice.api.client.event.connection.UdpClientPacketSendEvent;
import su.plo.voice.api.client.event.render.HudActivationRenderEvent;
import su.plo.voice.api.client.event.render.VoiceDistanceRenderEvent;
import su.plo.voice.api.client.event.socket.UdpClientConnectEvent;
import su.plo.voice.api.event.EventCancellable;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.proto.packets.Packet;
import su.plo.voice.proto.packets.udp.clientbound.SelfAudioInfoPacket;
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@Addon(id = "replayvoice", scope = Addon.Scope.CLIENT, version = "1.0.0", authors = "Apehum")
public class ReplayVoiceAddon implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final ResourceLocation SELF_AUDIO_PACKET = new ResourceLocation("plasmo:voice/v2/self_audio");
    public static final ResourceLocation SELF_AUDIO_INFO_PACKET = new ResourceLocation("plasmo:voice/v2/self_audio_info");
    public static final ResourceLocation SOURCE_AUDIO_PACKET = new ResourceLocation("plasmo:voice/v2/source_audio");
    public static final ResourceLocation KEYPAIR_PACKET = new ResourceLocation("plasmo:voice/v2/keypair");

    private final Minecraft minecraft = Minecraft.getInstance();

    private PlasmoVoiceClient voiceClient;

    private AlLoopbackOutputDevice loopback;

    @EventSubscribe
    public void onClientInitialized(@NotNull VoiceClientInitializedEvent event) {
        this.voiceClient = event.getClient();

        ClientNetworkHandler network = new ClientNetworkHandler(voiceClient);

        ClientPlayNetworking.registerGlobalReceiver(SOURCE_AUDIO_PACKET, network::handleSourceAudioPacket);
        ClientPlayNetworking.registerGlobalReceiver(SELF_AUDIO_INFO_PACKET, network::handleSelfAudioInfoPacket);
        ClientPlayNetworking.registerGlobalReceiver(SELF_AUDIO_PACKET, network::handleSelfAudioPacket);
        ClientPlayNetworking.registerGlobalReceiver(KEYPAIR_PACKET, network::handleKeyPairPacket);
    }

    @Override
    public void onInitializeClient() {
    }

    @EventSubscribe
    public void onDeviceOpen(@NotNull DeviceOpenEvent event) {
        if (loopback != null ||
                !(event.getDevice() instanceof OutputDevice) ||
                !(event.getDevice() instanceof AlAudioDevice)
        ) return;

        DeviceFactory deviceFactory = voiceClient.getDeviceFactoryManager().getDeviceFactory("AL_OUTPUT")
                .orElseThrow(() -> new IllegalStateException("Al Output device factory not initialized"));
        List<String> devices = deviceFactory.getDeviceNames();

        OutputDevice<?> device = (OutputDevice<?>) event.getDevice();

        this.loopback = new AlLoopbackOutputDevice(voiceClient, null);
        try {
            loopback.open(device.getFormat().get(), device.getParams().get());
            voiceClient.getDeviceManager().add(loopback);
        } catch (DeviceException e) {
            throw new RuntimeException(e);
        }
    }

    @EventSubscribe
    public void onDeviceClose(@NotNull DeviceClosedEvent event) {
        this.loopback = null;
    }

    @EventSubscribe
    public void onDistanceRender(@NotNull VoiceDistanceRenderEvent event) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor || event.isCancelled()) return;

        Entity camera = Minecraft.getInstance().cameraEntity;
        // todo: check if it's a player who recorded the replay
        if (!(camera instanceof RemotePlayer)) {
            event.setCancelled(true);
        }
    }

    @EventSubscribe
    public void onHudActivationRender(@NotNull HudActivationRenderEvent event) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor || event.isRender()) return;

        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera instanceof RemotePlayer) {
            RemotePlayer player = (RemotePlayer) camera;

            boolean isActivated = voiceClient.getSourceManager().getSelfSourceInfos()
                    .stream()
                    .filter((sourceInfo) ->
                            sourceInfo.getSelfSourceInfo().getPlayerId().equals(player.getUUID()) &&
                                    sourceInfo.getSelfSourceInfo().getActivationId().equals(event.getActivation().getId())
                    )
                    .anyMatch((sourceInfo) ->
                            voiceClient.getSourceManager()
                                    .getSourceById(sourceInfo.getSelfSourceInfo().getSourceInfo().getId())
                                    .map(ClientAudioSource::isActivated)
                                    .orElse(false)
                    );

            event.setRender(isActivated);
        }
    }

    @EventSubscribe
    public void onSourceAudioPacketReceived(@NotNull UdpClientPacketReceivedEvent event) {
        if (!ReplayInterface.INSTANCE.isReplayModActive()) return;

        if (event.getPacket() instanceof SourceAudioPacket) {
            System.out.println("record source");
            record(SOURCE_AUDIO_PACKET, event.getPacket());
        } else if (event.getPacket() instanceof SelfAudioInfoPacket) {
            System.out.println("record self info");
            record(SELF_AUDIO_INFO_PACKET, event.getPacket());
        }
    }

    @EventSubscribe
    public void onUdpPacketSend(@NotNull UdpClientPacketSendEvent event) {
        if (!ReplayInterface.INSTANCE.isReplayModActive() ||
                minecraft.player == null ||
                !(event.getPacket() instanceof PlayerAudioPacket)
        ) return;

        PlayerAudioPacket packet = (PlayerAudioPacket) event.getPacket();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        try {
            packet.write(out);
        } catch (IOException e) {
            LOGGER.error("Failed tto encode packet", e);
            return;
        }

        System.out.println("record self");
        ReplayModRecording.instance.getConnectionEventHandler().getPacketListener().save(
                new ClientboundCustomPayloadPacket(
                        SELF_AUDIO_PACKET,
                        new FriendlyByteBuf(Unpooled.wrappedBuffer(out.toByteArray()))
                )
        );
    }

    @EventSubscribe
    public void onEncryptionInitialize(@NotNull ConnectionKeyPairGenerateEvent event) {
        if (!ReplayInterface.INSTANCE.isReplayModActive()) return;

        KeyPair keyPair = event.getKeyPair();
        byte[] publicKey = keyPair.getPublic().getEncoded();
        byte[] privateKey = keyPair.getPrivate().getEncoded();

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(publicKey.length);
        buf.writeBytes(publicKey);

        buf.writeInt(privateKey.length);
        buf.writeBytes(privateKey);

        ReplayModRecording.instance.getConnectionEventHandler().getPacketListener().save(
                new ClientboundCustomPayloadPacket(KEYPAIR_PACKET, buf)
        );
    }

    @EventSubscribe
    public void onUdpClientConnect(@NotNull UdpClientConnectEvent event) {
        System.out.println(ReplayInterface.INSTANCE.isInReplayEditor);
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        DummyUdpClient udpClient = new DummyUdpClient(voiceClient, event.getConnectionPacket().getSecret());
        voiceClient.getEventBus().register(this, udpClient);

        voiceClient.getUdpClientManager().setClient(udpClient);
        event.setCancelled(true);
    }

    @EventSubscribe
    public void onAudioCaptureInitialize(@NotNull AudioCaptureInitializeEvent event) {
        cancelCaptureEvent(event);
    }

    @EventSubscribe
    public void onAudioCaptureStart(@NotNull AudioCaptureStartEvent event) {
        if (cancelCaptureEvent(event)) return;

        FriendlyByteBuf buf1 = new FriendlyByteBuf(Unpooled.buffer(4));
        buf1.writeInt(2);

        FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer(4));
        buf2.writeInt(1);

        long time2 = System.currentTimeMillis();
        long time1 = time2 - 1L;

//        System.out.println("record");
//        ((TimedPacketListener) ReplayModRecording.instance.getConnectionEventHandler().getPacketListener()).save(
//                time2, new ClientboundCustomPayloadPacket(SOURCE_AUDIO_PACKET, buf2)
//        );
//        ((TimedPacketListener) ReplayModRecording.instance.getConnectionEventHandler().getPacketListener()).save(
//                time1, new ClientboundCustomPayloadPacket(SOURCE_AUDIO_PACKET, buf1)
//        );
    }

    @EventSubscribe
    public void onSourceInitializedEvent(@NotNull AudioSourceInitializedEvent event) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        event.getSource().setCloseTimeoutMs(0L);
        event.getSource().getSourceGroup().ifPresent((sourceGroup) -> {
            sourceGroup.getSources().forEach((source) -> {
                if (source instanceof AlSource) {
                    ((AlSource) source).setCloseTimeoutMs(0L);
                }
            });
        });
    }

    private boolean cancelCaptureEvent(@NotNull EventCancellable event) {
        if (event.isCancelled() || ReplayInterface.INSTANCE.isInReplayEditor) {
            event.setCancelled(true);
            return true;
        }

        return false;
    }

    private void record(@NotNull ResourceLocation resourceLocation,
                        @NotNull Packet<?> packet) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            packet.write(out);

            FriendlyByteBuf buf = new FriendlyByteBuf(
                    Unpooled.wrappedBuffer(out.toByteArray())
            );
            ReplayInterface.INSTANCE.sendFakePacket(resourceLocation, buf);
        } catch (IOException e) {
            LOGGER.warn("Failed to serialize packet: {}", e.getMessage());
        }
    }
}
