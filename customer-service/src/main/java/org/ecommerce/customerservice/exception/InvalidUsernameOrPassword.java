package org.ecommerce.customerservice.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InvalidUsernameOrPassword extends RuntimeException {
    public InvalidUsernameOrPassword(String message) {
        super(message);
    }
}
