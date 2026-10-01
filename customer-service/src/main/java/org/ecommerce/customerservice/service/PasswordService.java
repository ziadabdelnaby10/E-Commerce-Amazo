package org.ecommerce.customerservice.service;

/**
 * Password hashing and verification contract.
 */
public interface PasswordService {

    /** @return secure hash representation of the supplied raw password */
    String encrypt(String password);

    /** @return whether the raw password matches the stored hash */
    boolean isPasswordValid(String password, String encryptedPassword);
}
