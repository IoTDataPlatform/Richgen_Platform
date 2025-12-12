package rich;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.lang.Math;

public class RichEventGenerator {
    private static final double SPEED_OF_LIGHT = 299792458; // m/s
    private static final double COS_PI_OVER_4 = 1 / Math.sqrt(2);
    private final Random rng = new Random();
    private final int pdSize; // photodetector pixel array size along one axis
    private final int pdHalfSize; // photodetector half pixel array size along one axis
    private final float pdPitch; // photodetector pixel pitch size in mm
    final double dcrTotalRate; // dark count rate for the photodetector, Hz
    private final float npe0; // maximum mean number of phototelectrons
    private final float ri; // radiator refractive index
    private final float distance; // proximity distance in mm
    private final float angRes; // angular resolution in mrad
    private final float timeResNs; // photodetector time resolution in nanoseconds
    private final float eventRate; // event rate in s^-1

    public RichEventGenerator(int pdSize,
                              float pdPitch,
                              float dcrTotalRate,
                              float npe0,
                              float ri,
                              float distance,
                              float angRes,
                              float timeResNs,
                              float eventRate) {
        this.pdSize = pdSize;
        this.pdHalfSize = pdSize / 2;
        this.pdPitch = pdPitch;
        this.dcrTotalRate = dcrTotalRate;
        this.npe0 = npe0;
        this.ri = ri;
        this.distance = distance;
        this.angRes = angRes;
        this.timeResNs = timeResNs;
        this.eventRate = eventRate;

        rng.setSeed(System.currentTimeMillis());
    }

    public ArrayList<Track> generateTracks(Instant startTime, Instant endTime) {
        Instant currentTime = startTime;

        ArrayList<Track> tracks = new ArrayList<>();

        while (true) {
            // Generate next Cherenkov event period
            long nextPeriodNanos = (long) (-1e9 / eventRate * Math.log(1 - rng.nextDouble()));

            // Update the current time
            currentTime = currentTime.plus(nextPeriodNanos, ChronoUnit.NANOS);

            // Check if the time window is expired
            if (currentTime.equals(endTime) || currentTime.isAfter(endTime))
                break;

            // Generate particle's position
            float xp = (float) (pdHalfSize * pdPitch * (rng.nextDouble() - 0.5));
            float yp = (float) (pdHalfSize * pdPitch * (rng.nextDouble() - 0.5));
            // Generate particle's angles
            double thetap = Math.acos(1 - rng.nextDouble() * (1 - COS_PI_OVER_4));
            double phip = 2 * Math.PI * rng.nextDouble();
            // Evaluate cosines and sines of particle's angles
            double cosThetap = Math.cos(thetap), sinThetap = Math.sin(thetap);
            double cosPhip = Math.cos(phip), sinPhip = Math.sin(phip);

            tracks.add(new Track(
                    xp,
                    yp,
                    0F,
                    (float) (sinThetap * cosPhip),
                    (float) (sinThetap * sinPhip),
                    (float) cosThetap,
                    currentTime.getEpochSecond(),
                    currentTime.getNano()));
        }

        Collections.shuffle(tracks);

        return tracks;
    }

    public ArrayList<RichHit> generateHits(Instant startTime, Instant endTime, List<Track> tracks) {
        Instant currentTime = startTime;

        ArrayList<RichHit> hitCollection = new ArrayList<>();

        // Generate dark counts
        while (true) {
            long nextPeriodNanos = (long) (-1e9 / dcrTotalRate * Math.log(1 - rng.nextDouble()));

            // Update the current time
            currentTime = currentTime.plus(nextPeriodNanos, ChronoUnit.NANOS);

            // Check if the time window is expired
            if (currentTime.equals(endTime) || currentTime.isAfter(endTime))
                break;

            // Dark count generation
            int ix = rng.nextInt(pdSize), iy = rng.nextInt(pdSize);
            hitCollection.add(new RichHit(ix, iy, currentTime.getEpochSecond(), currentTime.getNano()));
        }

        // Generate Cherenkov events
        for (Track t : tracks) {
            double cosThetap = t.vz, sinThetap = Math.sqrt(1 - t.vz * t.vz);
            double cosPhip = sinThetap == 0 ? 1. : t.vx / sinThetap, sinPhip = sinThetap == 0 ? 0. : t.vy / sinThetap;
            // Generate particle's velocity ad hoc
            double beta = 1 - 0.02 * rng.nextDouble();
            double riBeta = ri * beta;
            // Evaluate mean number of photoelectrons
            double meanNpe = npe0 * Math.max(0., (1. - 1/(riBeta*riBeta)) / (1. - 1/(ri*ri))) / t.vz;
            // Evaluate mean Cherenkov angle
            double meanThetac = Math.acos(Math.min(1., 1/riBeta));

            // Cherenkov hits generation
            // Possion number generation by Knuth's algorithm
            double L = Math.exp(-meanNpe);
            double p = 1.0;
            int k = 0;
            do {
                // Generate hit angles
                double thetac = Math.max(0., meanThetac + rng.nextGaussian() * angRes * 1e-3);
                double phic = 2 * Math.PI * rng.nextDouble();
                double ux = Math.sin(thetac) * Math.cos(phic);
                double uy = Math.sin(thetac) * Math.sin(phic);
                double uz = Math.cos(thetac);
                double vx = cosThetap * cosPhip * ux - sinPhip * uy + sinThetap * cosPhip * uz;
                double vy = cosThetap * sinPhip * ux + cosPhip * uy + sinThetap * sinPhip * uz;
                double vz = -sinThetap * ux + cosThetap * uz;

                if (vz <= 0.) continue;

                double x = t.x + vx / vz * distance, y = t.y + vy / vz * distance;

                // if photon hit outside the PD, skip it
                if (Math.abs(x) >= pdHalfSize * pdPitch || Math.abs(y) >= pdHalfSize * pdPitch)
                    continue;
                // Transform to pixel index
                int ix = (int) Math.floor(x / pdPitch + pdHalfSize);
                int iy = (int) Math.floor(y / pdPitch + pdHalfSize);
                // second check if the photon hit is inside the PD
                if (ix < 0 || ix >= pdSize || iy < 0 || iy >= pdSize)
                    continue;

                long hitTimeNanosec = t.timeNanosec +
                        (long) (1e6 * distance / vz / SPEED_OF_LIGHT +
                                (10 + rng.nextGaussian()) * timeResNs);
                long hitTimeSec = t.timeSec + hitTimeNanosec / 1_000_000_000L;
                hitTimeNanosec %= 1_000_000_000L;

                hitCollection.add(new RichHit(ix, iy, hitTimeSec, hitTimeNanosec));

                p *= rng.nextDouble();
                k++;
            } while (p > L && k < 1000);
        }

        Collections.shuffle(hitCollection);

        return hitCollection;
    }

}
