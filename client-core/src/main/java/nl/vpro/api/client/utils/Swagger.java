package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.rs.client.VersionResult;
import nl.vpro.util.IntegerVersion;

/**
 * @author Michiel Meeuwissen
 * @since 5.6
 */
@Slf4j
public class Swagger {

    private static final Pattern BRANCH_VERSION = Pattern.compile(".*?/REL-(.*?)/.*");

    private static final Pattern VERSION = Pattern.compile("(\\d+.\\d+(?:\\.\\d+)?).*");

    public static VersionResult getVersionFromSwagger(String baseUrl, String defaultVersion) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = new JsonFactory();
            URL url = new URL(baseUrl + "/swagger.json");
            JsonParser jp = factory.createParser(url.openStream());
            JsonNode swagger = mapper.readTree(jp);
            String versionString = swagger.get("info").get("version").asText();
            return VersionResult.builder().version(getVersion(versionString, defaultVersion)).available(true).build();
        } catch (JsonParseException jpe) {
            log.warn(jpe.getMessage());
            return VersionResult.builder().version(defaultVersion).available(true).build();
        } catch (ConnectException e) {
            log.warn(e.getMessage());
            return VersionResult.builder().version(defaultVersion).available(false).build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return VersionResult.builder().version(defaultVersion).available(false).build();
        }
    }

    public static String getVersion(String versionString, String defaultVersion) {
        String result = null;
        {
            Matcher matcher = BRANCH_VERSION.matcher(versionString);
            if (matcher.find()) {
                result = matcher.group(1);
                log.info("Version found from {} -> {}", versionString, result);
            }
        }
        if (result == null) {
            Matcher matcher = VERSION.matcher(versionString);
            if (matcher.matches()) {
                result = matcher.group(1);
            }
        }

        if (result == null) {
            result = defaultVersion;
            log.info("No version found in {}, supposing {}", versionString,  result);
        }
        return result;

    }


    public static IntegerVersion getVersionNumber(String versionString, String defaultVersion) {
        return getVersionNumber(getVersion(versionString, defaultVersion));
    }



    /**
     * The version of the npo frontend api we are talking too.
     **
     */
    public static IntegerVersion  getVersionNumber(String version) {
        return IntegerVersion.parseIntegers(version);
    }
}
