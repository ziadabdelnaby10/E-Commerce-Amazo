package org.ecommerce.inventoryservice.lock;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raised when a stock lock could not be obtained within the acquisition timeout.
 *
 * <p>Mapped to 409 CONFLICT rather than 500: the request is not invalid, it simply collided with
 * another in-flight reservation for the same product and is safe to retry.</p>
 */
public class LockAcquisitionException extends ResponseStatusException {

    public LockAcquisitionException(String key) {
        super(HttpStatus.CONFLICT, "Could not acquire inventory lock for " + key + "; please retry");
    }
}
