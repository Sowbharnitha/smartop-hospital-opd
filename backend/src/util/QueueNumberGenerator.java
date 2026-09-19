package util;

/**
 * Utility for generating deterministic, formatted OPD queue tokens.
 * Extracts the doctor's initial (e.g. Dr. Arun -> 'A', Dr. Priya -> 'P')
 * and formats the sequential index (e.g. A-001, A-002).
 */
public class QueueNumberGenerator {

    /**
     * Determines the prefix letter for the doctor.
     * Extracts first letter after any title like "Dr. " or "Doctor ".
     */
    public static String getDoctorPrefix(String doctorName) {
        if (doctorName == null || doctorName.trim().isEmpty()) {
            return "A";
        }
        String clean = doctorName.trim();
        if (clean.toLowerCase().startsWith("dr. ")) {
            clean = clean.substring(4).trim();
        } else if (clean.toLowerCase().startsWith("doctor ")) {
            clean = clean.substring(7).trim();
        }
        if (!clean.isEmpty() && Character.isLetter(clean.charAt(0))) {
            return String.valueOf(Character.toUpperCase(clean.charAt(0)));
        }
        return "A";
    }

    /**
     * Formats the prefix and 1-based sequential number into a 3-digit queue token.
     * Example: ('A', 4) -> "A-004"
     */
    public static String formatQueueNumber(String prefix, int sequenceNumber) {
        if (prefix == null || prefix.isEmpty()) {
            prefix = "A";
        }
        if (sequenceNumber < 1) {
            sequenceNumber = 1;
        }
        return String.format("%s-%03d", prefix.toUpperCase(), sequenceNumber);
    }
}
