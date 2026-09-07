package org.ecommerce.customerservice.service;

public interface PasswordService {

    String encrypt(String password);

    boolean isPasswordValid(String password, String encryptedPassword);
}
