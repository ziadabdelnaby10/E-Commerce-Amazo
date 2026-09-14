package org.ecommerce.customerservice.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MaxAttemptsException extends RuntimeException {
    public MaxAttemptsException(String message) {
        super(message);
    }
}
