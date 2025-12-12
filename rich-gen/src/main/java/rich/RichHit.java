package rich;

public class RichHit {
    public final int ix; // x position of pixel
    public final int iy; // y position of pixel
    public final long timeSec; // seconds since epoch
    public final long timeNanosec; // nanoseconds of a second

    public RichHit(int ix, int iy, long timeSec, long timeNanosec) {
        this.ix = ix;
        this.iy = iy;
        this.timeSec = timeSec;
        this.timeNanosec = timeNanosec;
    }

    @Override
    public String toString() {
        return "RichHit{" +
                "ix=" + ix +
                ", iy=" + iy +
                ", time_seconds=" + timeSec +
                ", time_nanoseconds=" + timeNanosec +
                '}';
    }
}
