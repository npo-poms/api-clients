/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
public class Util {

    private static final ZoneId GMT = ZoneId.of("GMT");


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
        return RFC_1123_DATE_TIME.format(date.atZone(GMT));
    }

    @Deprecated
    public static String rfc822(Date date) {
        return rfc822(date.toInstant());
    }

}
