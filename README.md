[![Build Status](https://travis-ci.org/npo-poms/api-clients.svg?)](https://travis-ci.org/npo-poms/api-clients)
[![Maven Central](https://img.shields.io/maven-central/v/nl.vpro.poms.api-clients/api-client-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.vpro.poms.api-clients%22)
[![codecov](https://codecov.io/gh/npo-poms/api-clients/branch/master/graph/badge.svg)](https://codecov.io/gh/npo-poms/api-clients)
[![javadoc](http://www.javadoc.io/badge/nl.vpro.poms.api-clients/frontend-api-client.svg?color=blue)](http://www.javadoc.io/doc/nl.vpro.poms.api-clients/frontend-api-client)
[![snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/nl.vpro.poms.api-clients/api-client-parent.svg)](https://oss.sonatype.org/content/repositories/staging/nl/vpro/poms/api-clients/)


# api-clients
Java API clients for the POMS Rest API's (Frontend API, Backend API, Pages Publisher)

It is split up in several modules. These are the important ones:

* [`frontend-api-client`](frontend-api-client) Provides clients for the [NPO Frontend API](https://rs.poms.omroep.nl) and Pages Update API, implemented using resteasy, which creates proxies for the actuall java rest interfaces

* [`client-extras`](client-extras) Provides some utilities which will make interaction with some of the calls simpler.

* [`media-backend-api-client`](media-backend-api-client). Provides a client for the [POMS Backend API](https://api.poms.omroep.nl). Also using resteasy.

The clients can be configured by code and/or an configuration file in `${user.home}/conf/apiclient.properties`

```java
    NpoApiClients clients = NpoApiClients.configured(nl.vpro.util.Env.TEST).build();
    NpoApiMediaUtil util = new NpoApiMediaUtil(clients);
    Iterator<MediaObject> i = util.iterate(null, "vpro-predictions");
    i.forEachRemaining(mediaObject -> {
       log.info("{}", i.next());
    });
   
```
There are also 'providers' available to configure them easily via spring XML's or for example XML's of magnolia CMS (which uses guice)



## Changes in 5.11

In the 5.11 release we made mayor changes in naming and structure. The groupId's will be changed and all the same ([`nl.vpro.poms.api-clients`](https://search.maven.org/search?q=g:nl.vpro.poms.api-clients)), and the artifactIds will get better names. Also, we will change package names.

See [#1](../../issues/1).

## TODO
- Document how to wire via spring xml
