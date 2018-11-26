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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import nl.vpro.domain.media.update.ImageUpdate;

/**
 * @author Michiel Meeuwissen
 */
@Named
@Slf4j
public class NpoApiImageUtil {


    @Setter
    @Getter
    private String baseUrl;

    @Inject
    public NpoApiImageUtil(
        @NotNull @Named("image.baseUrl") String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public Optional<String> getUrl(String imageUri) {
        return Optional.of(baseUrl + "" + imageUri);
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


    Optional<Long> getSize(Optional<String> url) {
        return url
            .map(u -> {
                try (CloseableHttpClient client =  getClient()) {
                    HttpHead head = new HttpHead(u);
                    HttpResponse response = client.execute(head);
                    if (response.getStatusLine().getStatusCode() == 200) {

                        return Long.valueOf(response.getFirstHeader("Content-Length").getValue());
                    } else {
                        return null;
                    }
                } catch (ClientProtocolException e) {
                    log.error(e.getMessage(), e);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
                return null;
            });
    }

    protected CloseableHttpClient getClient() {
        return HttpClientBuilder
            .create()
            .build();
    }
}