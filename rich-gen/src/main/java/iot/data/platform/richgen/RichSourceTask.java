package iot.data.platform.richgen;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import rich.*;

import java.time.Instant;
import java.util.*;
import static iot.data.platform.richgen.RichSourceConnector.*;

public class RichSourceTask extends SourceTask {

    private static final long TIME_STEP_MS = 200L;      // шаг окна
    private static final int PD_SIZE = 240;
    private static final float PD_PITCH = 3F;
    private static final float DISTANCE = 200F;
    private static final float DEFAULT_EVENT_RATE = 10F;
    private static final float DEFAULT_DCR_TOTAL_RATE = 2000F;

    private String name;
    private String trackTopic;
    private String hitTopic;

    private RichEventGenerator generator;
    private Instant windowStart;

    private final RichHitSerializer hitSerializer = new RichHitSerializer();
    private final TrackSerializer trackSerializer = new TrackSerializer();

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.name = props.getOrDefault(CFG_NAME, "rich-simulator");
        this.trackTopic = Objects.requireNonNull(props.get(CFG_TRACK_TOPIC), "track.topic is required");
        this.hitTopic = Objects.requireNonNull(props.get(CFG_HIT_TOPIC), "hit.topic is required");

        this.generator = new RichEventGenerator(
                PD_SIZE,
                PD_PITCH,
                DEFAULT_DCR_TOTAL_RATE,
                50F,
                1.05F,
                DISTANCE,
                10F,
                1000F,
                DEFAULT_EVENT_RATE
        );

        this.windowStart = Instant.now();

        try {
            Map<String, ?> off = context.offsetStorageReader().offset(Map.of("source", name));
            if (off != null) {
                Object ts = off.get("lastWindowEndMs");
                if (ts instanceof Number) {
                    long ms = ((Number) ts).longValue();
                    this.windowStart = Instant.ofEpochMilli(ms);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        Thread.sleep(TIME_STEP_MS);

        Instant startTime = windowStart;
        Instant endTime = windowStart.plusMillis(TIME_STEP_MS);
        windowStart = endTime;

        List<Track> tracks = generator.generateTracks(startTime, endTime);
        List<RichHit> hits = generator.generateHits(startTime, endTime, tracks);

        Map<String, String> sourcePartition = Map.of("source", name);
        Map<String, Object> sourceOffset = Map.of("lastWindowEndMs", endTime.toEpochMilli());

        List<SourceRecord> records = new ArrayList<>(tracks.size() + hits.size());

        for (Track t : tracks) {
            byte[] valueBytes = trackSerializer.serialize(trackTopic, t);

            records.add(new SourceRecord(
                    sourcePartition,
                    sourceOffset,
                    trackTopic,
                    null,
                    null,
                    null,
                    Schema.BYTES_SCHEMA,
                    valueBytes
            ));
        }

        for (RichHit h : hits) {
            byte[] valueBytes = hitSerializer.serialize(hitTopic, h);

            records.add(new SourceRecord(
                    sourcePartition,
                    sourceOffset,
                    hitTopic,
                    null,
                    null,
                    null,
                    Schema.BYTES_SCHEMA,
                    valueBytes
            ));
        }

        return records;
    }

    @Override
    public void stop() {
    }
}
