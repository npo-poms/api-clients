package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.vpro.poms.shared.Headers;
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
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                URI url = URI.create(baseUrl + "/swagger.json");
                HttpUriRequest request = new HttpGet(url);
                request.addHeader(Headers.NPO_DATE, "CacheBust-" + UUID.randomUUID()); // Cloudfront includes npo date as a cache key header.
                try (CloseableHttpResponse response = client.execute(request)) {
                    try (InputStream stream = response.getEntity().getContent()) {
                        JsonParser jp = factory.createParser(stream);
                        JsonNode swagger = mapper.readTree(jp);
                        String versionString = swagger.get("info").get("version").asText();
                        return VersionResult.builder().version(getVersion(versionString, defaultVersion)).available(true).build();
                    }
                }
            }
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
