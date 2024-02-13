package nl.vpro.api.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.Optional;
import java.util.OptionalLong;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import  jakarta.validation.constraints.NotNull;


import nl.vpro.domain.media.support.ImageUrlService;
import nl.vpro.domain.media.update.ImageUpdate;

/**
 * Utilities related to the 'image' servers, and the simple API that exist.
 *
 * @author Michiel Meeuwissen
 */
@Named
@Slf4j
public class NpoApiImageUtil implements ImageUrlService {

    protected final HttpClient client =  HttpClient.newBuilder().build();


    @Setter
    @Getter
    private String baseUrl;

    @Inject
    public NpoApiImageUtil(
        @NotNull
        @Named("npo-images.baseUrl") String baseUrl) {
        this.baseUrl = baseUrl;
    }


    public Optional<String> getUrl(String imageUri) {
        return Optional.of(getOriginalUrlFromImageUri(imageUri));
    }

    public Optional<String> getUrl(ImageUpdate iu) {
        return iu == null ? Optional.empty() : getUrl(iu.getImageUri());
    }

    public OptionalLong getSize(String imageUri) {
        return getSize(getUrl(imageUri));
    }

    public OptionalLong getSize(ImageUpdate iu) {
        Optional<String> url = getUrl(iu);
        return getSize(url);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    OptionalLong getSize(Optional<String> url) {
        if (url.isPresent()) {
            URI uri = URI.create(url.get());
            try {

                log.info("Getting size of image via {}", uri);
                HttpRequest head = HttpRequest.newBuilder(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

                HttpResponse<Void> response = client.send(head, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() == 200) {
                    return response.headers().firstValueAsLong("Content-Length");
                } else {
                    log.warn("Response {}", response.statusCode());
                    return OptionalLong.empty();
                }
            } catch (InterruptedException | IOException e) {
                log.error(e.getMessage(), e);
                return OptionalLong.empty();
            }
        } else {
            return OptionalLong.empty();
        }
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + getBaseUrl();
    }

    @Override
    public String getImageBaseUrl() {
        return baseUrl + "/image/";
    }

    public boolean isAvailable() {
        try {
            URI health = URI.create(baseUrl + "manage/health");
            HttpRequest head = HttpRequest.newBuilder(health)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<Void> send = client.send(head, HttpResponse.BodyHandlers.discarding());
            if (send.statusCode() == 200) {
                return true;
            } else {
                log.warn("For {} -> {}", health, send.statusCode());
                return false;
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }
}
