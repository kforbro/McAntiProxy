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
            System.out.println("1111111111111111");
            String token = config.token;
            String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
            if (config.whitelistedIps.contains(ip)) return;
            System.out.println("22222222222222");
            if (config.whitelistedPlayers.contains(event.getPlayer().getUsername())) return;
            System.out.println("33333333333333");
            if (cacheManager.isCached(ip)) {
                System.out.println("444444444444444444");
                event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getPlayer().getUsername())));
                return;
            }
            System.out.println("555555555555555555");
            try {
                HttpURLConnection connection = HttpUtil.prepareConnection(HttpUtil.createConnection("https://proxycheck.io/v2/" + ip + "?key=" + token + "&vpn=1"));
                if (connection.getResponseCode() != 200)
                    return;
                System.out.println("666666666666666666666666");
                JSONObject response = new JSONObject(HttpUtil.readSourceCode(connection));
                JSONObject data = response.getJSONObject(ip);
                if (!data.has("proxy"))
                    return;
                System.out.println("77777777777777777");
                if (data.getString("proxy").equals("yes")) {

                    event.getPlayer().disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getPlayer().getUsername())));
                    cacheManager.addToCache(ip, true);
                    return;
                }
                System.out.println("888888888888888888888888");
                cacheManager.addToCache(ip, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
