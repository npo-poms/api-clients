/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
public class Util {

    private static final String RFC822 = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final ZoneId GMT = ZoneId.of("GMT");

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern(RFC822, Locale.US);


    public static String hmacSHA256(String privateKey, String data) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(privateKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSHA256.init(keySpec);
            return Base64.getEncoder().encodeToString(hmacSHA256.doFinal(data.getBytes())).trim();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String rfc822(Instant date) {
        return FORMAT.format(date.atZone(GMT));
    }
    @Deprecated
    public static String rfc822(Date date) {
        return rfc822(date.toInstant());
    }

}
