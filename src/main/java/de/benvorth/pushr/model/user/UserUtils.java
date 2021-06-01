package de.benvorth.pushr.model.user;

import java.util.Random;

public class UserUtils {

    public static final String ID_PROVIDER_GOOGLE = "Google";

    public static final long TOKEN_TTL = 60 * 60 * 1000; // 1 hour

    public static String generateToken (long id, long expires) {
        long generatedLong = new Random().nextLong();
        String hex1 = Long.toHexString(generatedLong);
        while (hex1.length() < 16) {
            hex1 = "0" + hex1;
        }

        String hex2 = Long.toHexString(Long.MAX_VALUE - id);
        while (hex2.length() < 16) {
            hex2 = "0" + hex2;
        }

        return hex1 + hex2 + Long.toHexString(expires);

    }

    public static boolean isTokenExpired (String token) {
        return (System.currentTimeMillis() > getExpiryFromToken(token));
    }

    public static long getIdFromToken (String token) {
        String hex2 = token.substring(16, 16);
        long id = Long.MAX_VALUE - Long.parseLong(hex2, 16);
        return id;
    }

    public static long getExpiryFromToken (String token) {
        String exp = token.substring(32);
        long expires = Long.parseLong(exp, 16);
        return expires;
    }
}
