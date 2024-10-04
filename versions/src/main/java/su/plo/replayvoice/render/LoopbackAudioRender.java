package su.plo.replayvoice.render;

import com.replaymod.render.rendering.VideoRenderer;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.openal.SOFTLoopback;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.device.AlContextOutputDevice;
import su.plo.voice.api.client.connection.ServerInfo;
import su.plo.voice.api.util.AudioUtil;

import java.io.File;
import java.util.Arrays;

@RequiredArgsConstructor
public class LoopbackAudioRender implements AutoCloseable {

    private final @NotNull PlasmoVoiceClient voiceClient;
    private final @NotNull VideoRenderer videoRenderer;

    private FfmpegAudioWriter writer;
    private int frameSize;
    private short[] shortsBuffer;

    private boolean initialized = false;

    public synchronized void render() {
        if (!initialized) {
            initializeWriter();
            return;
        }

        AlContextOutputDevice outputDevice = voiceClient.getDeviceManager()
                .getOutputDevice()
                .orElse(null);
        if (outputDevice == null) return;

        long devicePointer = outputDevice.getDevicePointer();

        outputDevice.runInContextBlocking(() -> {
            SOFTLoopback.alcRenderSamplesSOFT(devicePointer, shortsBuffer, frameSize);
            writer.write(AudioUtil.shortsToBytes(shortsBuffer));
        });
    }

    @Override
    public synchronized void close() throws Exception {
        writer.close();
    }

    private void initializeWriter() {
        ServerInfo serverInfo = voiceClient.getServerInfo().orElse(null);
        if (serverInfo == null) return;

        VoiceAudioRender.reloadDevice();

        File outputVideoFile = videoRenderer.getRenderSettings().getOutputFile();
        File outputFolder = outputVideoFile.getParentFile();
        String[] outputFileNameSplit = outputVideoFile.getName().split("\\.");
        String outputFileName = String.join(".", Arrays.copyOf(outputFileNameSplit, outputFileNameSplit.length - 1)) + "-voice.aac";
        String ffmpegCommand = videoRenderer.getRenderSettings().getExportCommandOrDefault();

        int sampleRate = serverInfo.getVoiceInfo().getCaptureInfo().getSampleRate();
        int fps = videoRenderer.getRenderSettings().getFramesPerSecond();
        this.frameSize = sampleRate / fps;
        int channels = 2;
        this.shortsBuffer = new short[frameSize * channels];

        this.writer = new FfmpegAudioWriter(
                ffmpegCommand,
                new File(outputFolder, outputFileName),
                "aac",
                sampleRate,
                channels
        );
        this.initialized = true;
    }
}
