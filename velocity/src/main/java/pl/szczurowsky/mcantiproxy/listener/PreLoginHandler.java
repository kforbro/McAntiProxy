package pl.szczurowsky.mcantiproxy.listener;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.json.JSONObject;
import pl.szczurowsky.mcantiproxy.cache.CacheManager;
import pl.szczurowsky.mcantiproxy.configs.MessagesConfig;
import pl.szczurowsky.mcantiproxy.configs.PluginConfig;
import pl.szczurowsky.mcantiproxy.util.HttpUtil;

import java.net.HttpURLConnection;
import java.util.UUID;

public class PreLoginHandler {

    private final PluginConfig config;
    private final MessagesConfig messagesConfig;
    private final CacheManager cacheManager;

    public PreLoginHandler(PluginConfig config, MessagesConfig messagesConfig, CacheManager cacheManager) {
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
                HttpURLConnection connection = HttpUtil.prepareConnection(HttpUtil.createConnection("https://proxycheck.io/v2/" + ip + "?key=" + token + "&vpn=1"));
                if (connection.getResponseCode() != 200)
                    return;
                JSONObject response = new JSONObject(HttpUtil.readSourceCode(connection));
                JSONObject data = response.getJSONObject(ip);
                if (!data.has("proxy"))
                    return;
                if (data.getString("proxy").equals("yes")) {
                    event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getPlayer().getUsername())));
                    cacheManager.addToCache(ip, true);
                    return;
                }
                cacheManager.addToCache(ip, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public boolean isFloodgateId(UUID uuid) {
        return uuid.getMostSignificantBits() == 0L;
    }

}
