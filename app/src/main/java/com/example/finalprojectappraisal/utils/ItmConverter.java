package com.example.finalprojectappraisal.utils;

/**
 * Converts WGS84 geographic coordinates (latitude/longitude in degrees)
 * to ITM (Israeli Transverse Mercator / New Israeli Grid) coordinates.
 * Result units are metres.  Typical Israeli values:
 *   Easting  ~100 000 – 300 000
 *   Northing ~300 000 – 800 000
 */
public final class ItmConverter {

    // ---- GRS80 ellipsoid ----
    private static final double A  = 6_378_137.0;
    private static final double F  = 1.0 / 298.257_222_101;
    private static final double B  = A * (1.0 - F);
    private static final double E2 = 1.0 - (B * B) / (A * A);   // first eccentricity²
    private static final double EP2 = E2 / (1.0 - E2);           // second eccentricity²

    // ---- ITM projection parameters ----
    private static final double LAT0 = Math.toRadians(31.0 + 44.0 / 60.0 + 3.817  / 3600.0); // 31°44′03.817″ N
    private static final double LON0 = Math.toRadians(35.0 + 12.0 / 60.0 + 16.261 / 3600.0); // 35°12′16.261″ E
    private static final double K0   = 1.000_006_7;
    private static final double E0   = 219_529.584;
    // False Northing for the New Israeli Grid (ITM). The 2,885,516.94 value used by
    // EPSG:2039 applies to a formulation that does NOT subtract the origin meridional
    // arc (M0). Since this implementation subtracts M0, the correct value is 626,907.390.
    private static final double N0   = 626_907.390;

    private static final double M0   = meridionalArc(LAT0);

    private ItmConverter() {}

    /**
     * @param latDeg  WGS84 latitude  in decimal degrees (positive = North)
     * @param lonDeg  WGS84 longitude in decimal degrees (positive = East)
     * @return double[2] { easting, northing } in ITM metres
     */
    public static double[] wgs84ToItm(double latDeg, double lonDeg) {
        double phi    = Math.toRadians(latDeg);
        double lambda = Math.toRadians(lonDeg);

        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double tanPhi = Math.tan(phi);

        double N  = A / Math.sqrt(1.0 - E2 * sinPhi * sinPhi);
        double M  = meridionalArc(phi);

        double T  = tanPhi * tanPhi;
        double C  = EP2 * cosPhi * cosPhi;
        double Av  = cosPhi * (lambda - LON0);
        double A2 = Av * Av;
        double A3 = A2 * Av;
        double A4 = A3 * Av;
        double A5 = A4 * Av;
        double A6 = A5 * Av;

        double easting = E0 + K0 * N * (Av
                + (1.0 - T + C) * A3 / 6.0
                + (5.0 - 18.0 * T + T * T + 72.0 * C - 58.0 * EP2) * A5 / 120.0);

        double northing = N0 + K0 * ((M - M0)
                + N * tanPhi * (A2 / 2.0
                    + (5.0 - T + 9.0 * C + 4.0 * C * C)                          * A4 / 24.0
                    + (61.0 - 58.0 * T + T * T + 600.0 * C - 330.0 * EP2) * A6 / 720.0));

        return new double[]{easting, northing};
    }

    private static double meridionalArc(double phi) {
        double e4 = E2 * E2;
        double e6 = e4 * E2;
        return A * ((1.0 - E2 / 4.0 - 3.0 * e4 / 64.0  - 5.0 * e6 / 256.0)  * phi
                  - (3.0 * E2 / 8.0 + 3.0 * e4 / 32.0  + 45.0 * e6 / 1024.0) * Math.sin(2.0 * phi)
                  + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0)                  * Math.sin(4.0 * phi)
                  - (35.0 * e6 / 3072.0)                                        * Math.sin(6.0 * phi));
    }
}
