package byzas.libs.flow.manager.util.extensions;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public interface StringUtilSupport {
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");
    String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String LOWER = UPPER.toLowerCase();
    String DIGITS = "0123456789";
    char[] ALPHANUM = (UPPER + LOWER+ DIGITS).toCharArray();
    SecureRandom SECURE_RANDOM = new SecureRandom();

    default String formatDate(Date date, DateTimeFormatter formatter) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return formatter.format(ldt);
    }
    // todo generates 18 char transactionId
    default String generateTransactionId(Date date,long id){
        return String.format("%s%s", formatDate(date,DATE_TIME_FORMATTER), String.format("%08d", id));
    }

    default String generateNumericStringWithDateAndId(Date date, DateTimeFormatter formatter, long anotherId, int maxDigits) {
        String formattedDate = formatDate(date, formatter);
        int remaining = maxDigits - formattedDate.length();
        if (remaining < integerDigitCount(anotherId))
            throw new RuntimeException(String.format("invalid max length : %s", maxDigits));
        return String.format("%s%s", formattedDate, String.format("%0" + remaining + "d", anotherId));
    }

    default String randomString(int length) {
        char[] buf = new char[length];
        for (int idx = 0; idx < length; ++idx)
            buf[idx] = ALPHANUM[SECURE_RANDOM.nextInt(ALPHANUM.length)];
        return new String(buf);
    }

    default int integerDigitCount(long number){
        if (number < 100000) {
            if (number < 100) {
                if (number < 10) {
                    return 1;
                } else {
                    return 2;
                }
            } else {
                if (number < 1000) {
                    return 3;
                } else {
                    if (number < 10000) {
                        return 4;
                    } else {
                        return 5;
                    }
                }
            }
        } else {
            if (number < 10000000) {
                if (number < 1000000) {
                    return 6;
                } else {
                    return 7;
                }
            } else {
                if (number < 100000000) {
                    return 8;
                } else {
                    if (number < 1000000000) {
                        return 9;
                    } else {
                        return 10;
                    }
                }
            }
        }
    }

}

class StringUtilSupport2 {
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String LOWER = UPPER.toLowerCase();
    String DIGITS = "0123456789";
    char[] ALPHANUM = (UPPER + LOWER+ DIGITS).toCharArray();
    SecureRandom SECURE_RANDOM = new SecureRandom();

    static String formatDate(Date date, DateTimeFormatter formatter) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return formatter.format(ldt);
    }
    String generateTransactionId(Date date,long id){
        return String.format("%s%s", formatDate(date,DATE_TIME_FORMATTER), String.format("%08d", id));
    }

    static String generateNumericStringWithDateAndId(Date date, DateTimeFormatter formatter, long anotherId, int maxDigits) {
        String formattedDate = formatDate(date, formatter);
        int remaining = maxDigits - formattedDate.length();
        if (remaining < integerDigitCount(anotherId))
            throw new RuntimeException(String.format("invalid max length : %s", maxDigits));
        return String.format("%s%s", formattedDate, String.format("%0" + remaining + "d", anotherId));
    }


    String randomString(int length) {
        char[] buf = new char[length];
        for (int idx = 0; idx < length; ++idx)
            buf[idx] = ALPHANUM[SECURE_RANDOM.nextInt(ALPHANUM.length)];
        return new String(buf);
    }

    static int integerDigitCount(long number){
        if (number < 100000) {
            if (number < 100) {
                if (number < 10) {
                    return 1;
                } else {
                    return 2;
                }
            } else {
                if (number < 1000) {
                    return 3;
                } else {
                    if (number < 10000) {
                        return 4;
                    } else {
                        return 5;
                    }
                }
            }
        } else {
            if (number < 10000000) {
                if (number < 1000000) {
                    return 6;
                } else {
                    return 7;
                }
            } else {
                if (number < 100000000) {
                    return 8;
                } else {
                    if (number < 1000000000) {
                        return 9;
                    } else {
                        return 10;
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(generateNumericStringWithDateAndId(new Date(), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),0, 15 ));
    }



}