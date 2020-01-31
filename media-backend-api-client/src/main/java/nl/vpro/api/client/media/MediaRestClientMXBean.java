package nl.vpro.api.client.media;

import nl.vpro.rs.client.AbstractApiClientMXBean;

import javax.management.MXBean;

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
