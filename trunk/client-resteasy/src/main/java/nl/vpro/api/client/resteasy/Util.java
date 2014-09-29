/**
 * Copyright (C) 2013 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.client.resteasy;

import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Roelof Jan Koekoek
 * @since 2.1
 */
class Util {

    private static final String RFC822 = "EEE, dd MMM yyyy HH:mm:ss z";

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            return toString(hash);
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String hmacSHA256(String privateKey, String data) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(privateKey.getBytes("UTF-8"), "HmacSHA256");
            hmacSHA256.init(keySpec);
            return Base64.encodeBase64String(hmacSHA256.doFinal(data.getBytes())).trim();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String rfc822(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(RFC822, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    public static Date rfc822(String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(RFC822, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.parse(date);
    }

    private static String toString(byte[] hash) {
        StringBuffer hexString = new StringBuffer();

        for(int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private static String trimAndReplaceNull(String optional) {
        return (StringUtils.isNotBlank(optional) ? optional.trim() : UUID.randomUUID().toString());
    }
}
