package com.speedline.location.exception;

/**
 * Exception levée lorsqu'une zone chevauche une autre zone existante
 */
public class ZoneOverlapException extends RuntimeException {
    
    private final Long overlappingZoneId;
    private final String overlappingZoneName;
    
    public ZoneOverlapException(Long overlappingZoneId, String overlappingZoneName) {
        super("La zone chevauche la zone existante : " + overlappingZoneName + " (ID: " + overlappingZoneId + ")");
        this.overlappingZoneId = overlappingZoneId;
        this.overlappingZoneName = overlappingZoneName;
    }
    
    public ZoneOverlapException(String message) {
        super(message);
        this.overlappingZoneId = null;
        this.overlappingZoneName = null;
    }
    
    public Long getOverlappingZoneId() {
        return overlappingZoneId;
    }
    
    public String getOverlappingZoneName() {
        return overlappingZoneName;
    }
}
