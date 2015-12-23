package io.github.galaipa.bb;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class GameListener implements Listener {
    public SuperBuildBattle plugin;

    public GameListener(SuperBuildBattle instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(PlayerInteractEvent event) {
        if (plugin.voting) {
            if (plugin.playing_players.contains(event.getPlayer())) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    Player p = event.getPlayer();
                    event.setCancelled(true);
                    if(plugin.player_that_voted.contains(p)){
                        p.sendMessage(ChatColor.RED + plugin.getTr("25"));
                        return;
                    }
                    if (plugin.teams2[plugin.current_voting_team].checkPlayer(p)) {
                        p.sendMessage(ChatColor.RED + plugin.getTr("26"));
                    } else if (p.getItemInHand().getType() == Material.STAINED_CLAY) {
                        String item_name = p.getItemInHand().getItemMeta().getDisplayName();
                        System.out.println(item_name);
                        if (item_name.equalsIgnoreCase(ChatColor.RED + plugin.getTr("35"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(0);
                            p.sendMessage(ChatColor.GREEN + "Your selection: " + item_name);
                            plugin.player_that_voted.add(p);
                        } else if (item_name.equalsIgnoreCase(ChatColor.RED + plugin.getTr("33"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(1);
                            p.sendMessage(ChatColor.GREEN + "Your selection: " + item_name);
                            plugin.player_that_voted.add(p);
                        } else if (item_name.equalsIgnoreCase(ChatColor.RED + plugin.getTr("32"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(2);
                            p.sendMessage(ChatColor.GREEN + "Your selection: " + item_name);
                            plugin.player_that_voted.add(p);
                        } else if (item_name.equalsIgnoreCase(ChatColor.GREEN + plugin.getTr("31"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(3);
                            p.sendMessage(ChatColor.GREEN + "Your selection:: " + item_name);
                            plugin.player_that_voted.add(p);
                        } else if (item_name.equalsIgnoreCase(ChatColor.GREEN + plugin.getTr("30"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(4);
                            p.sendMessage(ChatColor.GREEN + "Your selection: " + item_name);
                            plugin.player_that_voted.add(p);
                        } else if (item_name.equalsIgnoreCase(ChatColor.GREEN + plugin.getTr("36"))) {
                            plugin.teams2[plugin.current_voting_team].addPoint(5);
                            p.sendMessage(ChatColor.GREEN + "Your selection: " + item_name);
                            plugin.player_that_voted.add(p);
                        }
                    }
                }
            }
        }

    }

    @EventHandler
    public void PlayerCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.inGame) {
            Player p = event.getPlayer();
            if (plugin.playing_players.contains(p)) {
                if (event.getMessage().toLowerCase().startsWith("/buildbattle")) {
                } else if (event.getMessage().toLowerCase().startsWith("/bb")) {
                } else {
                    event.setCancelled(true);
                    p.sendMessage(ChatColor.GREEN + "[BuildBattle] " + ChatColor.RED + "You can't use command during the game");
                }

            }
        }
    }

    @EventHandler
    public void CuboidProtection(BlockBreakEvent event) {
        if (plugin.inGame) {
            if (plugin.playing_players.contains(event.getPlayer())) {
                if (!getTeam(event.getPlayer()).getCuboid().contains(event.getBlock()) || plugin.voting) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void CuboidProtection2(BlockPlaceEvent event) {
        if (plugin.inGame) {
            if (plugin.playing_players.contains(event.getPlayer())) {
                if (!getTeam(event.getPlayer()).getCuboid().contains(event.getBlock())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public Team getTeam(Player p) {
        for (Team t : plugin.teams) {
            if (t.getPlayer() == p) {
                return t;

            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (plugin.inGame) {
            if (plugin.playing_players.contains(event.getPlayer())) {
                Location l = event.getBlockClicked().getLocation();
                l.setY(l.getY() + 1);
                if (!getTeam(event.getPlayer()).getCuboid().contains(l)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

