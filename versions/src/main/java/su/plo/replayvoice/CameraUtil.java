package su.plo.replayvoice;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;

public final class CameraUtil {

    public static boolean isReplayRecorder() {
        Entity camera = Minecraft.getInstance().cameraEntity;
        return camera instanceof RemotePlayer;
    }

    private CameraUtil() {
    }
}
