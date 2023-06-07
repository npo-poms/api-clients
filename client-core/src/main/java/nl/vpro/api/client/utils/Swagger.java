package nl.vpro.api.client.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    public static VersionResult getVersionFromSwagger(String baseUrl, String defaultVersion)  {
        return getVersionFromSwagger(baseUrl, defaultVersion, null);
    }

    public static VersionResult getVersionFromSwagger(String baseUrl, String defaultVersion, @Nullable Duration timeout) {
        try {
            if (timeout == null) {
                timeout = Duration.ofSeconds(3);
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonFactory factory = new JsonFactory();
            RequestConfig config = RequestConfig.custom()
                .setConnectTimeout((int) timeout.toMillis())
                .setConnectionRequestTimeout((int) timeout.toMillis())
                .setSocketTimeout((int) timeout.toMillis()).build();
            try (CloseableHttpClient client = HttpClientBuilder
                .create()
                .setDefaultRequestConfig(config)
                .build()) {

                URI url = URI.create(baseUrl + "/openapi.json");
                HttpUriRequest request = new HttpGet(url);

                request.addHeader(Headers.NPO_DATE, "CacheBust-" + UUID.randomUUID()); // Cloudfront includes npo date as a cache key header.
                try (CloseableHttpResponse response = client.execute(request)) {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        try (InputStream stream = response.getEntity().getContent()) {
                            JsonParser jp = factory.createParser(stream);
                            JsonNode openapi = mapper.readTree(jp);
                            String versionString = openapi.get("info").get("version").asText();
                            return VersionResult.builder().version(getVersion(versionString, defaultVersion)).available(true).build();
                        }
                    } else {
                        log.warn("No swagger found at {} -> {}", url, response.getStatusLine());
                    }
                }
                return VersionResult.builder().version(defaultVersion).available(false).build();
            }
        } catch (JsonParseException jpe) {
            log.warn(jpe.getMessage());
            return VersionResult.builder().version(defaultVersion).available(true).build();
        } catch (ConnectException | ConnectTimeoutException e) {
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
