package cn.paper_card.smurf;

import cn.paper_card.bilibili_bind.api.BilibiliBindApi;
import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.smurf.api.SmurfApi;
import cn.paper_card.smurf.api.SmurfInfo;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.UUID;

public class ThePlugin extends JavaPlugin {
    private final @NotNull TextComponent prefix;

    private final @NotNull TaskScheduler taskScheduler;

    private SmurfApiImpl smurfApi = null;

    private BilibiliBindApi bilibiliBindApi = null;


    public ThePlugin() {
        this.prefix = Component.text()
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(Component.text("小号管理").color(NamedTextColor.DARK_AQUA))
                .append(Component.text("]").color(NamedTextColor.GRAY))
                .build();

        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    void handleException(@NotNull Exception e) {
        this.getLogger().throwing(ThePlugin.class.getSimpleName(), "handleException", e);
    }

    @Override
    public void onLoad() {

        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("无法连接到" + DatabaseApi.class.getSimpleName());

        this.smurfApi = new SmurfApiImpl(api.getRemoteMySQL().getConnectionImportant());

        this.getSLF4JLogger().info("注册%s...".formatted(SmurfApi.class.getSimpleName()));
        this.getServer().getServicesManager().register(SmurfApi.class, this.smurfApi, this, ServicePriority.Highest);
    }

    @Override
    public void onEnable() {
        // 获取BilibiliAPI

        final String name = BilibiliBindApi.class.getSimpleName();
        this.bilibiliBindApi = this.getServer().getServicesManager().load(BilibiliBindApi.class);
        if (bilibiliBindApi == null) {
            this.getLogger().warning("无法连接到" + name);
        } else {
            this.getLogger().info("已连接到" + name);
        }

        new TheCommand(this);
    }

    @Override
    public void onDisable() {
        try {
            this.smurfApi.getSmurfService().close();
        } catch (SQLException e) {
            this.handleException(e);
        }

        this.taskScheduler.cancelTasks(this);

        this.getServer().getServicesManager().unregisterAll(this);
    }


    @NotNull SmurfApiImpl getSmurfApi() {
        return this.smurfApi;
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

    void sendException(@NotNull CommandSender sender, @NotNull Exception e) {
        final TextComponent.Builder text = Component.text();
        text.append(this.prefix);
        text.appendSpace();
        text.append(Component.text("==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        sender.sendMessage(text.build());
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

    void sendInfo(@NotNull CommandSender sender, @NotNull SmurfInfo info) {
        final TextComponent.Builder text = Component.text();
        text.append(Component.text("==== 小号信息 ===="));

        final NamedTextColor color = NamedTextColor.DARK_AQUA;

        // 小号
        text.appendNewline();
        text.append(Component.text("小号：").color(color));
        text.append(Component.text(info.smurfName())
                .color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(info.smurfName()))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制")))
        );

        final String smurfUuid = info.smurfUuid().toString();
        text.append(Component.text(" ("));
        text.append(Component.text(smurfUuid)
                .color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(smurfUuid))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制")))
        );
        text.append(Component.text(")"));

        // 大号
        text.appendNewline();
        text.append(Component.text("大号：").color(color));
        text.append(Component.text(info.mainName())
                .color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(info.mainName()))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制")))
        );

        final String mainUuid = info.mainUuid().toString();
        text.append(Component.text(" ("));
        text.append(Component.text(mainUuid)
                .color(NamedTextColor.GREEN).decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.copyToClipboard(mainUuid))
                .hoverEvent(HoverEvent.showText(Component.text("点击复制")))
        );
        text.append(Component.text(")"));

        // 备注
        text.appendNewline();
        text.append(Component.text("备注：").color(color));
        text.append(Component.text(info.remark()));

        // 时间
        text.appendNewline();
        final String datetime = new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss").format(info.time());
        text.append(Component.text("时间：").color(color));
        text.append(Component.text(datetime));

        sender.sendMessage(text.build());
    }


    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @Nullable BilibiliBindApi getBilibiliBindApi() {
        return this.bilibiliBindApi;
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
