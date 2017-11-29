package nl.vpro.api.client.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import nl.vpro.util.Env;
import nl.vpro.util.ReflectionUtils;

/**
 * Represents configuration for an api client.
 *
 * Arranges reading config files from classpath and ~/conf, and have a switcher with {@link Env}.
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Config {

    public static String CONFIG_FILE = "apiclient.properties";

    private final Map<Key, String> properties;
    private final String[] configFiles;
    private Env env = null;
    private final Map<Prefix, Map<String, String>> mappedProperties = new HashMap<>();


    public enum Prefix {

        npoapi("npo-api"),
        backendapi("backend-api"),
        parkpost,
        pageupdateapi("pageupdate-api"),
        poms;
        private final String alt;
        Prefix(String alt) {
            this.alt = alt;
        }
        Prefix() {
            this(null);
        }
        public String getAlt() {
            return alt == null ? name() : alt;
        }

        public static Prefix altValueOf(String value) {
            for (Prefix p : values()) {
                if (value.equals(p.name())) {
                    return p;
                }
                if (p.alt != null && p.alt.equals(value)) {
                    return p;
                }
            }
            throw new IllegalArgumentException();

        }
    }

    public Config (String... configFiles) {
        properties = new HashMap<>();
        this.configFiles = configFiles;



        try {
            Map<String, String> initial = new HashMap<>();
            initial.put("localhost", InetAddress.getLocalHost().getHostName());
            ReflectionUtils.getProperties(
                initial,
                ReflectionUtils.getConfigFilesInHome(configFiles)
            ).forEach((key1, value) -> {
                Key key = Key.of(key1);
                properties.put(key, value);
            });
            log.debug("{}", properties);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.info("Reading {} configuration from {}", env(), configFiles);
    }

    public  Optional<String> configOption(Prefix pref, String prop) {
        String value = getProperties(pref).get(prop);
        return Optional.ofNullable(value);
    }



    public  String requiredOption(Prefix pref, String prop) {
        return configOption(pref, prop)
            .orElseThrow(notSet(prop));
    }


    public  String url(Prefix pref, String path) {
        String base  = requiredOption(pref, "baseUrl");
        if (! base.endsWith("/")) {
            base = base + "/";
        }
        return base + path;

    }

    public  Map<String, String> getProperties(Prefix prefix) {
        Map<String, String> result = mappedProperties.get(prefix);
        if (result == null) {
            final Map<String, String> r = new HashMap<>();
            result = r;
            mappedProperties.put(prefix, result);
            properties.forEach((key, value) -> {
                if (key.getPrefix() == null || prefix.equals(key.getPrefix())) {
                    if (key.getEnv() == null || env().equals(key.getEnv())) {
                        r.put(key.getKey(), value);
                    }
                }
            });
            log.info("Read for {}.{} {}", prefix, env(), r.keySet());
        }
        return result;
    }

    public Map<String, String> getPrefixedProperties(Prefix prefix) {
        return getProperties(prefix)
            .entrySet()
            .stream()
            .map(e -> new AbstractMap.SimpleEntry<>(prefix.getAlt() + "." + e.getKey(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public  Supplier<RuntimeException> notSet(String prop) {
        return () -> new RuntimeException(prop + " is not set in " + Arrays.asList(configFiles));
    }

    public void setEnv(Env env) {
        if (this.env != env) {
            mappedProperties.clear();
            this.env = env;
        }
    }
    public Env env() {
        if (env == null) {
            String pref = System.getProperty("env");
            if (pref == null) {
                return Env.valueOf(properties.getOrDefault("env", "test").toUpperCase());
            } else {
                return Env.valueOf(pref.toUpperCase());
            }
        } else {
            return env;
        }
    }

    @AllArgsConstructor
    @Data
    public static class Key {
        private final Prefix prefix;
        private final String key;
        private final Env env;
        private final int strength;

        public static Key of(String joinedKey) {
            try {
                String[] split = joinedKey.split("\\.", 3);
                Prefix prefix;
                String key;
                Env env;
                if (split.length == 1) {
                    prefix = null;
                    key = split[0];
                    env = null;

                } else if (split.length == 2) {
                    try {
                        prefix = Prefix.altValueOf(split[0]);
                        key = split[1];
                        env = null;
                    } catch (IllegalArgumentException iae) {
                        prefix = null;
                        key = split[0];
                        env = Env.valueOf(split[1].toUpperCase());
                    }
                } else {
                    prefix = Prefix.altValueOf(split[0]);
                    key = split[1];
                    env = Env.valueOf(split[2].toUpperCase());
                }
                return new Key(prefix, key, env, split.length);
            } catch (IllegalArgumentException iae) {
                return new Key(null, joinedKey, null, 0);
            }
        }


    }

}
