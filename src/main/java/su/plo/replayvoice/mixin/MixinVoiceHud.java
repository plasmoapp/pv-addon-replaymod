package su.plo.replayvoice.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.gui.VoiceHud;
import su.plo.voice.client.socket.SocketClientUDPQueue;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = VoiceHud.class, remap = false)
public class MixinVoiceHud {
    @Shadow
    @Final
    private Minecraft client;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(CallbackInfo ci) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        Entity camera = Minecraft.getInstance().cameraEntity;
        if (camera instanceof RemotePlayer) {
            RemotePlayer player = (RemotePlayer) camera;
            final Gui inGameHud = client.gui;
            final PoseStack matrixStack = new PoseStack();

            Boolean isPriorityTalking = SocketClientUDPQueue.talking.get(player.getUUID());
            if (isPriorityTalking != null) {
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.client.getTextureManager().bind(VoiceClient.ICONS);

                if (isPriorityTalking) {
                    inGameHud.blit(
                            matrixStack,
                            VoiceClient.getClientConfig().micIconPosition.get().getX(client),
                            VoiceClient.getClientConfig().micIconPosition.get().getY(client),
                            16,
                            16,
                            16,
                            16
                    );
                } else {
                    inGameHud.blit(
                            matrixStack,
                            VoiceClient.getClientConfig().micIconPosition.get().getX(client),
                            VoiceClient.getClientConfig().micIconPosition.get().getY(client),
                            0,
                            0,
                            16,
                            16
                    );
                }
            }
        }

        ci.cancel();
    }
}
