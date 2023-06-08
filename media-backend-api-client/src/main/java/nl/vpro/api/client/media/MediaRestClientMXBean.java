package nl.vpro.api.client.media;

import javax.management.MXBean;

import nl.vpro.rs.client.AbstractApiClientMXBean;

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
