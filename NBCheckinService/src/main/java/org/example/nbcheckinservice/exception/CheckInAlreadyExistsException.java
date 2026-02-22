package org.example.nbcheckinservice.exception;


public class CheckInAlreadyExistsException extends RuntimeException {

    public CheckInAlreadyExistsException(String message) {
        super(message);
    }

    public CheckInAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
