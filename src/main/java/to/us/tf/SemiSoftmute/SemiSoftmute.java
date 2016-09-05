package to.us.tf.SemiSoftmute;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Robo on 9/5/2016.
 */
public class SemiSoftmute extends JavaPlugin implements Listener
{
    FileConfiguration config = getConfig();
    ConcurrentHashMap<Player, List<String>> loadedPlayers = new ConcurrentHashMap<>();

    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event)
    {
        if (config.contains(event.getPlayer().getUniqueId().toString()))
            loadedPlayers.put(event.getPlayer(), config.getStringList(event.getPlayer().getUniqueId().toString()));
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event)
    {
        loadedPlayers.remove(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    void onAsyncPlayerChat(AsyncPlayerChatEvent event)
    {
        //Don't do anything if nobody is semisoftmuted
        if (loadedPlayers.isEmpty())
            return;

        //Is the softmuted player speaking?
        if (!loadedPlayers.containsKey(event.getPlayer()))
            return; //Otherwise, do nothing

        Set<Player> recipients = event.getRecipients();
        List<String> okPlayers = loadedPlayers.get(event.getPlayer());
        for (Player target : recipients)
        {
            if (!okPlayers.contains(target))
                recipients.remove(target);
        }
        recipients.add(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerUseMe(PlayerCommandPreprocessEvent event)
    {
        if (!loadedPlayers.containsKey(event.getPlayer()))
            return;

        if (!event.getMessage().toLowerCase().startsWith("/me "))
            return;

        Player player = event.getPlayer();
        player.sendMessage("* " + player.getName() + event.getMessage().substring(3));
        event.setCancelled(true);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!sender.isOp())
        {
            sender.sendMessage("Whoops, did you make a mistake? Don't forget about " + ChatColor.GOLD + "/help");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("semisoftmute") && args.length > 1)
        {
            if (args[0].equalsIgnoreCase("reload"))
            {
                reloadConfig();
                loadedPlayers.clear();
                for (Player player : Bukkit.getOnlinePlayers())
                {
                    if (config.contains(player.getUniqueId().toString()))
                        loadedPlayers.put(player, config.getStringList(player.getUniqueId().toString()));
                }
                sender.sendMessage("Reloaded config and loadedPlayers");
                return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null)
            {
                sender.sendMessage(args[1] + " not online");
                return false;
            }

            if (args[0].equalsIgnoreCase("check"))
            {
                if (!loadedPlayers.contains(player))
                {
                    sender.sendMessage(player.getName() + " is not semisoftmuted");
                    return true;
                }

                UUID uuid;
                OfflinePlayer okPlayer;
                StringBuilder okPlayerList = new StringBuilder(player.getName());
                for (String uuidString : loadedPlayers.get(player))
                {
                    uuid = UUID.fromString(uuidString);
                    okPlayer = Bukkit.getOfflinePlayer(uuid);
                    okPlayerList.append(", ");
                    okPlayerList.append(okPlayer.getName());
                }
                sender.sendMessage(okPlayerList.toString());
                return true;
            }

            String uuidString = player.getUniqueId().toString();

            if (args[0].equalsIgnoreCase("delete"))
            {
                config.set(uuidString, null);
                saveConfig();
                sender.sendMessage(player.getName() + " is no longer semisoftmuted.");
            }

            if (args.length < 3)
                return false;

            if (args[0].equalsIgnoreCase("add"))
            {
                if (!config.contains(uuidString))
                {
                    sender.sendMessage("Creating new semisoftmuted player...");
                    config.set(uuidString, new ArrayList<String>());
                    saveConfig();
                }

                Player target = Bukkit.getPlayer(args[2]);
                if (target == null)
                {
                    sender.sendMessage(args[2] + " is not online");
                    return true;
                }

                //Add player to okPlayers
                List<String> okPlayers = config.getStringList(uuidString);
                okPlayers.add(target.getUniqueId().toString());
                config.set(uuidString, okPlayers);
                saveConfig();
                sender.sendMessage(target.getName() + " will now be able to hear " + player.getName());
            }

            if (args[0].equalsIgnoreCase("remove"))
            {
                if (!config.contains(uuidString))
                {
                    sender.sendMessage("Is not semisoftmuted.");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[2]);
                if (target == null)
                {
                    sender.sendMessage(args[2] + " is not online");
                    return true;
                }

                //Remove player from okPlayers
                List<String> okPlayers = config.getStringList(uuidString);
                okPlayers.remove(target.getUniqueId().toString());
                config.set(uuidString, okPlayers);
                saveConfig();
                sender.sendMessage(target.getName() + " will no longer hear " + player.getName());
            }
        }

        sender.sendMessage("If you see this message, make a /report");
        return false;
    }

}
