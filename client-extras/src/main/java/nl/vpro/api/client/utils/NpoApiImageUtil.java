package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import nl.vpro.domain.media.support.ImageUrlService;
import nl.vpro.domain.media.update.ImageUpdate;

/**
 * @author Michiel Meeuwissen
 */
@Named
@Slf4j
public class NpoApiImageUtil implements ImageUrlService {

    @Setter
    @Getter
    private String baseUrl;

    boolean supportsOriginal;

    @Inject
    public NpoApiImageUtil(
        @NotNull
        @Named("image.baseUrl") String baseUrl) {
        this.baseUrl = baseUrl;
        this.supportsOriginal = baseUrl.contains("dev") || baseUrl.contains("test");
    }


    public Optional<String> getUrl(String imageUri) {
        if (supportsOriginal) {
            return Optional.of(getOriginalUrlFromImageUri(imageUri));
        } else {
            return Optional.of(baseUrl + "/image/" + getIdFromImageUri(imageUri) + ".jpg");

        }
    }

    public Optional<String> getUrl(ImageUpdate iu) {
        return iu == null ? Optional.empty() : getUrl(iu.getImageUri());
    }

    public Optional<Long> getSize(String imageUri) {
        return getSize(getUrl(imageUri));
    }

    public Optional<Long> getSize(ImageUpdate iu) {
        Optional<String> url = getUrl(iu);
        return getSize(url);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<Long> getSize(Optional<String> url) {
        return url
            .map(u -> {
                try (CloseableHttpClient client =  getClient()) {
                    log.info("Getting size of image via {}", u);
                    HttpHead head = new HttpHead(u);
                    HttpResponse response = client.execute(head);
                    if (response.getStatusLine().getStatusCode() == 200) {
                        return Long.valueOf(response.getFirstHeader("Content-Length").getValue());
                    } else {
                        log.warn("Response {}", response.getStatusLine());
                        return -1L;
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                return -1L;
            });
    }

    protected CloseableHttpClient getClient() {
        return HttpClientBuilder
            .create()
            .build();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getBaseUrl();
    }

    @Override
    public String getImageBaseUrl() {
        return baseUrl + "/image/";

    }
}
