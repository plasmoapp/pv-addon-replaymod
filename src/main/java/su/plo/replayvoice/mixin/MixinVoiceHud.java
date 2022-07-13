package su.plo.replayvoice.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.client.gui.VoiceHud;
import su.plo.voice.client.socket.SocketClientUDPQueue;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = VoiceHud.class, remap = false)
public abstract class MixinVoiceHud {
    @Shadow protected abstract void renderPrioritySpeaking();

    @Shadow protected abstract void renderSpeaking();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera instanceof RemotePlayer) {
            RemotePlayer player = (RemotePlayer) camera;

            Boolean isPriorityTalking = SocketClientUDPQueue.talking.get(player.getUUID());
            if (isPriorityTalking != null) {
                if (isPriorityTalking) {
                    renderPrioritySpeaking();
                } else {
                    renderSpeaking();
                }
            }
        }

        ci.cancel();
    }
}
