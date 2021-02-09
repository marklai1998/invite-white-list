package com.mrmatches.invitewhitelist;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class invitewhitelist extends JavaPlugin implements Listener {
    private FileConfiguration config = this.getConfig();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("invite")) {
            if (args.length == 1) {
                addPlayer(sender, args[0]);
                return true;
            }
        }
        if (command.getName().equalsIgnoreCase("uninvite")) {
            if (args.length == 1) {
                removePlayer(sender, args[0]);
                return true;
            }
        }
        return false;
    }

    private Boolean checkName(Player player) {
        String invite_by = config.getString("Name." + player.getDisplayName() + ".invite_by");
        if (invite_by == null || invite_by.equals("")) {
            return false;
        } else {
            config.set("UUIDs." + player.getUniqueId().toString(), player.getName());
            config.set("Name." + player.getDisplayName() + ".UUID", player.getUniqueId().toString());
            this.saveConfig();
            return true;
        }
    }

    private Boolean checkUUID(Player player) {
        String player_name = config.getString("UUIDs." + player.getUniqueId().toString());
        if (player_name == null || player_name.equals("")) {
            return false;
        } else {
            String invite_by = config.getString("Name." + player_name + ".invite_by");
            if (invite_by == null || invite_by.equals("")) {
                config.set("UUIDs." + player.getUniqueId().toString(), null);
                return false;
            } else {
                if (!config.getString("UUIDs." + player.getUniqueId().toString()).equalsIgnoreCase(player.getDisplayName())) {
                    String old_name = config.getString("UUIDs." + player.getUniqueId().toString());
                    String old_invite_by = config.getString("Name." + old_name + ".invite_by");
                    String old_quota = config.getString("Name." + old_name + ".quota");
                    config.set("Name." + old_name, null);
                    config.set("Name." + player.getDisplayName() + ".invite_by", old_invite_by);
                    config.set("Name." + player.getDisplayName() + ".quota", old_quota);
                }
                config.set("UUIDs." + player.getUniqueId().toString(), player.getName());
                this.saveConfig();
                return true;
            }
        }
    }

    private void addPlayer(CommandSender sender, String player) {
        String try_player = config.getString("Name." + player);
        if (player.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "你不能邀請自己！");
            return;
        }
        if (try_player == null || try_player.equals("")) {
            if (!(sender instanceof Player)) {
                performAdd(sender, player);
                return;
            }
            if (config.getInt("Name." + sender.getName() + ".quota") > 0) {
                performAdd(sender, player);
            } else {
                sender.sendMessage(ChatColor.RED + "你的邀請名額不足！");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "此玩家己被邀請！");
        }
    }

    private void performAdd(CommandSender sender, String player) {
        if (sender instanceof Player) {
            Player sender_as_player = (Player) sender;
            config.set("Name." + player + ".invite_by", sender_as_player.getUniqueId().toString());
        } else {
            config.set("Name." + player + ".invite_by", sender.getName());
        }
        config.set("Name." + player + ".quota", 2);
        if (sender instanceof Player) {
            config.set("Name." + sender.getName() + ".quota", config.getInt("Name." + sender.getName() + ".quota") - 1);
            sender.sendMessage(ChatColor.GREEN + "你己成功邀請 " + player + " ！ 你還有 " + config.getInt("Name." + sender.getName() + ".quota") + " 個名額");
        }
        this.saveConfig();
    }

    private void removePlayer(CommandSender sender, String player) {
        String try_player = config.getString("Name." + player);
        if (!sender.getName().equalsIgnoreCase("CONSOLE") && !sender.getName().equalsIgnoreCase("@")) {
            Player sender_as_player = (Player) sender;
            if (!(try_player == null || try_player.equals(""))) {
                if (!config.getString("Name." + player + ".invite_by").equalsIgnoreCase(sender_as_player.getUniqueId().toString())) {
                    sender.sendMessage(ChatColor.RED + "此人不是由你邀請的！");
                    return;
                }
            }
        }
        if (player.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(ChatColor.RED + "你不能移除自己！");
            return;
        }
        if (!(try_player == null || try_player.equals(""))) {
            config.set("Name." + player, null);
            if (sender instanceof Player) {
                config.set("Name." + sender.getName() + ".quota", config.getInt("Name." + sender.getName() + ".quota") + 1);
                sender.sendMessage(ChatColor.GREEN + "你己成功移除 " + player + " ！ 你還有 " + config.getInt("Name." + sender.getName() + ".quota") + " 個名額");
            }
            this.saveConfig();
        } else {
            sender.sendMessage(ChatColor.RED + "此玩家尚未被邀請！");
        }
    }

    @EventHandler
    public void onJoin(PlayerLoginEvent e) {
        if (checkName(e.getPlayer()) || checkUUID(e.getPlayer())) {
            e.allow();
        } else {
            e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "你並不在白名單中,請其他玩家邀請你吧～");
        }
    }
}
