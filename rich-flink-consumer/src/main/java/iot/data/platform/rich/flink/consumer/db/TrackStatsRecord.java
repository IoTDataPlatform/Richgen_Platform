package iot.data.platform.rich.flink.consumer.db;

public record TrackStatsRecord(
        long calculatedAtMillis,
        int trackCount,
        double avgHitsPerTrack
) {
}