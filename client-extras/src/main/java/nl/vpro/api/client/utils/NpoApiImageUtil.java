package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.domain.media.update.ImageUpdate;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Michiel Meeuwissen
 */
@Named
@Slf4j
public class NpoApiImageUtil {


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
        int lastColon = imageUri.lastIndexOf(':');
        int number = Integer.parseInt(imageUri.substring(lastColon + 1));
        if (supportsOriginal) {
            return Optional.of(baseUrl + "/image/" + number);
        } else {
            return Optional.of(baseUrl + "/image/" + number + ".jpg");

        }
    }

    public Optional<String> getUrl(ImageUpdate iu) {
        return getUrl(iu.getImageUri());
    }

    public Optional<Long> getSize(String imageUri) {
        return getSize(getUrl(imageUri));
    }

    public Optional<Long> getSize(ImageUpdate iu) {
        return getSize(getUrl(iu));
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    Optional<Long> getSize(Optional<String> url) {
        return url
            .map(u -> {
                try (CloseableHttpClient client =  getClient()) {
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
        return getClass().getSimpleName() + "@" + getBaseUrl();
    }
}
