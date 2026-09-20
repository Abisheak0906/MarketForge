package com.bajrix.marketplace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateListingException extends RuntimeException {
    public DuplicateListingException(String message) {
        super(message);
    }
}
