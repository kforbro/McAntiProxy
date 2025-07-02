package pl.szczurowsky.mcantiproxy.listener;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.json.JSONObject;
import pl.szczurowsky.mcantiproxy.VelocityPlugin;
import pl.szczurowsky.mcantiproxy.cache.CacheManager;
import pl.szczurowsky.mcantiproxy.configs.MessagesConfig;
import pl.szczurowsky.mcantiproxy.configs.PluginConfig;
import pl.szczurowsky.mcantiproxy.util.HttpUtil;

import java.net.HttpURLConnection;
import java.util.UUID;

public class PreLoginHandler {

    private final VelocityPlugin velocityPlugin;
    private final PluginConfig config;
    private final MessagesConfig messagesConfig;
    private final CacheManager cacheManager;

    public PreLoginHandler(VelocityPlugin velocityPlugin, PluginConfig config, MessagesConfig messagesConfig, CacheManager cacheManager) {
        this.velocityPlugin = velocityPlugin;
        this.config = config;
        this.messagesConfig = messagesConfig;
        this.cacheManager = cacheManager;
    }

    @Subscribe(order = PostOrder.FIRST)
    public EventTask onPostLoginEvent(PostLoginEvent event) {
        return EventTask.async(() -> {
            String token = config.token;
            String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
            if (isFloodgateId(event.getPlayer().getUniqueId())) return;
            if (config.whitelistedIps.contains(ip)) return;
            if (config.whitelistedPlayers.contains(event.getPlayer().getUsername().toLowerCase())) return;
            if (cacheManager.isCached(ip)) {
                if (cacheManager.isProxy(ip)) {
                    event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getPlayer().getUsername())));
                }
                return;
            }
            try {
                HttpURLConnection connection = HttpUtil.prepareConnection(HttpUtil.createConnection("https://proxycheck.io/v2/" + ip + "?key=" + token + "&vpn=1&asn=1"));
                if (connection.getResponseCode() != 200)
                    return;
                JSONObject response = new JSONObject(HttpUtil.readSourceCode(connection));
                JSONObject data = response.getJSONObject(ip);
                if (!data.has("proxy"))
                    return;
                if (data.getString("proxy").equals("yes")) {
                    event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getPlayer().getUsername())));
                    cacheManager.addToCache(ip, true);
                    sendWebhook(event.getPlayer(), data);
                    return;
                }
                cacheManager.addToCache(ip, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }

    public void sendWebhook(Player player, JSONObject data) {
        try {
            WebhookEmbed embed = new WebhookEmbedBuilder()
                    .setAuthor(new WebhookEmbed.EmbedAuthor("Вход заблокирован", "https://sun9-69.userapi.com/impg/0k0SCPtMD2IsZmif47zXKFMNKDvjB541ZdHYEw/jgj_51K2CPc.jpg?size=1060x1060&quality=95&sign=c62d0cce33649f773997a7d3466ace63&type=album", null))
                    .setDescription("<t:" + ((int) (System.currentTimeMillis() / 1000)) + ":R>")
                    .addField(new WebhookEmbed.EmbedField(true, "Игрок", player.getUsername()))
                    .addField(new WebhookEmbed.EmbedField(true, "IP", player.getRemoteAddress().getAddress().getHostAddress()))
                    .addField(new WebhookEmbed.EmbedField(true, "Через", (data.has("country") ? data.getString("country") : "") + ", " + (data.has("city") ? data.getString("city") : "")))
                    .addField(new WebhookEmbed.EmbedField(false, "Провайдер, организация", (data.has("provider") ? data.getString("provider") : "") + ", " + (data.has("organisation") ? data.getString("organisation") : "")))
                    .setColor(0xfa5d5d)
                    .build();
            velocityPlugin.client.send(embed);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
