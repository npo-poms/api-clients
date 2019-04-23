[![Build Status](https://travis-ci.org/npo-poms/api-clients.svg?)](https://travis-ci.org/npo-poms/api-clients)
[![Maven Central](https://img.shields.io/maven-central/v/nl.vpro.api-client/api-client-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22nl.vpro.api-client%22)
[![codecov](https://codecov.io/gh/npo-poms/api-clients/branch/master/graph/badge.svg)](https://codecov.io/gh/npo-poms/api-clients)
[![javadoc](http://www.javadoc.io/badge/nl.vpro.api-client/client-resteasy.svg?color=blue)](http://www.javadoc.io/doc/nl.vpro.api-client/client-resteasy)


# api-clients
Java API clients for the POMS Rest API's (Frontend API, Backend API, Pages Publisher)

It is split up in several modules. These are the important ones:

* `client-resteasy` Provides clients for the [NPO Frontend API](https://rs.poms.omroep.nl) and Pages Update API, implemented using resteasy.

* `media-backend-rs-client`. Provides a client for the [POMS Backend API](https://api.poms.omroep.nl). Also using resteasy.

The clients can be configured by code and/or an configuration file in `${user.home}/conf/apiclient.properties`
```java
   clients = NpoApiClients.configured(env).build();
   
```
There are also 'providers' available to configure them easily via spring or guice.


## TODO
There might not be good reason to have this many modules in this project. 
