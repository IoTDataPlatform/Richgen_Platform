package iot.data.platform.rich.flink.consumer.rich;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TrackDeserializer implements DeserializationSchema<Track> {

    @Override
    public Track deserialize(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float x = buffer.getFloat();
        float y = buffer.getFloat();
        float z = buffer.getFloat();
        float vx = buffer.getFloat();
        float vy = buffer.getFloat();
        float vz = buffer.getFloat();
        long timeSec = buffer.getLong();
        long timeNanosec = buffer.getLong();
        return new Track(x, y, z, vx, vy, vz, timeSec, timeNanosec);
    }

    @Override
    public boolean isEndOfStream(Track track) {
        return false;
    }

    @Override
    public TypeInformation<Track> getProducedType() {
        return TypeInformation.of(Track.class);
    }
}

