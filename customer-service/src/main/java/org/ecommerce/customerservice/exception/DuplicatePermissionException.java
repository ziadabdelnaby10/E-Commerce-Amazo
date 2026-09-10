package org.ecommerce.customerservice.exception;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class DuplicatePermissionException extends RuntimeException {

    public DuplicatePermissionException(String message) {
        super(message);
    }
}

