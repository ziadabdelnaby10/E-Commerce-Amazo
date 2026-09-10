package org.ecommerce.customerservice.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class DefaultRoleMissingException extends RuntimeException {

    public DefaultRoleMissingException(String message) {
        super(message);
    }
}

