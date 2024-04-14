package com.ramide1.mcgemini;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gemini implements CommandExecutor {
    private App plugin;

    public Gemini(App plugin) {
        this.plugin = plugin;
    }

    public String sendRequestToGeminiApi(String instructions, String sender, String question, String apikey) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apikey;
        String content = "Response was not ok.";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            String messages = "{\"role\": \"user\",\"parts\": [" + "{\"text\": \"" + question + "\"}" + "]}";
            if (!getHistory(sender).isEmpty()) {
                messages = getHistory(sender) + "," + messages;
            }
            String newHistory = messages;
            if (!instructions.isEmpty()) {
                messages = "{\"role\": \"user\",\"parts\": [" + "{\"text\": \"" + instructions + "\"}" + "]}" + "," + "{\"role\": \"model\",\"parts\": [" + "{\"text\": \"Ok.\"}" + "]}" + "," + messages;
            }
            messages = "[" + messages + "]";
            String data = "{\"contents\": " + messages + "}";
            OutputStream os = connection.getOutputStream();
            byte[] postData = data.getBytes("utf-8");
            os.write(postData, 0, postData.length);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                content = "Field text was not found in JSON response.";
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                String regex = "\"candidates\"\\s*:\\s*\\[\\s*\\{\\s*\"content\"\\s*:\\s*\\{\\s*\"parts\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([^\"]+)\"";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(response.toString());
                if (matcher.find()) {
                    content = matcher.group(1);
                    newHistory = newHistory + "," + "{\"role\": \"model\",\"parts\": [" + "{\"text\": \"" + content
                            + "\"}" + "]}";
                    saveHistory(sender, newHistory);
                }
            }
            connection.disconnect();
        } catch (Exception e) {
            content = e.getMessage();
        }
        return content;
    }

    public boolean onCommand(CommandSender sender, Command gemini, String label, String[] args) {
        String instructions = plugin.getConfig().getString("Config.instructions", "");
        String apiKey = plugin.getConfig().getString("Config.apikey", "");
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length >= 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String response = sendRequestToGeminiApi(instructions, player.getName(), String.join(" ", args),
                                apiKey);
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(response));
                    }
                }.runTaskAsynchronously(plugin);
            } else {
                player.sendMessage(ChatColor.RED + "This command needs at least one argument.");
            }
        } else {
            if (args.length >= 1) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        String response = sendRequestToGeminiApi(instructions, "console", String.join(" ", args),
                                apiKey);
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getLogger().info(response));
                    }
                }.runTaskAsynchronously(plugin);
            } else {
                plugin.getLogger().info(ChatColor.RED + "This command needs at least one argument.");
            }
        }
        return true;
    }

    public boolean saveHistory(String sender, String history) {
        plugin.dataConfig.set(sender, history);
        try {
            plugin.dataConfig.save(plugin.data);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getHistory(String sender) {
        return plugin.dataConfig.contains(sender) ? plugin.dataConfig.getString(sender) : "";
    }
}