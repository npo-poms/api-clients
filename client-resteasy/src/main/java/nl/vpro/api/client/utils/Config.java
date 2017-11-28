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
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Config {

    private final Map<Key, String> properties;
    private final String[] configFiles;
    private Env env = null;


    public enum Prefix {
        npoapi,
        backendapi,
        parkpost,
        pageupdateapi,
        poms
    }

    public Config (String... configFiles) {
        properties = new HashMap<>();
        this.configFiles = configFiles;


        try {
            Map<String, String> initial = new HashMap<>();
            initial.put("localhost", InetAddress.getLocalHost().getHostName());
            properties.putAll(
                ReflectionUtils.getProperties(initial,
                    ReflectionUtils.getConfigFilesInHome(configFiles)
                ).entrySet()
                    .stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(Key.of(e.getKey()), e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            );
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
        return properties.entrySet()
            .stream()
            .filter(e -> prefix == null || e.getKey().getPrefix() == null || e.getKey().getPrefix().equals(prefix))
            .filter(e -> e.getKey().getEnv() == null || env() == e.getKey().getEnv())
            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().getKey(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public  Supplier<RuntimeException> notSet(String prop) {
        return () -> new RuntimeException(prop + " is not set in " + Arrays.asList(configFiles));
    }

    public void setEnv(Env env) {
        this.env = env;
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
                        prefix = Prefix.valueOf(split[0]);
                        key = split[1];
                        env = null;
                    } catch (IllegalArgumentException iae) {
                        prefix = null;
                        key = split[0];
                        env = Env.valueOf(split[1].toUpperCase());
                    }
                } else {
                    prefix = Prefix.valueOf(split[0]);
                    key = split[1];
                    env = Env.valueOf(split[2].toUpperCase());
                }
                return new Key(prefix, key, env, split.length);
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException("Could not parse " + joinedKey, iae);
            }
        }


    }

}
