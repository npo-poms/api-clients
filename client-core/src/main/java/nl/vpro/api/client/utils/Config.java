package nl.vpro.api.client.utils;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import nl.vpro.poms.shared.Deployment;
import nl.vpro.util.ConfigUtils;
import nl.vpro.util.Env;

/**
 * Represents configuration for an api client.
 * <p>
 * Arranges reading config files from classpath and ~/conf, and have a switcher with {@link Env}.
 * @author Michiel Meeuwissen
 */
@Slf4j
public class Config {

    /**
     * The resource with properties of all known environments
     * TODO: Move to {@link nl.vpro.poms.shared.NpoPomsEnvironment}
     */
    public static final String URLS_FILE = "poms-urls.properties";
    public static final String CONFIG_FILE = "apiclient.properties";

    private final Map<Key, String> properties;
    private final String[] configFiles;
    private final Map<Prefix, Env> envs= new HashMap<>();
    private final Map<Prefix, Map<String, String>> mappedProperties = new HashMap<>();


    public enum Prefix {
        api(Deployment.api),
        media_api_backend(Deployment.media_api_backend),
        parkpost(null),
        pages_publisher(Deployment.pages_publisher),
        media_publisher(Deployment.media_publisher),
        media(Deployment.media),
        images(Deployment.images),
        images_backend(Deployment.images_backend),
        nep(null);

        @Nullable
        @Getter
        private final Deployment deployment;

        Prefix(@Nullable Deployment deployment) {
            this.deployment = deployment;
            if (this.deployment != null) {
                assert this.deployment.name().equals(name());
            }
        }

        public String getKey() {
            return "npo-" + name();
        }

        public static Prefix ofKey(String value) {
            for (Prefix p : values()) {
                if (value.equals(p.getKey())) {
                    return p;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public Config(String... configFiles) {
        properties = new HashMap<>();
        this.configFiles = configFiles;

        try {
            Map<Key, String> initial = new HashMap<>();
            initial.put(Key.of("localhost"), InetAddress.getLocalHost().getHostName());
            String[] configFilesInHome = ConfigUtils.getConfigFilesInHome(configFiles);
            if (configFilesInHome.length > 0) {
                log.debug("Reading configuration from {}", Arrays.asList(configFilesInHome));
            }
            ConfigUtils.getProperties(
                initial,
                Key::of,
                configFilesInHome
            ).forEach((key, value) -> {
                String previous = properties.put(key, value);
                if (previous != null) {
                    log.info("replaced {}: {} -> {}", key, previous, value);
                }
            });
            log.debug("{}", properties);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.info("Read configuration from {}",  Arrays.asList(configFiles));
    }

    public  Optional<String> configOption(Prefix pref, String prop) {
        String value = getProperties(pref).get(prop);
        return Optional.ofNullable(value);
    }



    public  String requiredOption(Prefix pref, String prop) {
        return configOption(pref, prop)
            .orElseThrow(notSet(pref, prop));
    }


    public  String url(Prefix pref, String path) {
        String base  = requiredOption(pref, "baseUrl");
        if (! base.endsWith("/")) {
            base = base + "/";
        }
        return base + path;

    }

    private Map<String, String> getProperties() {
        return properties.entrySet().stream()
            .collect(Collectors.toMap((e) -> e.getKey().toString(), Map.Entry::getValue));
    }

    public String getProperty(String key) {
        String value = getProperties().get(key);
        if (value == null) {
            throw new IllegalArgumentException("No such key " + key);
        }
        return value;
    }

    public  Map<String, String> getProperties(Prefix prefix) {
        Map<String, String> result = mappedProperties.get(prefix);
        if (result == null) {
            final Map<String, String> r = new HashMap<>();
            final Map<String, Integer> strengths = new HashMap<>();
            result = r;
            mappedProperties.put(prefix, result);
            final Env env = env(prefix);
            log.info("Env for {}: {}", prefix, env);
            properties.forEach((key, value) -> {
                if (key.prefix() == null || prefix.equals(key.prefix())) {
                    Env.Match matches = env.matches(key.env());
                    if (matches.getAsBoolean()) {
                        Integer existingStrength = strengths.get(key.key());
                        int strength = key.strength() + matches.getStrength();
                        if (existingStrength != null && existingStrength == strength) {
                            log.warn("Found the same property twice {} {}", existingStrength, key);
                        }
                        if (existingStrength == null || existingStrength <= strength) {
                            r.put(key.key(), value);
                            strengths.put(key.key(), key.strength());
                            log.debug("Put {} -> {}", key, value);
                        } else {
                            log.debug("ignored {}", key);
                        }
                    }
                }
            });
            log.info("Read for {}.{} {}", prefix, env, r.keySet());
        }
        return result;
    }



    public Map<String, String> getPrefixedProperties(Prefix prefix) {
        return getProperties(prefix)
            .entrySet()
            .stream()
            .map(e -> new AbstractMap.SimpleEntry<>(prefix.getKey() + "." + e.getKey(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public  Supplier<RuntimeException> notSet(String prop) {
        return () -> new RuntimeException(prop + " is not set in " + Arrays.asList(configFiles));
    }

    public  Supplier<RuntimeException> notSet(Prefix pref, String prop) {
        return () -> new RuntimeException(pref + "." + prop + " is not set in " + Arrays.asList(configFiles));
    }

    public void setEnv(Prefix prefix, Env env) {
        if (! Objects.equals(envs.put(prefix, env), env)) {
            mappedProperties.clear();
        }
    }

    public void setEnv(Env env) {
        if (! Objects.equals(envs.put(null, env), env)) {
            mappedProperties.clear();
        }
    }

    @NonNull
    public Env env() {
        Env env = envs.get(null);
        if (env == null) {
            String pref = getEnvProperty("env");
            if (pref == null) {
                env =  Env.valueOf(properties.getOrDefault(ENV, "test").toUpperCase());
            } else {
                env = Env.valueOf(pref.toUpperCase());
            }
            envs.put(null, env);
        }
        return env;
    }

    @NonNull
    public Env env(Prefix prefix) {
        Env env = envs.get(prefix);
        if (env == null) {
            String pref = getEnvProperty(prefix.name() + ".env");
            if (pref != null) {
                env = Env.valueOf(pref.toUpperCase());
            } else {
                Key keyForPrefix = ENV.copyFor(prefix);
                String envString  = properties.getOrDefault(keyForPrefix, env().name());
                env = Env.optionalValueOf(envString).orElse(null);
            }
            if (envs.put(prefix, env) != null) {
                log.warn("Replaced {}", prefix);
            }
        }
        return env;
    }


    public static String getEnvProperty(String name) {
        return getEnvProperty(name, null);
    }

    public static String getEnvProperty(String name, String def) {
        return System.getenv().getOrDefault(name, System.getProperty(name, def));
    }

    private static final Key ENV = new Key(null, "env", null, 1);


    public record Key(Prefix prefix, String key, Env env, int strength) {
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
                        prefix = Prefix.ofKey(split[0]);
                        key = split[1];
                        env = null;
                    } catch (IllegalArgumentException iae) {
                        prefix = null;
                        key = split[0];
                        env = Env.valueOf(split[1].toUpperCase());
                    }
                } else {
                    prefix = Prefix.ofKey(split[0]);
                    key = split[1];
                    try {
                        env = Env.valueOf(split[2].toUpperCase());
                    } catch (IllegalArgumentException ia) {
                        key += "." + split[2];
                        env = null;

                    }
                }
                return new Key(prefix, key, env, split.length);
            } catch (IllegalArgumentException iae) {
                return new Key(null, joinedKey, null, 0);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            if (prefix != null) {
                builder.append(prefix.getKey()).append('.');
            }
            builder.append(key);
            if (env != null) {
                builder.append('.').append(env.name().toLowerCase());
            }
            return builder.toString();
        }

        public Key copyFor(Prefix prefix) {
            return new Key(prefix, key, env, 2);
        }
    }

}
