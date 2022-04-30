package su.plo.replayvoice.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.voice.client.VoiceClient;
import su.plo.voice.client.render.EntityIconRenderer;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

@Mixin(value = EntityIconRenderer.class, remap = false)
public class MixinEntityIconRenderer {
    @Shadow
    @Final
    private Minecraft client;

    @Inject(method = "isIconHidden", at = @At("HEAD"), cancellable = true)
    public void isIconHidden(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!ReplayInterface.INSTANCE.isInReplayEditor) return;

        if (VoiceClient.getClientConfig().showIcons.get() == 2) {
            cir.setReturnValue(true);
            return;
        }

        if (!client.player.connection.getOnlinePlayerIds().contains(player.getUUID())) {
            cir.setReturnValue(true);
            return;
        }

        if (player.isInvisibleTo(client.player) ||
                (client.options.hideGui && VoiceClient.getClientConfig().showIcons.get() == 0)) {
            cir.setReturnValue(true);
            return;
        }

        cir.setReturnValue(false);
    }
}
