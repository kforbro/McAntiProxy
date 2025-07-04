package pl.szczurowsky.mcantiproxy;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.rollczi.litecommands.velocity.LiteVelocityFactory;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.jetbrains.annotations.NotNull;
import pl.szczurowsky.mcantiproxy.cache.CacheManager;
import pl.szczurowsky.mcantiproxy.commands.AntiProxyCommandsBuilder;
import pl.szczurowsky.mcantiproxy.configs.MessagesConfig;
import pl.szczurowsky.mcantiproxy.configs.PluginConfig;
import pl.szczurowsky.mcantiproxy.configs.factory.ConfigFactory;
import pl.szczurowsky.mcantiproxy.listener.PreLoginHandler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

@Plugin(
        id = "mcantiproxy",
        name = "McAntiProxy-Velocity",
        authors = "Szczurowsky",
        version = "1.1",
        url = "https://szczurowsky.pl/"
)
public class VelocityPlugin {

    private PluginConfig config;
    private MessagesConfig messagesConfig;
    private CacheManager cacheManager;

    /**
     * Velocity plugin constructor.
     */
    private final Logger logger;
    private final Path dataDirectory;
    private final ProxyServer proxyServer;
    public WebhookClient client;

    @Inject
    public VelocityPlugin(Logger logger, @DataDirectory Path dataDirectory, ProxyServer proxyServer) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onProxyInitialization(@NotNull ProxyInitializeEvent event) {
        LocalDateTime starting = LocalDateTime.now();

        logger.info("Enabling anti-proxy plugin");

        registerConfigs();
        logger.info("Configs registered");

        this.cacheManager = new CacheManager(config.cacheExpirationTime);
        logger.info("Cache manager initialized");

        registerEvents();
        logger.info("Events registered");

        registerCommands();
        logger.info("Commands registered");

        WebhookClientBuilder builder = new WebhookClientBuilder(config.discordUrl); // or id, token
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("McAntiProxyDiscord Thread");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(true);
        client = builder.build();

        logger.info("Successfully enabled plugin in " + ChronoUnit.MILLIS.between(starting, LocalDateTime.now()) + "ms");
    }

    private void registerCommands() {
        AntiProxyCommandsBuilder.applyOn(LiteVelocityFactory.builder(proxyServer), this.config, this.messagesConfig)
                .forwardingRegister();
    }

    private void registerEvents() {
        this.proxyServer.getEventManager().register(this, new PreLoginHandler(this, config, messagesConfig, cacheManager));
    }

    private void registerConfigs() {
        ConfigFactory configFactory = new ConfigFactory(dataDirectory.toFile(), YamlSnakeYamlConfigurer::new);
        this.config = configFactory.produceConfig(PluginConfig.class, "config.yml");
        this.messagesConfig = configFactory.produceConfig(MessagesConfig.class, "message.yml");
    }

}
