/*
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.utils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
class Util {

    private static final String RFC822 = "EEE, dd MMM yyyy HH:mm:ss z";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

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

    public static String rfc822(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(RFC822, Locale.US);
        sdf.setTimeZone(GMT);
        return sdf.format(date);
    }

}
