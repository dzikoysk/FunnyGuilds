package net.dzikoysk.funnyguilds.system.security;

import net.dzikoysk.funnyguilds.FunnyGuilds;
import net.dzikoysk.funnyguilds.data.configs.MessageConfiguration;
import net.dzikoysk.funnyguilds.util.commons.ChatUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static double compensationMs(double millisecond) {
        return 0.007 * millisecond;
    }

    public static void sendToOperator(Player player, CheatType cheat, String note) {
        MessageConfiguration messages = FunnyGuilds.getInstance().getMessageConfiguration();
        String message = messages.SecuritySystemPrefix + messages.SecuritySystemInfo;
        String messageNote = messages.SecuritySystemPrefix + messages.SecuritySystemNote;

        message = StringUtils.replace(message, "{PLAYER}", player.getName());
        message = StringUtils.replace(message, "{CHEAT}", cheat.getName());
        messageNote = StringUtils.replace(messageNote, "{NOTE}", note);

        Bukkit.broadcast(ChatUtils.colored(message), "funnyguilds.admin");
        Bukkit.broadcast(ChatUtils.colored(messageNote), "funnyguilds.admin");
    }

}
