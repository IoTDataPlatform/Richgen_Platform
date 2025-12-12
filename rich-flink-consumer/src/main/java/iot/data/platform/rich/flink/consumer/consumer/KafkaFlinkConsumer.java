package iot.data.platform.rich.flink.consumer.consumer;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iot.data.platform.rich.flink.consumer.rich.RichHit;
import iot.data.platform.rich.flink.consumer.rich.RichHitDeserializer;
import iot.data.platform.rich.flink.consumer.rich.Track;
import iot.data.platform.rich.flink.consumer.rich.TrackDeserializer;
import iot.data.platform.rich.flink.consumer.web.WebSocketSink;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class KafkaFlinkConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaFlinkConsumer.class);

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        String bootstrap = System.getenv().getOrDefault(
                "KAFKA_BOOTSTRAP_SERVERS",
                "kafka:9092"
        );

        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", bootstrap);
        properties.setProperty("group.id", "rich-flink-consumer");

        FlinkKafkaConsumer<RichHit> hitConsumer = new FlinkKafkaConsumer<>(
                "HitStream",
                new RichHitDeserializer(),
                properties
        );
        DataStream<RichHit> hitDataStream = env
                .addSource(hitConsumer)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<RichHit>forBoundedOutOfOrderness(Duration.ofSeconds(1000000))
                                .withTimestampAssigner((hit, ts) -> convertToNanoseconds(hit.timeSec, hit.timeNanosec))
                );

        FlinkKafkaConsumer<Track> trackConsumer = new FlinkKafkaConsumer<>(
                "TrackStream",
                new TrackDeserializer(),
                properties
        );
        DataStream<Track> trackDataStream = env
                .addSource(trackConsumer)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<Track>forBoundedOutOfOrderness(Duration.ofSeconds(1000000))
                                .withTimestampAssigner((track, ts) -> convertToNanoseconds(track.timeSec, track.timeNanosec))
                );

        DataStream<Tuple2<Track, RichHit>> joinedStream = trackDataStream
                .keyBy(track -> 1)
                .intervalJoin(hitDataStream.keyBy(hit -> 1))
                .between(Time.milliseconds(-70000), Time.milliseconds(70000))
                .process(new ProcessJoinFunction<>() {
                    @Override
                    public void processElement(Track track,
                                               RichHit hit,
                                               Context ctx,
                                               Collector<Tuple2<Track, RichHit>> out) {
                        out.collect(Tuple2.of(track, hit));
                    }
                });

        DataStream<RichHit> signalHitsStream = joinedStream
                .map((MapFunction<Tuple2<Track, RichHit>, RichHit>) value -> {
                    value.f1.isSignal = true;
                    return value.f1;
                });

        DataStream<RichHit> finalHitStream = hitDataStream
                .keyBy(hit -> hit.ix + "_" + hit.iy + "_" + hit.timeSec + "_" + hit.timeNanosec)
                .connect(signalHitsStream.keyBy(hit -> hit.ix + "_" + hit.iy + "_" + hit.timeSec + "_" + hit.timeNanosec))
                .process(new DeduplicateRichHitsFunction());

        finalHitStream.addSink(new WebSocketSink());

        DataStream<Tuple2<Track, List<RichHit>>> trackWithHitsStream = joinedStream
                .keyBy(value -> value.f0.toString())
                .timeWindow(Time.seconds(1_000))
                .process(new ProcessWindowFunction<>() {
                    @Override
                    public void process(String key,
                                        Context context,
                                        Iterable<Tuple2<Track, RichHit>> elements,
                                        Collector<Tuple2<Track, List<RichHit>>> out) {
                        Track track = null;
                        List<RichHit> hits = new ArrayList<>();

                        for (Tuple2<Track, RichHit> element : elements) {
                            track = element.f0;
                            hits.add(element.f1);
                        }

                        if (track != null) {
                            out.collect(Tuple2.of(track, hits));
                        }
                    }
                });

        DataStream<Tuple2<Integer, Double>> trackStatsStream = trackWithHitsStream
                .timeWindowAll(Time.seconds(1_000_000))
                .aggregate(new AggregateFunction<Tuple2<Track, List<RichHit>>, Tuple2<Integer, Integer>, Tuple2<Integer, Double>>() {
                    @Override
                    public Tuple2<Integer, Integer> createAccumulator() {
                        return Tuple2.of(0, 0);
                    }

                    @Override
                    public Tuple2<Integer, Integer> add(Tuple2<Track, List<RichHit>> value,
                                                        Tuple2<Integer, Integer> acc) {
                        int trackCount = acc.f0 + 1;
                        int totalHits = acc.f1 + value.f1.size();
                        return Tuple2.of(trackCount, totalHits);
                    }

                    @Override
                    public Tuple2<Integer, Double> getResult(Tuple2<Integer, Integer> acc) {
                        int trackCount = acc.f0;
                        int totalHits = acc.f1;
                        double avgHitsPerTrack = trackCount == 0 ? 0.0 : (double) totalHits / trackCount;
                        return Tuple2.of(trackCount, avgHitsPerTrack);
                    }

                    @Override
                    public Tuple2<Integer, Integer> merge(Tuple2<Integer, Integer> a,
                                                          Tuple2<Integer, Integer> b) {
                        return Tuple2.of(a.f0 + b.f0, a.f1 + b.f1);
                    }
                });

        trackStatsStream
                .map(stats -> "Tracks: " + stats.f0 + " | Avg hits per track: " + stats.f1)
                .print();

        env.execute("Rich Kafka Flink Consumer");
    }

    private static long convertToNanoseconds(long timeSec, long timeNanosec) {
        return (timeSec * 1_000_000_000L) + timeNanosec;
    }
}
