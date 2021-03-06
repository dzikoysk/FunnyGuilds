package net.dzikoysk.funnyguilds.listener;

import net.dzikoysk.funnyguilds.FunnyGuilds;
import net.dzikoysk.funnyguilds.basic.guild.Region;
import net.dzikoysk.funnyguilds.basic.guild.RegionUtils;
import net.dzikoysk.funnyguilds.basic.user.User;
import net.dzikoysk.funnyguilds.basic.user.UserCache;
import net.dzikoysk.funnyguilds.basic.user.UserUtils;
import net.dzikoysk.funnyguilds.concurrency.ConcurrencyManager;
import net.dzikoysk.funnyguilds.concurrency.requests.dummy.DummyGlobalUpdateUserRequest;
import net.dzikoysk.funnyguilds.concurrency.requests.prefix.PrefixGlobalUpdatePlayer;
import net.dzikoysk.funnyguilds.concurrency.requests.rank.RankUpdateUserRequest;
import net.dzikoysk.funnyguilds.data.configs.PluginConfiguration;
import net.dzikoysk.funnyguilds.element.IndividualPrefix;
import net.dzikoysk.funnyguilds.element.tablist.IndividualPlayerList;
import net.dzikoysk.funnyguilds.nms.api.packet.FunnyGuildsChannelHandler;
import net.dzikoysk.funnyguilds.system.war.WarPacketCallbacks;
import net.dzikoysk.funnyguilds.util.nms.GuildEntityHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoin implements Listener {

    private final FunnyGuilds plugin;

    public PlayerJoin(FunnyGuilds plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        User user = User.get(player);

        if (user == null) {
            user = User.create(player);
        }
        else {
            String playerName = player.getName();

            if (! user.getName().equals(playerName)) {
                UserUtils.updateUsername(user, playerName);
            }
        }

        user.updateReference(player);
        UserCache cache = user.getCache();
        PluginConfiguration config = FunnyGuilds.getInstance().getPluginConfiguration();

        IndividualPlayerList individualPlayerList = new IndividualPlayerList(
                user,
                plugin.getNmsAccessor().getPlayerListAccessor(),
                config.playerList,
                config.playerListHeader, config.playerListFooter,
                config.playerListPing,
                config.playerListFillCells
        );

        cache.setPlayerList(individualPlayerList);

        if (cache.getScoreboard() == null) {
            if (config.useSharedScoreboard) {
                cache.setScoreboard(player.getScoreboard());
            }
            else {
                cache.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }

        if (cache.getIndividualPrefix() == null && config.guildTagEnabled) {
            IndividualPrefix prefix = new IndividualPrefix(user);
            prefix.initialize();

            cache.setIndividualPrefix(prefix);
        }

        ConcurrencyManager concurrencyManager = FunnyGuilds.getInstance().getConcurrencyManager();
        concurrencyManager.postRequests(
                new PrefixGlobalUpdatePlayer(player),
                new DummyGlobalUpdateUserRequest(user),
                new RankUpdateUserRequest(user)
        );

        final FunnyGuildsChannelHandler channelHandler = this.plugin.getNmsAccessor().getPacketAccessor().getOrInstallChannelHandler(player);
        channelHandler.getPacketCallbacksRegistry().registerPacketCallback(new WarPacketCallbacks(player));

        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            this.plugin.getVersion().isNewAvailable(player, false);

            Region region = RegionUtils.getAt(player.getLocation());
            if (region == null || region.getGuild() == null) {
                return;
            }
            
            if (config.createEntityType != null) {
                GuildEntityHelper.spawnGuildHeart(region.getGuild(), player);
            }
        }, 30L);
    }
    
}
