package pl.szczurowsky.mcantiproxy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.json.JSONObject;
import pl.szczurowsky.mcantiproxy.cache.CacheManager;
import pl.szczurowsky.mcantiproxy.configs.MessagesConfig;
import pl.szczurowsky.mcantiproxy.configs.PluginConfig;
import pl.szczurowsky.mcantiproxy.util.ColorUtil;
import pl.szczurowsky.mcantiproxy.util.HttpUtil;

import java.net.HttpURLConnection;

public class PreLoginHandler implements Listener {

    private final PluginConfig config;
    private final MessagesConfig messagesConfig;
    private final CacheManager cacheManager;

    public PreLoginHandler(PluginConfig config, MessagesConfig messagesConfig, CacheManager cacheManager) {
        this.config = config;
        this.messagesConfig = messagesConfig;
        this.cacheManager = cacheManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String token = config.token;
        String ip = event.getAddress().getHostAddress();
        System.out.println("[McAntiProxy] " + event.getName() + " 1 " + ip);
        if (config.whitelistedIps.contains(ip)) return;
        System.out.println("[McAntiProxy] " + event.getName() + " 2 " + ip);
        if (config.whitelistedPlayers.contains(event.getName().toLowerCase())) return;
        System.out.println("[McAntiProxy] " + event.getName() + " 3 " + ip);
        if (cacheManager.isCached(ip)) {
            System.out.println("[McAntiProxy] " + event.getName() + " 4 " + ip);
            if (cacheManager.isProxy(ip)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ColorUtil.format(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getName())));

                System.out.println("[McAntiProxy] " + event.getName() + " 5 " + ip);
            }
            return;
        }
        System.out.println("[McAntiProxy] " + event.getName() + " 6 " + ip);
        try {
            HttpURLConnection connection = HttpUtil.prepareConnection(HttpUtil.createConnection("https://proxycheck.io/v2/" + ip + "?key=" + token + "&vpn=1"));
            if (connection.getResponseCode() != 200)
                return;
            System.out.println("[McAntiProxy] " + event.getName() + " 7 " + ip);
            JSONObject response = new JSONObject(HttpUtil.readSourceCode(connection));
            JSONObject data = response.getJSONObject(ip);
            if (!data.has("proxy"))
                return;
            System.out.println("[McAntiProxy] " + event.getName() + " 8 " + ip);
            if (data.getString("proxy").equals("yes")) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ColorUtil.format(messagesConfig.kickMessage.replace("{ip}", ip).replace("{username}", event.getName())));
                cacheManager.addToCache(ip, true);
                System.out.println("[McAntiProxy] " + event.getName() + " 10 " + ip);
                return;
            }
            System.out.println("[McAntiProxy] " + event.getName() + " 9 " + ip);
            cacheManager.addToCache(ip, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
