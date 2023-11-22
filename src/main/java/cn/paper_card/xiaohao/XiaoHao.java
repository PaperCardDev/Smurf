package cn.paper_card.xiaohao;

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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public final class XiaoHao extends JavaPlugin implements XiaoHaoApi {

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;
    private Connection connection = null;
    private Table table = null;

    private final @NotNull TextComponent prefix;

    private final @NotNull TaskScheduler taskScheduler;

    public XiaoHao() {
        this.mySqlConnection = this.getDatabaseApi0().getRemoteMySqlDb().getConnectionImportant();

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

    private @NotNull Table getTable() throws SQLException {
        final Connection newCon = this.mySqlConnection.getRowConnection();
        if (this.connection == null) {
            this.connection = newCon;
            if (this.table != null) this.table.close();
            this.table = new Table(this.connection);
            return this.table;
        } else if (this.connection == newCon) {
            return this.table;
        } else {
            this.connection = newCon;
            if (this.table != null) this.table.close();
            this.table = new Table(this.connection);
            return this.table;
        }
    }


    @Override
    public void onEnable() {
        new TheCommand(this);
    }

    @Override
    public void onDisable() {
        this.taskScheduler.cancelTasks(this);

        synchronized (this.mySqlConnection) {
            if (this.table != null) {
                try {
                    this.table.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.table = null;
            }
        }
    }

    @Override
    public void addXiaoHao(@NotNull UUID main, @NotNull String name, @NotNull UUID xiaohao, @NotNull String name2) throws Exception {
        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();

                // 确认这个小号不是一个已经存在的大号
                {
                    final List<UUID> list = t.queryDaByXiao(main);
                    this.mySqlConnection.setLastUseTime();

                    if (list.size() > 0) throw new Exception("无法添加小号，%s已经是别人的小号".formatted(name));

                    final List<UUID> list1 = t.queryDaByXiao(xiaohao);
                    this.mySqlConnection.setLastUseTime();

                    if (list1.size() > 0) throw new Exception("无法添加小号，%s已经绑定了一个大号".formatted(name2));
                }

                // 确认这个大号不是小号
                {
                    final int c = t.queryCountByDa(xiaohao);

                    if (c > 0) throw new Exception("无法添加小号，%s是一个大号".formatted(name2));
                }

                // 插入数据
                final int inserted = t.insert(main, xiaohao);

                if (inserted != 1) throw new Exception("插入了%d条数据！".formatted(inserted));

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public boolean removeXiaoHao(@NotNull UUID xiaohao) throws Exception {
        synchronized (this.mySqlConnection) {

            try {
                final Table t = this.getTable();

                final int deleted = t.deleteByXiao(xiaohao);
                this.mySqlConnection.setLastUseTime();

                if (deleted == 1) return true;
                if (deleted == 0) return false;
                throw new Exception("删除了%d条数据！".formatted(deleted));

            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable UUID queryDaHao(@NotNull UUID xiaohao) throws Exception {
        synchronized (this.mySqlConnection) {

            try {
                final Table t = this.getTable();

                final List<UUID> uuids = t.queryDaByXiao(xiaohao);
                this.mySqlConnection.setLastUseTime();

                final int size = uuids.size();
                if (size == 0) return null;
                if (size == 1) return uuids.get(0);

                throw new Exception("查询到了%d条数据！".formatted(size));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public int queryCountByDa(@NotNull UUID dahao) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final Table t = this.getTable();

                final int c = t.queryCountByDa(dahao);
                this.mySqlConnection.setLastUseTime();

                return c;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.checkClosedException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
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
