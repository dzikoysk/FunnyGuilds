package net.dzikoysk.funnyguilds.system.protection;

import net.dzikoysk.funnyguilds.FunnyGuilds;
import net.dzikoysk.funnyguilds.basic.guild.Guild;
import net.dzikoysk.funnyguilds.basic.guild.Region;
import net.dzikoysk.funnyguilds.basic.guild.RegionUtils;
import net.dzikoysk.funnyguilds.basic.user.User;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.panda_lang.utilities.commons.function.Option;

import java.util.concurrent.TimeUnit;

public final class ProtectionSystem {

    private final FunnyGuilds plugin;

    public ProtectionSystem(FunnyGuilds plugin) {
        this.plugin = plugin;
    }

    public Option<Triple<Player, Guild, ProtectionType>> isProtected(Player player, Location location, boolean includeBuildLock) {
        if (player == null || location == null) {
            return Option.none();
        }
        
        if (player.hasPermission("funnyguilds.admin.build")) {
            return Option.none();
        }
        
        Region region = RegionUtils.getAt(location);

        if (region == null) {
            return Option.none();
        }
        
        Guild guild = region.getGuild();

        if (guild == null || guild.getName() == null) {
            return Option.none();
        }
        
        User user = plugin.getUserManager().getUser(player);

        if (!guild.getMembers().contains(user)) {
            return Option.of(Triple.of(player, guild, ProtectionType.UNAUTHORIZED));
        }

        if (includeBuildLock && !guild.canBuild()) {
            return Option.of(Triple.of(player, guild, ProtectionType.LOCKED));
        }

        if (location.equals(region.getHeart())) {
            Pair<Material, Byte> heartMaterial = plugin.getPluginConfiguration().createMaterial;
            return Option.when(heartMaterial != null && heartMaterial.getLeft() != Material.AIR, Triple.of(player, guild, ProtectionType.HEART));
        }

        return Option.none();
    }

    public void defaultResponse(Triple<Player, Guild, ProtectionType> result) {
        if (result.getRight() == ProtectionType.LOCKED) {
            sendRegionExplodeMessage(result.getLeft(), result.getMiddle());
        }
        else {
            result.getLeft().sendMessage(plugin.getMessageConfiguration().regionOther);
        }
    }

    private void sendRegionExplodeMessage(Player player, Guild guild) {
        player.sendMessage(plugin.getMessageConfiguration().regionExplodeInteract
                .replace("{TIME}",Long.toString(TimeUnit.MILLISECONDS.toSeconds(guild.getBuild() - System.currentTimeMillis())))
        );
    }

    public enum ProtectionType {
        UNAUTHORIZED,
        LOCKED,
        HEART
    }

}
