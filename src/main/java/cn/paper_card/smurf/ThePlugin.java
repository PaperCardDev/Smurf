package cn.paper_card.smurf;

import cn.paper_card.bilibili_bind.BilibiliBindApi;
import cn.paper_card.database.DatabaseApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.UUID;

public class ThePlugin extends JavaPlugin {
    private final @NotNull TextComponent prefix;

    private final @NotNull TaskScheduler taskScheduler;

    public ThePlugin() {

        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(Component.text("小号管理").color(NamedTextColor.DARK_AQUA))
                .append(Component.text("]").color(NamedTextColor.GRAY))
                .build();

        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @NotNull DatabaseApi getDatabaseApi0() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("Database");
        if (plugin instanceof DatabaseApi api) {
            return api;
        } else throw new NoSuchElementException("Database插件未安装");
    }

    @Nullable BilibiliBindApi getBilibiliBindApi() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("BilibiliBind");
        if (plugin instanceof BilibiliBindApi api) {
            return api;
        } else return null;
    }

    @NotNull Permission addPermission(@NotNull String name) {
        final Permission permission = new Permission(name);
        this.getServer().getPluginManager().addPermission(permission);
        return permission;
    }

    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(error).color(NamedTextColor.RED))
                .build());
    }

    void sendWarning(@NotNull CommandSender sender, @NotNull String warning) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(warning).color(NamedTextColor.YELLOW))
                .build());
    }

    void sendInfo(@NotNull CommandSender sender, @NotNull String info) {
        sender.sendMessage(Component.text()
                .append(this.prefix)
                .appendSpace()
                .append(Component.text(info).color(NamedTextColor.GREEN))
                .build());
    }


    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @Nullable UUID parseArgPlayer(@NotNull String arg) {
        try {
            return UUID.fromString(arg);
        } catch (IllegalArgumentException ignored) {
        }

        for (OfflinePlayer offlinePlayer : this.getServer().getOfflinePlayers()) {
            if (arg.equals(offlinePlayer.getName())) return offlinePlayer.getUniqueId();
        }
        return null;
    }

    @NotNull String getPlayerNameOrNull(@NotNull UUID uuid) {
        final OfflinePlayer offlinePlayer = this.getServer().getOfflinePlayer(uuid);
        final String name = offlinePlayer.getName();
        if (name != null) return name;
        return "null";
    }

    @NotNull LinkedList<String> tabCompletePlayerName(@NotNull String arg, @NotNull String tip) {
        final LinkedList<String> list = new LinkedList<>();
        if (arg.isEmpty()) {
            list.add(tip);
        } else {
            for (OfflinePlayer offlinePlayer : this.getServer().getOfflinePlayers()) {
                final String name = offlinePlayer.getName();
                if (name == null) continue;
                if (name.startsWith(arg)) list.add(name);
            }
        }
        return list;
    }

}
