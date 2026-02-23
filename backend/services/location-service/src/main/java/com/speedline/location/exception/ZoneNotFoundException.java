package com.speedline.location.exception;

/**
 * Exception levée lorsqu'une zone n'est pas trouvée
 */
public class ZoneNotFoundException extends RuntimeException {
    
    public ZoneNotFoundException(Long zoneId) {
        super("Zone avec l'ID " + zoneId + " non trouvée");
    }
    
    public ZoneNotFoundException(String message) {
        super(message);
    }
}
