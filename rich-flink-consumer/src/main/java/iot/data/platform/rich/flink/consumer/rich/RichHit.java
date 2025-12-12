package iot.data.platform.rich.flink.consumer.rich;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RichHit {
    public final int ix; // x position of pixel
    public final int iy; // y position of pixel
    public final long timeSec; // seconds since epoch
    public final long timeNanosec; // nanoseconds of a second
    public boolean isSignal;

    public RichHit(int ix, int iy, long timeSec, long timeNanosec) {
        this.ix = ix;
        this.iy = iy;
        this.timeSec = timeSec;
        this.timeNanosec = timeNanosec;
        this.isSignal = false;
    }

    @JsonCreator
    public RichHit(
            @JsonProperty("ix") int ix,
            @JsonProperty("iy") int iy,
            @JsonProperty("timeSec") long timeSec,
            @JsonProperty("timeNanosec") long timeNanosec,
            @JsonProperty("isSignal") boolean isSignal
    ) {
        this.ix = ix;
        this.iy = iy;
        this.timeSec = timeSec;
        this.timeNanosec = timeNanosec;
        this.isSignal = isSignal;
    }

    @Override
    public String toString() {
        return "RichHit{" +
                "ix=" + ix +
                ", iy=" + iy +
                ", timeSec=" + timeSec +
                ", timeNanosec=" + timeNanosec +
                ", isSignal=" + isSignal +
                '}';
    }
}

