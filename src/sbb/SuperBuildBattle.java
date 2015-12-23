package sbb;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class SuperBuildBattle extends JavaPlugin {
    //LANGUAGES:
    public static String translation;
    public static YamlConfiguration yaml;
    public Economy econ = null;
    private File languageFile;

    public static SuperBuildBattle getInstance() {
        return JavaPlugin.getPlugin(SuperBuildBattle.class);
    }

    public static String getTr(String path) {
        if (yaml.getString(path) == null) {
            String msg = "Message missing in the lang file. Contact Admin (N." + path + ")";
            return msg;
        } else {
            String msg = ChatColor.translateAlternateColorCodes('&', yaml.getString(path));
            return msg;
        }
    }

    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        getConfig().options().copyHeader(true);
        saveConfig();
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(), this);
        getServer().getPluginManager().registerEvents(new AdminGui(), this);
        getServer().getPluginManager().registerEvents(new InGameGui(), this);
        loadTranslations();

        if ((getConfig().getBoolean("Rewards.Vault.Enabled"))) {
            if (!setupEconomy()) {

                getServer().getPluginManager().disablePlugin(this);
                ArenaManager.Vault = false;
                return;
            } else {
                ArenaManager.Vault = true;
            }
        } else {
            ArenaManager.Vault = false;
        }
        ArenaManager.Command = getConfig().getBoolean("Rewards.Command.Enabled");
        ArenaManager.debug = getConfig().getBoolean("Debug");
        ArenaManager.WorldGuarda = getServer().getPluginManager().getPlugin("WorldGuard") != null;
        ArenaManager.getManager().loadArenas();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player p = (Player) sender;
        if (cmd.getName().equalsIgnoreCase("buildbattle")) {
            if (!p.hasPermission("bb.user")) {
                sender.sendMessage(ChatColor.GREEN + "[Build Battle] " + ChatColor.GREEN + "Super Build Battle by Galaipa");
            } else if (args.length < 1) {
                ArenaManager.sendTitle(p, 20, 40, 20, "&2Super Build Battle", "By Galaipa");
                sender.sendMessage(ChatColor.GREEN + "[Build Battle] " + ChatColor.RED + "Super Build Battle by Galaipa");
            } else if (args[0].equalsIgnoreCase("join")) {
                ArenaManager.getManager().addPlayer(p, p.getLocation());
            } else if (args[0].equalsIgnoreCase("leave")) {
                ArenaManager.getManager().removePlayer(p);
                return true;
            } else {
                sender.sendMessage(ChatColor.GREEN + "[Build Battle] " + ChatColor.RED + getTr("2"));
            }

        } else if (cmd.getName().equalsIgnoreCase("buildbattleadmin")) {
            if (!p.hasPermission("bb.admin")) {
                sender.sendMessage(ChatColor.GREEN + "[Build Battle] " + ChatColor.RED + "You don't have permission");
            } else if (args.length == 0) {
                ArenaManager.admin = true;
                AdminGui.arenaGui(p);
            } else if (args[0].equalsIgnoreCase("start")) {
                p.sendMessage(ChatColor.YELLOW + "[Build Battle] " + ChatColor.GREEN + "You forced the game to start");
                ArenaManager.getManager().getArena(Integer.parseInt(args[1])).start();
            } else if (args[0].equalsIgnoreCase("stop")) {
                p.sendMessage(ChatColor.YELLOW + "[Build Battle] " + ChatColor.GREEN + "You forced the game to stop");
                //reset();
            } else if (args[0].equalsIgnoreCase("reset")) {

            } else if (args[0].equalsIgnoreCase("topic")) {
                String gaia = args[1];
                List<String> themes = getConfig().getStringList("Themes");
                themes.add(gaia);
                getConfig().set("Themes", themes);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Added a new topic: " + gaia);
                return true;
            } else if (args[0].equalsIgnoreCase("removetopic")) {
                List<String> themes = getConfig().getStringList("Themes");
                String gaia = args[1];
                themes.remove(gaia);
                getConfig().set("Gaiak", themes);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Removed topic: " + gaia);
                return true;
            } else if (args[0].equalsIgnoreCase("topiclist")) {
                List<String> themes = getConfig().getStringList("Themes");
                sender.sendMessage(ChatColor.GREEN + "Topics: ");
                String s = "";
                for (String g : themes) {
                    s = s + ", " + g;
                }
                sender.sendMessage(s);
                return true;
            } else if (args[0].equalsIgnoreCase("cmdwhitelist")) {
                ArrayList<String> list = (ArrayList<String>) getConfig().getStringList("CmdWhitelist");
                if (args[1].equalsIgnoreCase("add") && args.length >= 2) {
                    list.add(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "Command added to whitelist");
                } else if (args[1].equalsIgnoreCase("remove") && args.length >= 2) {
                    list.remove(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "Command removed from whitelist");
                } else if (args[1].equalsIgnoreCase("list")) {
                    String a = "CmdWhitelist: ";
                    for (String s : list) {
                        a = a + ", " + s;
                    }
                    p.sendMessage(a);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid command");
                    return true;
                }
                getConfig().set("CmdWhitelist", list);
                saveConfig();
                return true;
            } else if (args[0].equalsIgnoreCase("itemblacklist")) {
                ArrayList<String> list = (ArrayList<String>) getConfig().getStringList("ItemBlackList");
                if (args[1].equalsIgnoreCase("add")) {
                    list.add(p.getItemInHand().getType().toString());
                    sender.sendMessage(ChatColor.GREEN + "Item inhand added to blacklist");
                } else if (args[1].equalsIgnoreCase("remove")) {
                    list.remove(p.getItemInHand().getType().toString());
                    sender.sendMessage(ChatColor.GREEN + "Item inhand removed from blacklist");
                } else if (args[1].equalsIgnoreCase("list")) {
                    String a = "ItemBlackList: ";
                    for (String s : list) {
                        a = a + ", " + s;
                    }
                    p.sendMessage(a);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid command");
                    return true;
                }
                getConfig().set("ItemBlackList", list);
                saveConfig();
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid command");
                return true;
            }
        }
        return true;
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

    private void loadTranslations() {
        copyTranslation("custom");
        translation = getConfig().getString("Language");
        if (translation.equalsIgnoreCase("custom")) {
            languageFile = new File(getDataFolder() + File.separator + "lang" + File.separator + translation + ".yml");
            yaml = YamlConfiguration.loadConfiguration(languageFile);
        } else {
            InputStream defaultStream = getResource(translation + ".yml");
            yaml = YamlConfiguration.loadConfiguration(defaultStream);
        }
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
            e.printStackTrace();
        }
    }

}
