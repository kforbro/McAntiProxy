package pl.szczurowsky.mcantiproxy.configs;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.*;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Names(strategy = NameStrategy.HYPHEN_CASE, modifier = NameModifier.TO_LOWER_CASE)
public class PluginConfig extends OkaeriConfig {

    @Variable("API-TOKEN")
    @Comment("Proxycheck.io token")
    public String token = "Put your proxycheck.io token here";

    @Variable("IPS-WHITELIST")
    @Comment("List of IPs which are allowed to connect to server")
    public List<String> whitelistedIps = Collections.singletonList("127.0.0.1");

    @Variable("PLAYERS-WHITELIST")
    @Comment("List of players which are allowed to connect to server")
    public List<String> whitelistedPlayers = Collections.singletonList("Player1");

    @Variable("CACHE-EXPRIE")
    @Comment("Cache expiration time")
    public Duration cacheExpirationTime = Duration.ofSeconds(15);
    @Variable("DISCORD-URL")
    public String discordUrl = "";
    @Variable("DISCORD-THREAD-ID")
    public Long discordThreadId = 1111111111L;
}
