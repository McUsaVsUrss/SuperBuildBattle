package sbb;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import spigboard.Spigboard;
import spigboard.SpigboardEntry;
import java.util.*;

import static sbb.SuperBuildBattle.getTr;

public class Arena {
    public int id;
    private boolean started = false;
    public int maxPlayers, minPlayers, time, votingtime;
    List<SbbPlayer> players = new ArrayList<>();
    HashMap<Player, Integer> player_that_voted = new HashMap<>();
    Location lobby;
    Cuboid[] cuboid;
    Boolean inGame = false, voting = false;
    String theme;
    Spigboard SpigBoard;
    SbbPlayer currentPlayer;
    SuperBuildBattle plugin = SuperBuildBattle.getInstance();

    public Arena(int id, int minPlayers, int maxPlayers, int time, int votingTime, Location lobby, Cuboid[] cuboid) {
        this.id = id;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.time = time;
        this.votingtime = votingTime;
        this.lobby = lobby;
        this.cuboid = cuboid;
    }

    public int getID() {
        return this.id;
    }

    public List<SbbPlayer> getPlayers() {
        return this.players;
    }

    public SbbPlayer getArenaPlayer(Player p) {
        for (SbbPlayer j : players) {
            if (j.getPlayer().equals(p)) {
                return j;
            }
        }
        return null;
    }

    public boolean contains(Player p){
        for (SbbPlayer j : players) {
            if (j.getPlayer().equals(p)) {
                return true;
            }
        }
        return false;
    }

    public void assignArenas() {
        for (SbbPlayer j2 : players) {
            j2.addRegion(cuboid[j2.getID() - 1]);
        }
    }

    public void Broadcast(String msg) {
        for (SbbPlayer j : players) {
            j.getPlayer().sendMessage(msg);
        }
    }

    public void sendTitleAll(Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        for (SbbPlayer j : players) {
            ArenaManager.sendTitle(j.getPlayer(), fadeIn, stay, fadeOut, title, subtitle);
        }
    }

    public synchronized void start() {
        if(started) return;
        started = true;
        assignArenas();
        //  Broadcast("AssignArenas OK");
        theme = ArenaManager.getManager().getRandomTheme();
        Broadcast(ChatColor.GREEN + getTr("13"));
        new BukkitRunnable() {
            int countdown = 10;
            @Override
            public void run() {
                for (SbbPlayer j : getPlayers()) {
                    Player p = j.getPlayer();
                    p.setLevel(countdown);
                    //   p.sendMessage(ChatColor.GREEN + " " + countdown);
                    p.getWorld().playSound(p.getLocation(), Sound.NOTE_STICKS, 10, 1);
                    ArenaManager.sendTitle(p, 20, 40, 20, Integer.toString(countdown), "");
                }
                countdown--;
                if (countdown < 0) {
                    Broadcast(ChatColor.GREEN + "-----------------------------------------------");
                    Broadcast(ChatColor.BOLD.toString());
                    Broadcast(ChatColor.WHITE + "                         lSuper Build Battle");
                    Broadcast(ChatColor.GREEN + "       " + getTr("15") + " " + time + " " + getTr("16"));
                    Broadcast(ChatColor.GREEN + "         " + getTr("17") + ": " + ChatColor.YELLOW + theme);
                    Broadcast(ChatColor.BOLD.toString());
                    Broadcast(ChatColor.GREEN + "-----------------------------------------------");
                    sendTitleAll(20, 40, 20, ChatColor.GREEN + theme, getTr("14"));
                    for (SbbPlayer j : getPlayers()) {
                        Player p = j.getPlayer();
                        p.teleport(j.getSpawnPoint());
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            p.setGameMode(GameMode.CREATIVE);
                            p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 10, 1);
                            InGameGui.giveUserGui(p);
                            InGameGui.userGui();
                            if (plugin.getConfig().getBoolean("StartCommand.Enabled")) {
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), (plugin.getConfig().getString("StartCommand.Command")).replace("$player$", p.getName()));
                            }
                        }, 5L);
                    }
                    inGame = true;
                    cancel();
                    Building();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void Building() {
        SpigBoard = new Spigboard(ChatColor.BOLD + "BuildBattle");
        SpigBoard.add("theme", ChatColor.GREEN + getTr("17") + ": " + ChatColor.YELLOW + theme, 4);
        for (SbbPlayer j : getPlayers()) {
            SpigBoard.add(j.getPlayer());
        }
        new BukkitRunnable() {
            int seconds = 0;
            int minutes = time;

            @Override
            public void run() {
                if (minutes == 10 && seconds == 2 || minutes == 5 && seconds == 2 || minutes == 4 && seconds == 2 || minutes == 3 && seconds == 2 || minutes == 2 && seconds == 2 || minutes == 1 && seconds == 2) {
                    sendTitleAll(20, 40, 20, Integer.toString(minutes), getTr("29"));
                }
                if (minutes == 0 && seconds == 0) {
                    cancel();
                    voting = true;
                    voting();
                } else if (seconds == 0) {
                    seconds = 60;
                    minutes = minutes - 1;
                } else {
                    seconds = seconds - 1;
                }

                if (seconds < 10 && seconds >= 0) {
                    //Ma quanto eri ubriaco quando hai scritto questo codice??!?!?!?
                    // seconds = 0 + seconds;
                    
                    String timer2 = ChatColor.GREEN + getTr("18") + ": " + ChatColor.YELLOW + minutes + ":" + "0" + seconds;
                    SpigboardEntry score = SpigBoard.getEntry("timer");
                    if (score != null) {
                        score.update(timer2);
                    } else {
                        SpigBoard.add("timer", timer2, 2);
                    }
                } else {
                    String timer2 = ChatColor.GREEN + getTr("18") + ": " + ChatColor.YELLOW + minutes + ":" + seconds;
                    SpigboardEntry score = SpigBoard.getEntry("timer");
                    if (score != null) {
                        score.update(timer2);
                    } else {
                        SpigBoard.add("timer", timer2, 2);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void voting() {
        SpigBoard.remove(SpigBoard.getEntry("timer"));
        for (SbbPlayer j : getPlayers()) {
            Player p = j.getPlayer();
            Inventory inv = p.getInventory();
            inv.clear();
            inv.addItem(AdminGui.item(Material.STAINED_GLASS_PANE, 14, 1, ChatColor.RED + getTr("37")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 14, 1, ChatColor.RED + getTr("35")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 1, 1, ChatColor.RED + getTr("33")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 6, 1, ChatColor.RED + getTr("32")));
            inv.addItem(AdminGui.item(Material.STAINED_GLASS_PANE, 0, 1, getTr("37")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 4, 1, ChatColor.GREEN + getTr("31")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 5, 1, ChatColor.GREEN + getTr("30")));
            inv.addItem(AdminGui.item(Material.STAINED_CLAY, 13, 1, ChatColor.GREEN + getTr("36")));
            inv.addItem(AdminGui.item(Material.STAINED_GLASS_PANE, 13, 1, ChatColor.GREEN + getTr("37")));
            p.updateInventory();
        }
        new BukkitRunnable() {
            int current = 0;
            @Override
            public void run() {
                if(currentPlayer != null){
                    for (SbbPlayer j2 : players) {
                        if (player_that_voted.containsKey(j2.getPlayer())) {
                            currentPlayer.addPoint(player_that_voted.get(j2.getPlayer()));
                        }
                    }
                }

                player_that_voted.clear();
                if (current >= players.size()) {
                    winner();
                    this.cancel();
                }else{
                    currentPlayer = getPlayer(current);
                    for (SbbPlayer j : getPlayers()) {
                        Player p = j.getPlayer();
                        Location tp = currentPlayer.getSpawnPoint();
                        tp.setY(tp.getY()+5);
                        p.teleport(tp);
                        sendTitleAll(20, 40, 20, currentPlayer.getPlayerString(), "");
                        p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 10, 1);
                    }
                    String scoreboardstring = ChatColor.GREEN + getTr("19") + ": " + ChatColor.YELLOW + currentPlayer.getPlayerString();
                    SpigboardEntry score = SpigBoard.getEntry("playervote");
                    if (score != null) {
                        score.update(scoreboardstring);
                    } else {
                        SpigBoard.add("playervote", scoreboardstring, 2);
                    }
                    if (current == 0) {
                        Broadcast(ChatColor.GREEN + "-----------------------------------------------");
                        Broadcast(ChatColor.BOLD.toString());
                        Broadcast(ChatColor.WHITE + "                         Voting");
                        Broadcast(ChatColor.GREEN + "        " + getTr("21"));
                        Broadcast(ChatColor.BOLD.toString());
                        Broadcast(ChatColor.GREEN + "-----------------------------------------------");
                    }
                    Broadcast(ChatColor.YELLOW + getTr("19") + ": " + currentPlayer.getPlayerString());
                    current++;
                }

            }
        }.runTaskTimer(plugin, 0, 20 * votingtime);
    }

    public void winner() {
        SbbPlayer winner1 = null;
        SbbPlayer winner2 = null;
        SbbPlayer winner3 = null;
        List<Winners> users = new ArrayList<>();
        for (SbbPlayer t : players) {
            users.add(new Winners(t, t.getPoint()));
        }
        if(users.isEmpty()) return;
        Collections.sort(users);
        for (Winners n : users) {
            if (winner1 == null) {
                winner1 = n.getName();
            } else if (winner2 == null) {
                winner2 = n.getName();
            } else if (winner3 == null) {
                winner3 = n.getName();
            }
        }
        Broadcast(ChatColor.GREEN + "------------------------------------------------");
        Broadcast(ChatColor.BOLD.toString());
        Broadcast(ChatColor.WHITE + "                         Super Build Battle");
        Broadcast(ChatColor.GREEN + "       " + getTr("20"));
        Broadcast(ChatColor.YELLOW + "       " + "1: " + ChatColor.GREEN + winner1.getPlayerString() + "(" + winner1.getPoint() + " " + getTr("24") + ")");
        if (winner2 != null) {
            Broadcast(ChatColor.YELLOW + "       " + "2: " + ChatColor.GREEN + winner2.getPlayerString() + "(" + winner2.getPoint() + " " + getTr("24") + ")");
        }
        if (winner3 != null) {
            Broadcast(ChatColor.YELLOW + "       " + "3: " + ChatColor.GREEN + winner3.getPlayerString() + "(" + winner3.getPoint() + " " + getTr("24") + ")");
        }
        Broadcast(ChatColor.BOLD.toString());
        Broadcast(ChatColor.GREEN + "------------------------------------------------");
        ArenaManager.getManager().Rewards(winner1, "Winner");
        if (players.size() > 1) {
            ArenaManager.getManager().Rewards(winner2, "Second");
        }
        if (players.size() > 2) {
            ArenaManager.getManager().Rewards(winner3, "Third");
        }

        for (SbbPlayer j : players) {
            Player p = j.getPlayer();
            p.teleport(winner1.getSpawnPoint());
            if (p != winner1.getPlayer() && (winner2 == null || p != winner2.getPlayer()) && (winner3 == null || p != winner3.getPlayer())) {
                ArenaManager.getManager().Rewards(p, "Rest");
            }
        }
        final SbbPlayer finalWinner = winner1;
        new BukkitRunnable() {
            int zenbat = 0;
            @Override
            public void run() {
                Firework f = finalWinner.getWorld().spawn(finalWinner.getCuboid().getCenter(), Firework.class);
                FireworkMeta fm = f.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder().flicker(false).trail(true).with(FireworkEffect.Type.STAR).withColor(Color.GREEN).withFade(Color.BLUE).build());
                fm.setPower(3);
                f.setFireworkMeta(fm);
                zenbat++;
                if (zenbat == 5) {
                    cancel();
                    reset();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void reset() {
        Iterator<SbbPlayer> it = players.iterator();
        while (it.hasNext()) {
            SbbPlayer j = it.next();
            ArenaManager.getManager().removePlayer(j.getPlayer());
        }
        SpigBoard = null;
        inGame = false;
        time = 0;
        voting = false;
        currentPlayer = null;
        players.clear();
        player_that_voted.clear();
        started = false;

    }

    public void minimunReached() {
        new BukkitRunnable() {
            int a = 0;

            @Override
            public void run() {
                if (maxPlayers == minPlayers) {
                    cancel();
                    start();
                } else if (a == 10) {
                    cancel();
                    start();
                } else {
                    a++;
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public SbbPlayer getPlayer(int index) {
        if(index < 0 || index >= players.size()) return null;
        return players.get(index);
    }

}
