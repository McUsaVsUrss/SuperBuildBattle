package io.github.galaipa.bb;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

public class SuperBuildBattle extends JavaPlugin implements Listener {
    public static final Logger log = Logger.getLogger("Minecraft");
    public static String translation;
    public static YamlConfiguration yaml;
    private static Plugin plugin;
    public final Map<UUID, ItemStack[]> inv = new HashMap<>();
    public final Map<UUID, ItemStack[]> armor = new HashMap<>();
    public int player_online;
    public Economy econ = null;
    Team[] teams2;
    boolean inGame;
    boolean wg;
    int player_max;
    int taldeKopuruMinimoa;
    int time;
    int timeVote;
    boolean Vault;
    boolean Command;
    ArrayList<Player> playing_players = new ArrayList<>();
    ArrayList<Player> player_that_voted = new ArrayList<>();
    ArrayList<Team> teams = new ArrayList<>();
    Location SpawnPoint;
    ScoreboardManager manager;
    Scoreboard board;
    boolean voting;
    Objective objective;
    boolean admin;
    String timer = "";
    String reset = "";
    String taldekideak = "";
    List<String> gaiak = getConfig().getStringList("Gaiak");
    int current_voting_team;

    public static Plugin getPlugin() {
        return plugin;
    }

    public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        PacketPlayOutTitle packetPlayOutTimes = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
        connection.sendPacket(packetPlayOutTimes);

        if (subtitle != null) {
            subtitle = subtitle.replaceAll("%player%", player.getDisplayName());
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
            IChatBaseComponent titleSub = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitle + "\"}");
            PacketPlayOutTitle packetPlayOutSubTitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, titleSub);
            connection.sendPacket(packetPlayOutSubTitle);
        }

        if (title != null) {
            title = title.replaceAll("%player%", player.getDisplayName());
            title = ChatColor.translateAlternateColorCodes('&', title);
            IChatBaseComponent titleMain = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}");
            PacketPlayOutTitle packetPlayOutTitle = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleMain);
            connection.sendPacket(packetPlayOutTitle);
        }
    }

    public static String getTr(String path) {
        if (yaml.getString(path) == null) {
            path = "Message missing in the lang file. Contact Admin (N." + path + ")";
            return path;
        }
        String msg = yaml.getString(path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void onEnable() {
        defaultValues();
        getConfig().options().copyDefaults(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminGui(this), this);
        manager = Bukkit.getScoreboardManager();
        board = manager.getNewScoreboard();
        plugin = this;
        loadTranslations();
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            wg = true;
        } else {
            wg = false;
        }
        if ((getConfig().getBoolean("Rewards.Vault.Enabled"))) {
            if (!setupEconomy()) {
                log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
                getServer().getPluginManager().disablePlugin(this);
                Vault = false;
                return;
            } else {
                Vault = true;
            }
        } else {
            Vault = false;
        }
        if (getConfig().getBoolean("Rewards.Command.Enabled")) {
            Command = true;
            log.info("Command true");
        } else {
            Command = false;
        }
        objective = board.registerNewObjective(ChatColor.BOLD + "BuildBattle", "dummy");
    }

    @Override
    public void onDisable() {
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("buildbattle")) {
            if (!p.hasPermission("bb.user")) {
                sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.GREEN + "Super Build Battle by Galaipa");
            } else if (args.length < 1) {
                sendTitle(p, 20, 40, 20, "&2Super Build Battle", "By Galaipa");
                sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("2"));
            } else if (args[0].equalsIgnoreCase("join")) {
                join(p);
            } else if (args[0].equalsIgnoreCase("leave")) {
                allPlayers();
                if (!playing_players.contains(p)) {
                    sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("5"));
                } else {
                    resetPlayer(p);
                    sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("3"));
                    Broadcast(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + p.getName() + getTr("4"));
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("2"));
            }

        } else if (cmd.getName().equalsIgnoreCase("buildbattleadmin")) {
            if (!p.hasPermission("bb.admin")) {
                sender.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + "You don't have permission");
            } else if (args.length < 1) {
                admin = true;
                saveInventory(p);
                AdminGui.adminGui(p);
            } else if (args[0].equalsIgnoreCase("start")) {
                p.sendMessage(ChatColor.YELLOW + "[BuildBattle]" + ChatColor.GREEN + "You forced the game to start");
                hasiera();
            } else if (args[0].equalsIgnoreCase("stop")) {
                p.sendMessage(ChatColor.YELLOW + "[BuildBattle]" + ChatColor.GREEN + "You forced the game to stop");
                reset();
            } else if (args[0].equalsIgnoreCase("reset")) {

            } else if (args[0].equalsIgnoreCase("topic")) {
                String gaia = args[1];
                gaiak.add(gaia);
                getConfig().set("Gaiak", gaiak);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Added a new topic: " + gaia);
                return true;
            } else if (args[0].equalsIgnoreCase("removetopic")) {
                String gaia = args[1];
                gaiak.remove(gaia);
                getConfig().set("Gaiak", gaiak);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Removed topic: " + gaia);
                return true;
            } else if (args[0].equalsIgnoreCase("topiclist")) {
                sender.sendMessage(ChatColor.GREEN + "Topics: ");
                String s = "";
                for (String g : gaiak) {
                    s = s + ", " + g;
                }
                sender.sendMessage(s);
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid command");
                return true;
            }
               /* else if (cmd.getName().equalsIgnoreCase("buildbattleadmin")&& args[0].equalsIgnoreCase("public")) {
                    playing_players.add(p);
                    sender.sendMessage(ChatColor.GREEN +"[BuildBattle]" + ChatColor.YELLOW + "You have entered the public");
                    p.setGameMode(GameMode.SPECTATOR);
                    return true;
                }*/
        }
        return true;
    }

    public void defaultValues() {
        inGame = false;
        player_online = 0;
        player_max = 0;
        time = 0;
        voting = false;
        current_voting_team = -1;
        admin = false;
        playing_players.clear();
        teams.clear();
        player_that_voted.clear();
    }

    public void saveInventory(Player p) {
        inv.put(p.getUniqueId(), p.getInventory().getContents());
        armor.put(p.getUniqueId(), p.getInventory().getArmorContents());
        p.getInventory().setArmorContents(null);
        p.getInventory().clear();
    }

    public void returnInventory(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.getInventory().setContents(inv.get(p.getUniqueId()));
        p.getInventory().setArmorContents(armor.get(p.getUniqueId()));
        inv.remove(p.getUniqueId());
        armor.remove(p.getUniqueId());
        p.updateInventory();
    }

    public void reset() {
        for (Player p : playing_players) {
            p.setScoreboard(manager.getNewScoreboard());
            returnInventory(p);
            p.teleport(SpawnPoint);
            p.setGameMode(GameMode.SURVIVAL);
        }
        for (Team t : teams) {
            t.resetArenas();
            if (wg == true) {
                WorldGuardOptional.WGregionRM(t.getID());
            }
        }
        for (String s : board.getEntries()) {
            board.resetScores(s);
        }
        board.resetScores(ChatColor.DARK_GREEN.BOLD + "BuildBattle");
        board.clearSlot(DisplaySlot.SIDEBAR);
        defaultValues();
    }

    public void resetPlayer(Player p) {
        Team t2 = null;
        for (Team t : teams) {
            if (t.checkPlayer(p) == true) {
                t2 = t;
                break;
            }
        }
        if (inGame != true) {
            p.teleport(SpawnPoint);
            player_online--;
            teams.remove(t2);
            teams2 = new Team[teams.size()];
            teams2 = teams.toArray(teams2);
            returnInventory(p);
            playing_players.remove(p);
            return;
        }
        player_online--;
        returnInventory(p);
        p.setGameMode(GameMode.SURVIVAL);
        p.setScoreboard(manager.getNewScoreboard());
        p.teleport(SpawnPoint);
        playing_players.remove(p);
        t2.resetArenas();
        if (wg == true) {
            WorldGuardOptional.WGregionRM(t2.getID());
        }
        teams.remove(t2);
        teams2 = new Team[teams.size()];
        teams2 = teams.toArray(teams2);
        if (player_online == 0 && inGame == true) {
            board.resetScores(ChatColor.DARK_GREEN.BOLD + "BuildBattle");
            board.clearSlot(DisplaySlot.SIDEBAR);
            defaultValues();
        } else if (player_online == 1 && inGame == true) {
            teams2[0].getPlayer().sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("6"));
            resetPlayer(teams2[0].getPlayer());
        }

    }

    public synchronized void hasiera() {
        if (inGame) return;
        teams2 = new Team[teams.size()];
        teams2 = teams.toArray(teams2);
        loadSelection();
        allPlayers();
        Random random = new Random();
        String gaia = gaiak.get(random.nextInt(gaiak.size()));
        start(gaia);
    }

    public void SaveSpawn(Location l, int time, int taldeKopuruMaximoa, int taldeKopuruMinimoa, int timeVote) {
        getConfig().set("Time", time);
        getConfig().set("VotingTime", timeVote);
        getConfig().set("MinPlayers", taldeKopuruMinimoa);
        getConfig().set("MaxPlayers", taldeKopuruMaximoa);
        getConfig().set("Spawn.World", l.getWorld().getName());
        getConfig().set("Spawn.X", l.getX());
        getConfig().set("Spawn.Y", l.getY());
        getConfig().set("Spawn.Z", l.getZ());
        saveConfig();
    }

    public void saveSelection(int id, Location l1, Location l2) {
        getConfig().set("Region." + id + ".World", l1.getWorld().getName());
        getConfig().set("Region." + id + ".Min.x", l1.getX());
        getConfig().set("Region." + id + ".Min.y", l1.getY());
        getConfig().set("Region." + id + ".Min.z", l1.getZ());

        getConfig().set("Region." + id + ".Max.x", l2.getX());
        getConfig().set("Region." + id + ".Max.y", l2.getY());
        getConfig().set("Region." + id + ".Max.z", l2.getZ());
        saveConfig();
    }

    public void loadSelection() {
        player_max = getConfig().getInt("MaxPlayers");
        taldeKopuruMinimoa = getConfig().getInt("MinPlayers");
        time = getConfig().getInt("Time");
        if (getConfig().get("VotingTime") != null) {
            timeVote = getConfig().getInt("VotingTime");
        } else {
            timeVote = 30;
        }
        String w22 = getConfig().getString("Spawn.World");
        Double x22 = getConfig().getDouble("Spawn.X");
        Double y22 = getConfig().getDouble("Spawn.Y");
        Double z22 = getConfig().getDouble("Spawn.Z");
        SpawnPoint = new Location(Bukkit.getServer().getWorld(w22), x22, y22, z22);
        for (Team team : teams) {
            String w = getConfig().getString("Region." + team.getID() + ".World");
            Double x = getConfig().getDouble("Region." + team.getID() + ".Min.x");
            Double y = getConfig().getDouble("Region." + team.getID() + ".Min.y");
            Double z = getConfig().getDouble("Region." + team.getID() + ".Min.z");
            Double x2 = getConfig().getDouble("Region." + team.getID() + ".Max.x");
            Double y2 = getConfig().getDouble("Region." + team.getID() + ".Max.y");
            Double z2 = getConfig().getDouble("Region." + team.getID() + ".Max.z");
            Location l1 = new Location(Bukkit.getServer().getWorld(w), x, y, z);
            Location l2 = new Location(Bukkit.getServer().getWorld(w), x2, y2, z2);
            team.addRegion(l1, l2);
            if (wg) {
                WorldGuardOptional.WGregion(Bukkit.getServer().getWorld(w), x, y, z, x2, y2, z2, team.getID());
            }
        }
    }

    public void allPlayers() {
        for (Team team : teams) {
            Player a = team.getPlayer();
            if (!playing_players.contains(a)) {
                playing_players.add(a);
            }
        }
    }

    public void join(Player p) {
        if (player_online == 0) {
            loadSelection();
        }
        allPlayers();
        for (Player a : playing_players) {
            if (a == p) {
                p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("7"));
                return;
            }
        }
        if (inGame) {
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("8"));
        } else if (player_online == player_max) {
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("9"));
        } else if (!p.hasPermission("bb.user")) {
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.RED + getTr("1"));
        } else {
            // if(args.length == 1){
            player_online++;
            Broadcast(ChatColor.GREEN + "[BuildBattle]" + ChatColor.GREEN + p.getName() + getTr("10") + "(" + player_online + "/" + player_max + ")");
            Team team = new Team(player_online);
            teams.add(team);
            team.addPlayer(p);
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.YELLOW + getTr("11"));
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.YELLOW + getTr("12") + ": " + player_online + "/" + player_max);
            //inventory
            inv.put(p.getUniqueId(), p.getInventory().getContents());
            armor.put(p.getUniqueId(), p.getInventory().getArmorContents());
            p.getInventory().setArmorContents(null);
            //  }
                         /*   else{
                                Player p2 = Bukkit.getServer().getPlayer(args[1]);
                                player_online++;
                                Team team = new Team(player_online);
                                teams.add(team);
                                team.addPlayers(p,p2);
                                p.sendMessage(ChatColor.GREEN +"[BuildBattle]" + ChatColor.YELLOW + "The game will start soon");
                                p.sendMessage(ChatColor.GREEN +"[BuildBattle]" + ChatColor.YELLOW + "Number of players:" + player_online);
                                p2.sendMessage(ChatColor.GREEN +"[BuildBattle]" + ChatColor.YELLOW + "The game will start soon");
                                p2.sendMessage(ChatColor.GREEN +"[BuildBattle]" + ChatColor.YELLOW + "Your team mate: " + sender.getName());
                        }*/
            if (player_online == taldeKopuruMinimoa) {
                minimunReached();
            }
        }
    }

    public void minimunReached() {
        new BukkitRunnable() {
            int a = 0;

            @Override
            public void run() {
                if (player_max == taldeKopuruMinimoa) {
                    this.cancel();
                    hasiera();
                } else if (a == 10) {
                    this.cancel();
                    hasiera();
                } else {
                    a++;
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    public void teleport() {
        for (Team team : teams) {
            team.getPlayer().teleport(team.getSpawnPoint());
            team.getPlayer().setGameMode(GameMode.CREATIVE);
            team.getPlayer().getWorld().playSound(team.getPlayer().getLocation(), Sound.NOTE_PLING, 10, 1);
            saveInventory(team.getPlayer());
            /*Player a2 = team.getPlayer2();
            if (a2 != null) {
                team.getPlayer2().teleport(team.getSpawnPoint());
                team.getPlayer2().setGameMode(GameMode.CREATIVE);
            }*/
        }
    }

    public void start(final String gaia) {
        if (inGame) return;
        inGame = true;
        Broadcast(ChatColor.GREEN + getTr("13"));
        BukkitRunnable task = new BukkitRunnable() {
            int countdown = 10;

            public void run() {
                for (Player p : playing_players) {
                    p.setLevel(countdown);
                    p.sendMessage(ChatColor.GREEN + " " + countdown);
                    p.getWorld().playSound(p.getLocation(), Sound.NOTE_STICKS, 10, 1);
                    sendTitle(p, 20, 40, 20, Integer.toString(countdown), "");
                }
                countdown--;
                if (countdown < 0) {
                    this.cancel();
                    Broadcast(ChatColor.YELLOW + "----------------------------------------------------");
                    Broadcast(ChatColor.BOLD + "" + ChatColor.GREEN + "Super Build Battle ");
                    Broadcast(ChatColor.GREEN + getTr("15") + " " + time + " " + getTr("16"));
                    Broadcast(ChatColor.GREEN + "Topic: " + ChatColor.YELLOW + gaia);
                    Broadcast(ChatColor.YELLOW + "----------------------------------------------------");
                    sendTitleAll(20, 40, 20, ChatColor.GREEN + gaia, getTr("14"));
                    teleport();
                    ScoreBoard(gaia);
                }
            }
        };
        task.runTaskTimer(this, 0L, 20L);
    }

    public void ScoreBoard(final String gaia) {
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Score gaiaa = objective.getScore(ChatColor.GREEN + getTr("17") + ": " + ChatColor.YELLOW + gaia);
        gaiaa.setScore(3);

        new BukkitRunnable() {
            int seconds = 0;
            int minutes = time;

            @Override
            public void run() {
                if (minutes == 10 && seconds == 2 || minutes == 5 && seconds == 2 || minutes == 4 && seconds == 2 || minutes == 3 && seconds == 2 || minutes == 2 && seconds == 2 || minutes == 1 && seconds == 2) {
                    sendTitleAll(20, 40, 20, Integer.toString(minutes), getTr("29"));
                }
                if (minutes == 0 && seconds == 0) {
                    voting = true;
                    voting();
                    this.cancel();
                } else if (seconds == 0) {
                    seconds = 60;
                    minutes = minutes - 1;
                } else {
                    seconds = seconds - 1;
                }
                board.resetScores(timer);
                timer = ChatColor.GREEN + getTr("18") + ": " + minutes + ":" + seconds;
                Score denbora = objective.getScore(ChatColor.GREEN + getTr("18") + ": " + minutes + ":" + seconds);
                denbora.setScore(2);
                for (Player p : playing_players) {
                    p.setScoreboard(board);
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    public void ScoreBoard2() {
        board.resetScores(timer);
        board.resetScores(reset);
        reset = ChatColor.GREEN + getTr("19") + ": " + ChatColor.YELLOW + (current_voting_team + 1);
        board.resetScores(taldekideak);
        taldekideak = ChatColor.YELLOW + teams2[current_voting_team].getPlayerString();
        Objective objective = board.getObjective(DisplaySlot.SIDEBAR);
        Score denbora = objective.getScore(ChatColor.GREEN + getTr("19") + ": " + ChatColor.YELLOW + (current_voting_team + 1));
        denbora.setScore(2);
        Score taldekideak = objective.getScore(ChatColor.YELLOW + teams2[current_voting_team].getPlayerString());
        taldekideak.setScore(1);
    }

    public void voting() {
        current_voting_team = -1;
        new BukkitRunnable() {
            //int current = 0;
            public void run() {
                if (current_voting_team >= teams.size() - 1) {
                    Broadcast(ChatColor.GREEN + getTr("20"));
                    winner();
                    this.cancel();
                } else {
                    current_voting_team++;
                    player_that_voted.clear();
                    InventoryMenu();
                    ScoreBoard2();
                    Broadcast(ChatColor.YELLOW + getTr("19") + ": " + teams2[current_voting_team].getPlayerString());
                    for (Player p : playing_players) {
                        p.teleport(teams2[current_voting_team].getSpawnPoint());
                        sendTitleAll(20, 40, 20, teams2[current_voting_team].getPlayerString(), "");
                        p.getWorld().playSound(p.getLocation(), Sound.NOTE_PLING, 10, 1);
                    }
                    /*if (current == 0) {
                        Broadcast(ChatColor.GREEN + getTr("21"));

                        current++;
                    } else {
                        Broadcast(ChatColor.YELLOW + getTr("19") + ": " + teams2[current_voting_team].getPlayerString());
                        current++;
                    }*/
                }

            }
        }.runTaskTimer(this, 0, 20 * timeVote);
    }

    public void InventoryMenu() {
        for (Player p : playing_players) {
            p.setScoreboard(board);
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
    }

    public void winner() {
        Team taldeIrabazlea = null;
        Team talde2 = null;
        Team talde3 = null;
        List<Winners> users = new ArrayList<>();
        for (Team t : teams) {
            users.add(new Winners(t, t.getPoint()));
        }
        Collections.sort(users);
        for (Winners n : users) {
            if (taldeIrabazlea == null) {
                taldeIrabazlea = n.getName();
            } else if (talde2 == null) {
                talde2 = n.getName();
            } else if (talde3 == null) {
                talde3 = n.getName();
            }
        }
        final Team t = taldeIrabazlea;
        Broadcast(ChatColor.YELLOW + "----------------------------------------------------");
        Broadcast(ChatColor.BOLD + "" + ChatColor.GREEN + "Super Build Battle ");
        Broadcast(ChatColor.GREEN + "1: " + taldeIrabazlea.getPlayerString() + "(" + taldeIrabazlea.getPoint() + " " + getTr("24") + ")");
        if (player_online > 1) {
            Broadcast(ChatColor.GREEN + "2: " + talde2.getPlayerString() + "(" + talde2.getPoint() + " " + getTr("24") + ")");
        }
        if (player_online > 2) {
            Broadcast(ChatColor.GREEN + "2: " + talde3.getPlayerString() + "(" + talde3.getPoint() + " " + getTr("24") + ")");
        }
        Broadcast(ChatColor.YELLOW + "----------------------------------------------------");
        Rewards(taldeIrabazlea, "Winner");
        if (player_online > 1) {
            Rewards(talde2, "Second");
        }
        if (player_online > 2) {
            Rewards(talde3, "Third");
        }
        for (Player p : playing_players) {
            p.teleport(taldeIrabazlea.getSpawnPoint());
            if (p != taldeIrabazlea.getPlayer() && p != talde2.getPlayer() && p != talde3.getPlayer()) {
                Rewards(p, "Rest");
            }
        }

        new BukkitRunnable() {
            int zenbat = 0;

            @Override
            public void run() {
                Firework f = t.getWorld().spawn(t.getCuboid().getCenter(), Firework.class);
                FireworkMeta fm = f.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder().flicker(false).trail(true).with(Type.STAR).withColor(Color.GREEN).withFade(Color.BLUE).build());
                fm.setPower(3);
                f.setFireworkMeta(fm);
                zenbat++;
                if (zenbat == 10) {
                    reset();
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 20);
    }

    public void Broadcast(String s) {
        allPlayers();
        for (Player p : playing_players) {
            p.sendMessage(s);
        }
    }

    public void sendTitleAll(Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        for (Player p : playing_players) {
            sendTitle(p, fadeIn, stay, fadeOut, title, subtitle);
        }
    }

    //LANGUAGES:
    private void loadTranslations() {
        copyTranslation("en");
        translation = getConfig().getString("Language");
        File languageFile = new File(getDataFolder() + File.separator + "lang" + File.separator + translation + ".yml");
        //Settings default if language in config isn't found
        if (!languageFile.exists()) {
            getLogger().info("Could not find language file, language set to english");
            translation = "en";
        }
        getLogger().info(translation);
        yaml = YamlConfiguration.loadConfiguration(languageFile);
    }

    private void copyTranslation(String trans) {
        File file = new File(getDataFolder().getAbsolutePath() + File.separator + "lang" + File.separator + trans + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            copy(getResource(trans + ".yml"), file);
        }
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public void giveVaultRewards(Player p, int points) {
        EconomyResponse r = econ.depositPlayer(p, points);
        if (r.transactionSuccess()) {
            p.sendMessage(ChatColor.GREEN + "[BuildBattle]" + ChatColor.GREEN + getTr("34") + " " + points + " " + "$");
        }
    }

    public void Rewards(Team t, String s) {
        Player p = t.getPlayer();
        if (Vault) {
            giveVaultRewards(p.getPlayer(), getConfig().getInt("Rewards.Vault." + s));
        }
        if (Command) {
            getServer().dispatchCommand(getServer().getConsoleSender(), (getConfig().getString("Rewards.Command." + s)).replace("$player$", p.getName()));
        }
    }

    public void Rewards(Player p, String s) {
        if (Vault) {
            giveVaultRewards(p.getPlayer(), getConfig().getInt("Rewards.Vault." + s));
        }
        if (Command) {
            getServer().dispatchCommand(getServer().getConsoleSender(), (getConfig().getString("Rewards.Command." + s)).replace("$player$", p.getName()));
        }
    }

    public Team getTeam(Player p) {
        for (Team t : teams) {
            if (t.getPlayer() == p) {
                return t;

            }
        }
        return null;
    }
}

       

   
    
