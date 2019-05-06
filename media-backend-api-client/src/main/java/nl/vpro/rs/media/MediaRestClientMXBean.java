package nl.vpro.rs.media;

import javax.management.MXBean;

import nl.vpro.api.client.resteasy.AbstractApiClientMXBean;

/**
 * @author Michiel Meeuwissen
 * @since 5.3
 */
@MXBean
public interface MediaRestClientMXBean extends AbstractApiClientMXBean {

    String getErrors();

    void setErrors(String errors);

    String getUserName();

    void setUserName(String user);

    String getPassword();
    void setPassword(String password);
}
