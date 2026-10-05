package com.hottalk.hottalkserver.util;

import org.locationtech.jts.geom.Envelope;

public class GeoUtils {
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 중심점과 거리(km)를 기준으로 바운딩 박스를 계산합니다.
     *
     * @param latitude   중심 위도
     * @param longitude  중심 경도
     * @param distanceKm 거리(km)
     * @return 바운딩 박스
     */
    public static Envelope calculateBoundingBox(double latitude, double longitude, double distanceKm) {
        double deltaLat = Math.toDegrees(distanceKm / EARTH_RADIUS_KM);
        double deltaLon = Math.toDegrees(distanceKm / (EARTH_RADIUS_KM * Math.cos(Math.toRadians(latitude))));

        double minLat = latitude - deltaLat;
        double maxLat = latitude + deltaLat;
        double minLon = longitude - deltaLon;
        double maxLon = longitude + deltaLon;

        if(minLat<=-90){
            minLat=-89;
        }
        if(maxLat>=90){
            maxLat=89;
        }
        if(minLon<=-180){
            minLon=-179;
        }
        if(maxLon>=180){
            maxLon=179;
        }

        //return new Envelope(minLon, maxLon, minLat, maxLat);
        return new Envelope(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Envelope 객체를 WKT 형식의 POLYGON 문자열로 변환합니다.
     *
     * @param envelope 바운딩 박스
     * @return WKT 형식의 POLYGON 문자열
     */
    public static String envelopeToWKT(Envelope envelope) {
        return String.format("POLYGON((%f %f, %f %f, %f %f, %f %f, %f %f))",
                envelope.getMinX(), envelope.getMinY(),
                envelope.getMinX(), envelope.getMaxY(),
                envelope.getMaxX(), envelope.getMaxY(),
                envelope.getMaxX(), envelope.getMinY(),
                envelope.getMinX(), envelope.getMinY());
    }
}