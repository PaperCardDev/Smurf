package cn.paper_card.smurf;

import cn.paper_card.bilibili_bind.api.BilibiliBindApi;
import cn.paper_card.bilibili_bind.api.BindCodeInfo;
import cn.paper_card.mc_command.TheMcCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

class TheCommand extends TheMcCommand.HasSub {

    private final @NotNull ThePlugin plugin;
    private final @NotNull Permission permission;


    TheCommand(@NotNull ThePlugin plugin) {
        super("smurf");
        this.plugin = plugin;

        final PluginCommand command = plugin.getCommand(this.getLabel());
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);

        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("smurf.command"));

        this.addSubCommand(new BiliCode());
        this.addSubCommand(new Add());
        this.addSubCommand(new Remove());
        this.addSubCommand(new Query());
    }


    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }


    class BiliCode extends TheMcCommand {

        private final @NotNull Permission permission;

        protected BiliCode() {
            super("bili-code");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            final String argCode = strings.length > 0 ? strings[0] : null;

            if (argCode == null) {
                plugin.sendError(commandSender, "必须提供参数：Bilibili绑定验证码");
                return true;
            }

            if (!(commandSender instanceof final Player player)) {
                plugin.sendError(commandSender, "该命令只能由玩家来执行");
                return true;
            }

            final int code;

            try {
                code = Integer.parseInt(argCode);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的验证码".formatted(argCode));
                return true;
            }

            final BilibiliBindApi bilibiliBindApi = plugin.getBilibiliBindApi();
            if (bilibiliBindApi == null) {
                plugin.sendError(commandSender, "%s不可用，服务器插件未安装？".formatted(BilibiliBindApi.class.getSimpleName()));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                // 检查自己的小号数量

                final List<SmurfApi.SmurfInfo> list;

                try {
                    list = plugin.getSmurfApi().queryByMainUuid(player.getUniqueId(), 1, 0);
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (!list.isEmpty()) {
                    plugin.sendWarning(commandSender, "你已经有了至少一个小号，每人至多一个小号，需要添加更多小号请联系服主");
                    return;
                }

                // 取出验证码
                final BindCodeInfo bindCodeInfo;

                try {
                    bindCodeInfo = bilibiliBindApi.getBindCodeService().takeByCode(code);
                } catch (Exception e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (bindCodeInfo == null) {
                    plugin.sendWarning(commandSender, "验证码 %d 不存在或已经过期失效".formatted(code));
                    return;
                }

                // 添加
                final SmurfApi.SmurfInfo info = new SmurfApi.SmurfInfo(
                        player.getUniqueId(), player.getName(),
                        bindCodeInfo.uuid(), bindCodeInfo.name(),
                        System.currentTimeMillis(), "bilibili绑定验证码添加"
                );

                try {
                    plugin.getSmurfApi().addSmurf(info);
                } catch (SmurfApi.IsAlreadySmurf | SmurfApi.TheSmurfIsSelf | SmurfApi.TheSmurfIsMain e) {
                    plugin.sendWarning(commandSender, e.getMessage());
                    return;
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                plugin.sendInfo(commandSender, "添加小号成功 :D");
                plugin.sendInfo(commandSender, info);
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            if (strings.length == 1) {
                final String code = strings[0];
                if (code.isEmpty()) {
                    final LinkedList<String> list = new LinkedList<>();
                    list.add("<验证码>");
                    return list;
                }
            }

            return null;
        }
    }

    class Add extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Add() {
            super("add");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            final String argPlayerMain = strings.length > 0 ? strings[0] : null;
            final String argPlayerSmurf = strings.length > 1 ? strings[1] : null;

            if (argPlayerMain == null) {
                plugin.sendError(commandSender, "必须提供参数：大号游戏名或UUID");
                return true;
            }

            if (argPlayerSmurf == null) {
                plugin.sendError(commandSender, "必须提供参数：小号游戏名或UUID");
                return true;
            }


            final UUID main = plugin.parseArgPlayer(argPlayerMain);
            if (main == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayerMain));
                return true;
            }

            final UUID smurf = plugin.parseArgPlayer(argPlayerSmurf);
            if (smurf == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayerSmurf));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final String mainName = plugin.getPlayerNameOrNull(main);
                final String smurfName = plugin.getPlayerNameOrNull(smurf);

                final SmurfApi.SmurfInfo info = new SmurfApi.SmurfInfo(
                        main, mainName, smurf, smurfName,
                        System.currentTimeMillis(), "add执行添加，%s执行".formatted(commandSender.getName())
                );

                try {
                    plugin.getSmurfApi().addSmurf(info);
                } catch (SmurfApi.IsAlreadySmurf | SmurfApi.TheSmurfIsSelf | SmurfApi.TheSmurfIsMain e) {
                    plugin.sendWarning(commandSender, e.getMessage());
                    return;
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                plugin.sendInfo(commandSender, "添加小号成功 :D");
                plugin.sendInfo(commandSender, info);

            });

            return true;
        }


        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String arg = strings[0];
                return plugin.tabCompletePlayerName(arg, "<大号游戏名或UUID>");
            }

            if (strings.length == 2) {
                final String arg = strings[1];
                return plugin.tabCompletePlayerName(arg, "<小号游戏名或UUID>");
            }

            return null;
        }
    }

    class Remove extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Remove() {
            super("remove");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argPlayer = strings.length > 0 ? strings[0] : null;
            if (argPlayer == null) {
                plugin.sendError(commandSender, "必须提供参数：小号游戏名或UUID");
                return true;
            }

            final UUID uuid = plugin.parseArgPlayer(argPlayer);

            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                // 先查询
                final SmurfApi.SmurfInfo info;
                try {
                    info = plugin.getSmurfApi().queryBySmurfUuid(uuid);
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (info == null) {
                    plugin.sendWarning(commandSender, "%s 不是任何人的小号".formatted(argPlayer));
                    return;
                }


                final boolean removed;

                try {
                    removed = plugin.getSmurfApi().removeSmurf(info.mainUuid(), info.smurfUuid());
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (!removed) {
                    plugin.sendWarning(commandSender, "未知错误！");
                    return;
                }

                plugin.sendInfo(commandSender, "已删除该小号");
                plugin.sendInfo(commandSender, info);
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String arg = strings[0];
                return plugin.tabCompletePlayerName(arg, "<小号游戏名或UUID>");
            }

            return null;
        }
    }

    class Query extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Query() {
            super("query");
            this.permission = plugin.addPermission(TheCommand.this.permission.getName() + "." + this.getLabel());
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !commandSender.hasPermission(this.permission);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            final String argPlayer = strings.length > 0 ? strings[0] : null;

            final UUID uuid;
            final boolean isSelf;
            if (argPlayer == null) {
                if (commandSender instanceof final Player player) {
                    uuid = player.getUniqueId();
                    isSelf = true;
                } else {
                    plugin.sendError(commandSender, "必须提供参数：小号游戏名或UUID");
                    return true;
                }
            } else {
                uuid = plugin.parseArgPlayer(argPlayer);
                if (uuid == null) {
                    plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayer));
                    return true;
                }

                if (commandSender instanceof final Player player) {
                    isSelf = player.getUniqueId().equals(uuid);
                } else {
                    isSelf = false;
                }
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                final String name = isSelf ? "你" : "该玩家";

                // 查询是不是小号
                final SmurfApi.SmurfInfo info;
                try {
                    info = plugin.getSmurfApi().queryBySmurfUuid(uuid);
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                if (info != null) {
                    plugin.sendInfo(commandSender, name + "是一个小号");
                    plugin.sendInfo(commandSender, info);
                    return;
                }

                // 不是小号
                final List<SmurfApi.SmurfInfo> list;

                try {
                    list = plugin.getSmurfApi().queryByMainUuid(uuid, 4, 0);
                } catch (SQLException e) {
                    plugin.handleException(e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                final int size = list.size();
                if (size == 0) {
                    plugin.sendInfo(commandSender, name + "没有小号");
                    return;
                }

                plugin.sendInfo(commandSender, name + "有若干个小号，最多只列出4个，TODO：看到此条消息叫腐竹去完善一下");
                for (final SmurfApi.SmurfInfo smurfInfo : list) {
                    plugin.sendInfo(commandSender, smurfInfo);
                }
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String arg = strings[0];
                return plugin.tabCompletePlayerName(arg, "[游戏名或UUID]");
            }

            return null;
        }
    }
}
