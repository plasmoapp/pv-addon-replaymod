package su.plo.replayvoice.mixin;

import com.replaymod.core.KeyBindingRegistry;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.voice.client.VoiceClientFabric;
import su.plo.voice.client.gui.VoiceNotAvailableScreen;
import su.plo.voice.client.gui.VoiceSettingsScreen;

import static su.plo.voice.client.VoiceClient.socketUDP;

@Mixin(value = KeyBindingRegistry.class, remap = false)
public class MixinKeyBindingRegistry {
    private static final Minecraft client = Minecraft.getInstance();

    @Inject(method = "handleRaw", at = @At("HEAD"), cancellable = true)
    private void handleKeyBindings(int keyCode, int action, CallbackInfoReturnable<Boolean> cir) {
        if (VoiceClientFabric.menuKey.consumeClick()) {
            if (!socketUDP.isConnected()) {
                VoiceNotAvailableScreen screen = new VoiceNotAvailableScreen();
                if (socketUDP != null) {
                    if (socketUDP.isTimedOut()) {
                        screen.setConnecting();
                    } else if (!socketUDP.isAuthorized()) {
                        screen.setConnecting();
                    } else {
                        screen.setCannotConnect();
                    }
                }
                client.setScreen(screen);
            } else {
                if (client.screen instanceof VoiceSettingsScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new VoiceSettingsScreen());
                }
            }

            cir.setReturnValue(true);
        }
    }
}
