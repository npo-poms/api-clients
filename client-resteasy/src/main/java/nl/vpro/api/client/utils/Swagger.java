package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class Swagger {

    private static final Pattern VERSION = Pattern.compile(".*?/REL-(.*?)/.*");

    public static String getVersionFromSwagger(String baseUrl, String defaultVersion) {
        String result = "unknown";
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = new JsonFactory();
            URL url = new URL(baseUrl + "/swagger.json");
            JsonParser jp = factory.createParser(url.openStream());
            JsonNode swagger = mapper.readTree(jp);
            String versionString = swagger.get("info").get("version").asText();
            Matcher matcher = VERSION.matcher(versionString);
            if (matcher.find()) {
                result = matcher.group(1);
            } else {
                result = defaultVersion;
                log.info("No version found in {} {}, supposing {}", url, versionString,  result);
            }
        } catch (JsonParseException jpe) {
            log.warn(jpe.getMessage());
            result = defaultVersion;
        } catch (ConnectException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }


    /**
     * The version of the npo frontend api we are talking too.
     *
     * @return a float representing the major/minor version. The patch level is added as thousands.
     */
    public static Float getVersionNumber(String version) {
        Matcher matcher = Pattern.compile("(\\d+\\.\\d+)\\.?(\\d+)?.*").matcher(version);
        if (matcher.find()) {
            Double result = Double.parseDouble(matcher.group(1));
            String minor = matcher.group(2);
            if (minor != null) {
                result += (double) Integer.parseInt(minor) / 1000d;
            }
            return result.floatValue();
        } else {
            log.info("Version {} could not be parsed", version);
            return 0f;
        }
    }
}
