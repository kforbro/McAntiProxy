package pl.szczurowsky.mcantiproxy.commands;

import dev.rollczi.litecommands.argument.Arg;
import dev.rollczi.litecommands.argument.Name;
import dev.rollczi.litecommands.command.execute.Execute;
import dev.rollczi.litecommands.command.section.Section;
import eu.okaeri.configs.exception.OkaeriException;
import pl.szczurowsky.mcantiproxy.configs.MessagesConfig;
import pl.szczurowsky.mcantiproxy.configs.PluginConfig;

import java.util.List;

@Section(route = "antiproxy")
public class AntiProxyCommand {

    private final MessagesConfig messagesConfig;
    private final PluginConfig config;

    public AntiProxyCommand(MessagesConfig messagesConfig, PluginConfig config) {
        this.messagesConfig = messagesConfig;
        this.config = config;
    }

    @Execute(required = 0)
    public String help() {
        return String.join("\n", messagesConfig.helpMessage);
    }

    @Execute(route = "reload")
    public String reload() {
        try {
            config.load();
            messagesConfig.load();
            return messagesConfig.reloadSuccessMessage;
        } catch (OkaeriException exception) {
            return messagesConfig.reloadFailureMessage;
        }
    }

    @Execute(route = "whitelist", required = 3)
    public String whitelist(@Arg @Name("add|remove") String action, @Arg @Name("ip|player") String target, @Arg @Name("IP") String value) {
        List<String> whitelist = (target.equalsIgnoreCase("ip")) ? config.whitelistedIps : config.whitelistedPlayers;
        if (action.equalsIgnoreCase("add")) {
            if (!whitelist.contains(value)) {
                whitelist.add(value);
                config.save();
            }
            return messagesConfig.addWhitelistMessage;
        } else {
            whitelist.remove(value);
            config.save();
            return messagesConfig.removeWhitelistMessage;
        }
    }
}
