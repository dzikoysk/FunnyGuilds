package net.dzikoysk.funnyguilds.command.user;

import net.dzikoysk.funnycommands.stereotypes.FunnyCommand;
import net.dzikoysk.funnyguilds.basic.guild.Guild;
import net.dzikoysk.funnyguilds.basic.user.User;
import net.dzikoysk.funnyguilds.command.IsOwner;
import net.dzikoysk.funnyguilds.data.configs.MessageConfiguration;
import net.dzikoysk.funnyguilds.event.FunnyEvent.EventCause;
import net.dzikoysk.funnyguilds.event.SimpleEventHandler;
import net.dzikoysk.funnyguilds.event.guild.member.GuildMemberDeputyEvent;
import org.bukkit.entity.Player;

public final class DeputyCommand {

    @FunnyCommand(
        name = "${user.deputy.name}",
        description = "${user.deputy.description}",
        aliases = "${user.deputy.aliases}",
        permission = "funnyguilds.deputy",
        completer = "online-players:3",
        acceptsExceeded = true,
        playerOnly = true
    )
    public void execute(MessageConfiguration messages, Player player, @IsOwner User owner, String[] args) {
        if (args.length < 1) {
            player.sendMessage(messages.generalNoNickGiven);
            return;
        }

        String name = args[0];
        User deputyUser = User.get(name);

        if (deputyUser == null) {
            player.sendMessage(messages.generalNotPlayedBefore);
            return;
        }
        
        if (owner.equals(deputyUser)) {
            player.sendMessage(messages.deputyMustBeDifferent);
            return;
        }

        Guild guild = owner.getGuild();
        Player deputyPlayer = deputyUser.getPlayer();

        if (!guild.getMembers().contains(deputyUser)) {
            player.sendMessage(messages.generalIsNotMember);
            return;
        }

        if (!SimpleEventHandler.handle(new GuildMemberDeputyEvent(EventCause.USER, owner, guild, deputyUser))) {
            return;
        }
        
        if (deputyUser.isDeputy()) {
            guild.removeDeputy(deputyUser);;
            player.sendMessage(messages.deputyRemove);
            
            if (deputyPlayer != null) {
                deputyPlayer.sendMessage(messages.deputyMember);
            }

            return;
        }

        guild.addDeputy(deputyUser);
        player.sendMessage(messages.deputySet);
        
        if (deputyPlayer != null) {
            deputyPlayer.sendMessage(messages.deputyOwner);
        }
    }

}
