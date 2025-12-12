package rich;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;

public class TrackSerializer implements Serializer<Track> {
    @Override
    public byte[] serialize(String s, Track track) {
        ByteBuffer buffer = ByteBuffer.allocate(40);
        buffer.putFloat(track.x);
        buffer.putFloat(track.y);
        buffer.putFloat(track.z);
        buffer.putFloat(track.vx);
        buffer.putFloat(track.vy);
        buffer.putFloat(track.vz);
        buffer.putLong(track.timeSec);
        buffer.putLong(track.timeNanosec);
        return buffer.array();
    }
}
