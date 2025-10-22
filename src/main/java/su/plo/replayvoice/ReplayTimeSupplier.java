package su.plo.replayvoice;

import su.plo.voice.api.client.time.TimeSupplier;
import xyz.breadloaf.replaymodinterface.ReplayInterface;

public class ReplayTimeSupplier implements TimeSupplier {

    @Override
    public long getCurrentTimeMillis() {
        if (ReplayInterface.INSTANCE.replayHandler == null) {
            throw new IllegalStateException("Replay handler is null");
        }

        return ReplayInterface.INSTANCE.replayHandler.getReplaySender().currentTimeStamp();
    }
}
