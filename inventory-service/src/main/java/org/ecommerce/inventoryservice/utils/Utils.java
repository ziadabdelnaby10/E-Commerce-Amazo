package org.ecommerce.inventoryservice.utils;

public final class Utils {
    private Utils() {}

    public static Long nextVersion(Long version) {
        return version == null ? 1L : version + 1;
    }
}
