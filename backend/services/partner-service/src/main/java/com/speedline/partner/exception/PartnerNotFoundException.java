package com.speedline.partner.exception;

public class PartnerNotFoundException extends RuntimeException {

    public PartnerNotFoundException(Long id) {
        super("Partner not found with id: " + id);
    }

    public PartnerNotFoundException(String message) {
        super(message);
    }
}
