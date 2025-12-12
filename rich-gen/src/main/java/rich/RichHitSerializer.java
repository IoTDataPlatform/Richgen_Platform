package rich;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;

public class RichHitSerializer implements Serializer<RichHit> {
    @Override
    public byte[] serialize(String s, RichHit hit) {
        ByteBuffer buffer = ByteBuffer.allocate(24);
        buffer.putInt(hit.ix);
        buffer.putInt(hit.iy);
        buffer.putLong(hit.timeSec);
        buffer.putLong(hit.timeNanosec);
        return buffer.array();
    }
}
