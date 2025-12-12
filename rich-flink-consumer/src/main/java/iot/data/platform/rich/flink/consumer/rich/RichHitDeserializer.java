package iot.data.platform.rich.flink.consumer.rich;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RichHitDeserializer implements DeserializationSchema<RichHit> {

    @Override
    public RichHit deserialize(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 20) { // Проверка на минимальную длину (2 int + 2 long = 20 байтов)
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int ix = buffer.getInt();
        int iy = buffer.getInt();
        long timeSec = buffer.getLong();
        long timeNanosec = buffer.getLong();
        return new RichHit(ix, iy, timeSec, timeNanosec);
    }

    @Override
    public boolean isEndOfStream(RichHit nextElement) {
        return false; // Сообщаем, что поток не заканчивается
    }

    @Override
    public TypeInformation<RichHit> getProducedType() {
        return TypeInformation.of(RichHit.class);
    }
}

