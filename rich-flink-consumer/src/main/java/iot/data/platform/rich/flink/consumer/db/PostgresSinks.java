package iot.data.platform.rich.flink.consumer.db;

import iot.data.platform.rich.flink.consumer.rich.RichHit;
import iot.data.platform.rich.flink.consumer.rich.Track;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Timestamp;
import java.time.Instant;

public final class PostgresSinks {

    private PostgresSinks() {
    }

    public static SinkFunction<Track> createTrackSink() {
        return JdbcSink.sink(
                """
                INSERT INTO tracks (
                    event_time,
                    time_sec,
                    time_nanosec,
                    x,
                    y,
                    z,
                    vx,
                    vy,
                    vz
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (time_sec, time_nanosec, x, y, z, vx, vy, vz)
                DO NOTHING
                """,
                (ps, track) -> {
                    ps.setTimestamp(1, toTimestamp(track.timeSec, track.timeNanosec));
                    ps.setLong(2, track.timeSec);
                    ps.setLong(3, track.timeNanosec);
                    ps.setFloat(4, track.x);
                    ps.setFloat(5, track.y);
                    ps.setFloat(6, track.z);
                    ps.setFloat(7, track.vx);
                    ps.setFloat(8, track.vy);
                    ps.setFloat(9, track.vz);
                },
                jdbcExecutionOptions(),
                jdbcConnectionOptions()
        );
    }

    public static SinkFunction<RichHit> createRichHitSink() {
        return JdbcSink.sink(
                """
                INSERT INTO rich_hits (
                    event_time,
                    time_sec,
                    time_nanosec,
                    ix,
                    iy,
                    is_signal
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (time_sec, time_nanosec, ix, iy)
                DO UPDATE SET is_signal = EXCLUDED.is_signal
                """,
                (ps, hit) -> {
                    ps.setTimestamp(1, toTimestamp(hit.timeSec, hit.timeNanosec));
                    ps.setLong(2, hit.timeSec);
                    ps.setLong(3, hit.timeNanosec);
                    ps.setInt(4, hit.ix);
                    ps.setInt(5, hit.iy);
                    ps.setBoolean(6, hit.isSignal);
                },
                jdbcExecutionOptions(),
                jdbcConnectionOptions()
        );
    }


    public static SinkFunction<TrackStatsRecord> createTrackStatsSink() {
        return JdbcSink.sink(
                """
                INSERT INTO track_stats (
                    calculated_at,
                    track_count,
                    avg_hits_per_track
                )
                VALUES (?, ?, ?)
                """,
                (ps, stats) -> {
                    ps.setTimestamp(1, new Timestamp(stats.calculatedAtMillis()));
                    ps.setInt(2, stats.trackCount());
                    ps.setDouble(3, stats.avgHitsPerTrack());
                },
                jdbcExecutionOptions(),
                jdbcConnectionOptions()
        );
    }

    private static JdbcConnectionOptions jdbcConnectionOptions() {
        String url = System.getenv().getOrDefault(
                "POSTGRES_JDBC_URL",
                "jdbc:postgresql://postgres:5432/richdb"
        );
        String user = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
        String password = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");

        return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(url)
                .withDriverName("org.postgresql.Driver")
                .withUsername(user)
                .withPassword(password)
                .build();
    }

    private static JdbcExecutionOptions jdbcExecutionOptions() {
        return JdbcExecutionOptions.builder()
                .withBatchSize(100)
                .withBatchIntervalMs(1000)
                .withMaxRetries(3)
                .build();
    }

    private static Timestamp toTimestamp(long timeSec, long timeNanosec) {
        return Timestamp.from(Instant.ofEpochSecond(timeSec, timeNanosec));
    }
}