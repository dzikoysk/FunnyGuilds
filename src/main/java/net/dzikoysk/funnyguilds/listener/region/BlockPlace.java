package net.dzikoysk.funnyguilds.listener.region;

import net.dzikoysk.funnyguilds.FunnyGuilds;
import net.dzikoysk.funnyguilds.data.configs.PluginConfiguration;
import net.dzikoysk.funnyguilds.system.protection.ProtectionSystem;
import net.dzikoysk.funnyguilds.util.nms.Reflections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BlockPlace implements Listener {

    private final PluginConfiguration config = FunnyGuilds.getInstance().getPluginConfiguration();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        Location blockLocation = block.getLocation();
        if (!ProtectionSystem.build(player, blockLocation, true)) {
            return;
        }

        // always cancel to prevent breaking other protection
        // plugins or plugins using BlockPlaceEvent (eg. special ability blocks)
        event.setCancelled(true);

        // disabled bugged-blocks or blacklisted item
        if (!this.config.buggedBlocks || this.config.buggedBlocksExclude.contains(type)) {
            return;
        }

        // remove one item from the player
        ItemStack itemInHand = event.getItemInHand();
        if ((itemInHand.getAmount() - 1) == 0) {
            // wondering why? because bukkit and you probably don't want dupe glitches
            if (Reflections.USE_PRE_9_METHODS) {
                player.setItemInHand(null);
            } else {
                itemInHand.setAmount(0);
            }
        } else {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        }

        // if the player is standing on the placed block add some velocity to prevent glitching
        // side effect: velocity with +y0.4 is like equivalent to jumping while building, just hold right click, that's real fun!
        Location playerLocation = player.getLocation();
        boolean sameColumn = (playerLocation.getBlockX() == blockLocation.getBlockX()) && (playerLocation.getBlockZ() == blockLocation.getBlockZ());
        double distanceUp = (playerLocation.getY() - blockLocation.getBlockY());
        boolean upToTwoBlocks = (distanceUp > 0) && (distanceUp <= 2);
        if (sameColumn && upToTwoBlocks) {
            player.setVelocity(new Vector(0, 0.4, 0));
        }

        // delay, because we cannot do {@link Block#setType(Material)} immediately
        Bukkit.getScheduler().runTaskLater(FunnyGuilds.getInstance(), () -> {

            // fake place for bugged block
            block.setType(type);

            // start timer and return the item to the player if specified to do so
            ItemStack returnItem = event.getItemInHand().clone();
            returnItem.setAmount(1);

            Bukkit.getScheduler().runTaskLater(FunnyGuilds.getInstance(), () -> {
                event.getBlockReplacedState().update(true);
                if (!player.isOnline()) {
                    return;
                }
                if (this.config.buggedBlockReturn) {
                    player.getInventory().addItem(returnItem);
                }
            }, this.config.buggedBlocksTimer);

        }, 0);
    }

}
