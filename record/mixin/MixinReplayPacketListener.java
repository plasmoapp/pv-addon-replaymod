package su.plo.replayvoice.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.lib.com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.replaymod.recording.packet.PacketListener;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayOutputStream;
import com.replaymod.replaystudio.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import su.plo.replayvoice.replay.TimedPacketListener;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

@Mixin(value = PacketListener.class, remap = false)
public abstract class MixinReplayPacketListener implements TimedPacketListener {

    @Shadow protected abstract ConnectionProtocol getConnectionState();

    @Shadow @Final private static Logger logger;

    @Shadow private volatile boolean serverWasPaused;

    @Shadow private long timePassedWhilePaused;

    @Shadow @Final private long startTime;

    @Shadow private long lastSentPacket;

    @Shadow @Final private ExecutorService saveService;

    @Shadow @Final private ReplayOutputStream packetOutputStream;

    @Override
    public void save(long time, Packet<?> mcPacket) {
        com.replaymod.replaystudio.protocol.Packet packet;
        try {
            packet = ReplayPacketListenerAccessor.invokeEncodeMcPacket(this.getConnectionState(), mcPacket);
        } catch (Exception var4) {
            logger.error("Encoding packet:", var4);
            return;
        }

        try {
            if (this.serverWasPaused) {
                this.timePassedWhilePaused = time - this.startTime - this.lastSentPacket;
                this.serverWasPaused = false;
            }

            int timestamp = (int)(time - this.startTime - this.timePassedWhilePaused);
//            this.lastSentPacket = (long)timestamp;
            PacketData packetData = new PacketData((long)timestamp, packet);
            this.saveService.submit(() -> {
                try {
                    if (ReplayMod.isMinimalMode()) {
                        ByteBuf packetIdBuf = PooledByteBufAllocator.DEFAULT.buffer();
                        ByteBuf packetBuf = packetData.getPacket().getBuf();

                        try {
                            (new ByteBufNetOutput(packetIdBuf)).writeVarInt(packetData.getPacket().getId());
                            int packetIdLen = packetIdBuf.readableBytes();
                            int packetBufLen = packetBuf.readableBytes();
                            Utils.writeInt(this.packetOutputStream, (int)packetData.getTime());
                            Utils.writeInt(this.packetOutputStream, packetIdLen + packetBufLen);
                            packetIdBuf.readBytes(this.packetOutputStream, packetIdLen);
                            packetBuf.getBytes(packetBuf.readerIndex(), this.packetOutputStream, packetBufLen);
                        } finally {
                            packetIdBuf.release();
                            packetBuf.release();
                        }
                    } else {
                        this.packetOutputStream.write(packetData);
                    }

                } catch (IOException var10) {
                    throw new RuntimeException(var10);
                }
            });
        } catch (Exception var6) {
            logger.error("Writing packet:", var6);
        }
    }
}
