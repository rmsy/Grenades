package tc.oc.grenades;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import tc.oc.tracker.Trackers;
import tc.oc.tracker.trackers.ExplosiveTracker;

import com.google.common.collect.Maps;

public class GrenadesListener implements Listener {
    public GrenadesListener(GrenadesPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new GrenadeDestructionRunner(this.grenades), 1, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if(!event.getPlayer().hasPermission("grenades.use")) return;
        if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack held = event.getItem();
            if(held != null && held.getType() == Material.FIREBALL) {
                Vector normal = VectorUtil.calculateLookVector(event.getPlayer().getLocation());
                normal.multiply(2);

                Location spawnLocation = event.getPlayer().getLocation().clone();
                spawnLocation.setY(spawnLocation.getY() + event.getPlayer().getEyeHeight());
                spawnLocation.add(normal);

                ItemStack itemStack = new ItemStack(Material.FIREBALL, 1);
                Item item = event.getPlayer().getWorld().dropItem(spawnLocation, itemStack);
                item.setVelocity(normal);

                int newAmount = held.getAmount() - 1;
                if(newAmount > 0) {
                    held.setAmount(newAmount);
                } else {
                    event.getPlayer().setItemInHand(null);
                }

                this.grenades.put(item, event.getPlayer());
            }
        }
    }

    private final @Nonnull Map<Item, Player> grenades = Maps.newHashMap();

    public static class GrenadeDestructionRunner implements Runnable {
        public GrenadeDestructionRunner(@Nonnull Map<Item, Player> grenades) {
            this.grenades = grenades;
        }

        @Override
        public void run() {
            for(Iterator<Map.Entry<Item, Player>> it = this.grenades.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Item, Player> entry = it.next();
                Item item = entry.getKey();
                Player player = entry.getValue();

                Location location = item.getLocation();

                Block block = location.getBlock();
                Location min = block.getLocation();
                Location max = block.getLocation().clone().add(new Vector(1, 1, 1));
                boolean removed = false;

                for(BlockFace face : BlockFace.values()) {
                    if(block.getRelative(face).getType() == Material.AIR) continue;
                    Vector rel = new Vector(face.getModX(), face.getModY(), face.getModZ()).multiply(0.8);
                    if(location.toVector().isInAABB(min.clone().add(rel).toVector(), max.clone().add(rel).toVector())) {
                        createExplosive(player, location);
                        it.remove();
                        item.remove();
                        removed = true;
                        break;
                    }
                }

                if(!removed && (location.getY() < 0 || item.getVelocity().lengthSquared() < 1)) {
                    it.remove();
                    item.remove();
                    break;
                }
            }
        }

        public static void createExplosive(@Nonnull Player player, @Nonnull Location location) {
            TNTPrimed tnt = (TNTPrimed) location.getWorld().spawnEntity(location, EntityType.PRIMED_TNT);
            tnt.setFuseTicks(0);
            tnt.setYield(2);

            Trackers.getManager().getTracker(ExplosiveTracker.class).setOwner(tnt, player);
        }

        private final @Nonnull Map<Item, Player> grenades;
    }
}
