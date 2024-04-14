package com.ramide1.mcgemini;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

public class Reload implements CommandExecutor {
    private App plugin;

    public Reload(App plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command gptreload, String label, String[] args) {
        plugin.reloadConfig();
        sender.sendMessage(plugin.pluginName + " has reloaded!");
        return true;
    }
}