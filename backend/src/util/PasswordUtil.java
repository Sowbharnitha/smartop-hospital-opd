package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for cryptographic password hashing and verification using SHA-256
 * and application-level salting from standard Java libraries.
 */
public class PasswordUtil {

    private static final String SALT = "SmartOP@2026";

    /**
     * Hashes a raw password with the salt using SHA-256.
     * @param password Plain-text password
     * @return 64-character lowercase hex string
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((password + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable in JVM", e);
        }
    }

    /**
     * Verifies if a raw password matches the stored SHA-256 hash.
     * @param rawPassword Plain-text password candidate
     * @param storedHash Hashed password stored in database
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String calculated = hashPassword(rawPassword);
        return calculated.equalsIgnoreCase(storedHash.trim());
    }
}
