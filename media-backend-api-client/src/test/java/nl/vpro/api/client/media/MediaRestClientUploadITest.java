package nl.vpro.api.client.media;

import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;

import nl.vpro.domain.media.Encryption;
import nl.vpro.domain.media.update.TranscodeRequest;
import nl.vpro.logging.LoggerOutputStream;
import nl.vpro.util.Env;
import nl.vpro.util.FileCachingInputStream;

/**
/**
 * @author Michiel Meeuwissen
 */
@Slf4j
@Disabled
class MediaRestClientUploadITest {

    private MediaRestClient client;

    @BeforeEach
    void setUp() {
        client = MediaRestClient.configured(Env.TEST).build();

    }
    @AfterEach
    void shutdown() {
        client.shutdown();
    }



    @Test
    void uploadAndTranscode() throws IOException {
        File file = new File("/Users/michiel/samples/huge.mxf");
        try(InputStream is = FileCachingInputStream.builder()
            .input(Files.newInputStream(file.toPath()))
            .batchSize(5_000_000L)
            .progressLogging(false)
            .logger(log)
            .build();
            Response response = client.getBackendRestService().
                uploadAndTranscode("POMS_VPRO_1424050",Encryption.NONE, TranscodeRequest.Priority.NORMAL,  "",
                    is,
                    "video/mp4",
                    null,  true,
                    true,
                    null,  null,
                    null)) {
            InputStream responseEntity = (InputStream) response.getEntity();
            log.info("Found {}", responseEntity);
            IOUtils.copy(responseEntity, LoggerOutputStream.info(log));

        }



    }



}
