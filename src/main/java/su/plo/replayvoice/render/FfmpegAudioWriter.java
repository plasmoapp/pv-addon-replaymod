package su.plo.replayvoice.render;

import com.replaymod.lib.org.apache.commons.exec.CommandLine;
import org.jetbrains.annotations.NotNull;
import su.plo.replayvoice.ReplayVoiceAddon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class FfmpegAudioWriter implements AutoCloseable {

    private final @NotNull Process process;
    private final @NotNull InputStream inputStream;
    private final @NotNull OutputStream outputStream;

    public FfmpegAudioWriter(
            @NotNull String ffmpegCommand,
            @NotNull File outputFile,
            @NotNull String outputFormat,
            int sampleRate,
            int channels
    ) {
        List<String> commandArguments = new ArrayList<>();
        commandArguments.add("-y");
        commandArguments.add("-f s16le");
        commandArguments.add("-ar " + sampleRate);
        commandArguments.add("-ac " + channels);
        commandArguments.add("-i -");
        commandArguments.add("-c:a " + outputFormat);
        commandArguments.add(outputFile.getName());

        String[] commandLine = (new CommandLine(ffmpegCommand))
                .addArguments(String.join(" ", commandArguments), false)
                .toStrings();

        ReplayVoiceAddon.LOGGER.info("Starting ffmpeg process: {}", String.join(" ", commandLine));

        try {
            process = (new ProcessBuilder(commandLine))
                    .directory(outputFile.getParentFile())
                    .redirectErrorStream(true)
                    .start();

            inputStream = process.getInputStream();
            outputStream = process.getOutputStream();
        } catch (IOException e) {
            ReplayVoiceAddon.LOGGER.info("Failed to create ffmpeg process", e);
            throw new RuntimeException(e);
        }
    }

    public void write(byte[] samples) {
        try {
            outputStream.write(samples);

            byte[] available = new byte[inputStream.available()];
            inputStream.read(available);
        } catch (IOException e) {
            ReplayVoiceAddon.LOGGER.info("Failed to write to ffmpeg stdin", e);
        }
    }

    @Override
    public void close() throws Exception {
        try {
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            process.waitFor();
        } catch (InterruptedException | IOException e) {
            ReplayVoiceAddon.LOGGER.info("Failed to exit ffmpeg process", e);
        }
        process.destroy();
    }
}
