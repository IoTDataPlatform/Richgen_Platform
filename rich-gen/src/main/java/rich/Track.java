package rich;

public class Track {
    public final float x; // x position of the track in radiator
    public final float y; // y position of the track in radiator
    public final float z; // z position of the track in radiator
    public final float vx; // direction unit vector x component
    public final float vy; // direction unit vector y component
    public final float vz; // direction unit vector z component
    public final long timeSec; // seconds since epoch
    public final long timeNanosec; // nanoseconds of a second

    public Track(float x, float y, float z, float vx, float vy, float vz, long timeSec, long timeNanosec) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.timeSec = timeSec;
        this.timeNanosec = timeNanosec;
    }

    @Override
    public String toString() {
        return "Track{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", vx=" + vx +
                ", vy=" + vy +
                ", vz=" + vz +
                ", timeSec=" + timeSec +
                ", timeNanosec=" + timeNanosec +
                '}';
    }

}
