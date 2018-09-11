package nl.vpro.rs.media;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
public class MediaRestClientUploadITest {

    private MediaRestClient client;

    @Before
    public void setUp() {
        client = MediaRestClient.configured(Env.LOCALHOST).build();

    }
    @After
    public void shutdown() {
        client.shutdown();
    }



    @Test
    public void uploadAndTranscode() throws IOException {
        File file = new File("/Users/michiel/mmbase/trunk/applications/streams/samples/JessieJ-174205.jpg");
        InputStream is = FileCachingInputStream.builder()
            .input(new FileInputStream(file))
            .batchSize(5_000_000L)
            .logger(log)
            .build();
        Response response = client.getBackendRestService().
            uploadAndTranscode("POMS_VPRO_1424050",Encryption.NONE, TranscodeRequest.Priority.NORMAL,  "",
                is,
                "video/mp4",
                null,  true, true, null,
                null);
        InputStream responseEntity = (InputStream) response.getEntity();

        IOUtils.copy(responseEntity, LoggerOutputStream.info(log));

    }



}
