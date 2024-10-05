package su.plo.replayvoice.render;

import com.replaymod.render.rendering.VideoRenderer;
import org.jetbrains.annotations.NotNull;
import su.plo.replayvoice.ReplayVoiceAddon;
import su.plo.voice.api.client.audio.device.DeviceException;
import su.plo.voice.api.client.audio.device.DeviceManager;

public final class VoiceAudioRender {

    public static LoopbackAudioRender AUDIO_RENDER;

    public static boolean isRendering() {
        return AUDIO_RENDER != null;
    }

    public static void startRender(@NotNull VideoRenderer renderer) {
        if (AUDIO_RENDER != null) return;

        AUDIO_RENDER = new LoopbackAudioRender(ReplayVoiceAddon.INSTANCE.voiceClient, renderer);
    }

    public static void stopRender(@NotNull VideoRenderer videoRenderer) {
        if (AUDIO_RENDER == null) return;

        try {
            AUDIO_RENDER.close();
        } catch (Exception e) {
            ReplayVoiceAddon.LOGGER.error("Failed to close audio renderer", e);
        }
        AUDIO_RENDER = null;

        reloadDevice();
    }

    public static void reloadDevice() {
        DeviceManager devices = ReplayVoiceAddon.INSTANCE.voiceClient.getDeviceManager();

        devices.getOutputDevice().ifPresent((device) -> {
            try {
                device.reload();
            } catch (DeviceException e) {
                ReplayVoiceAddon.LOGGER.error("Failed to reload output device", e);
            }
        });
    }
}
