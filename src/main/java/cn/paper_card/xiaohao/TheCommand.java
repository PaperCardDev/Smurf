package cn.paper_card.xiaohao;

import cn.paper_card.bilibili_bind.BilibiliBindApi;
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

    private final @NotNull XiaoHao plugin;
    private final @NotNull Permission permission;

    TheCommand(@NotNull XiaoHao plugin) {
        super("xiaohao");
        this.plugin = plugin;

        final PluginCommand command = plugin.getCommand(this.getLabel());
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);

        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("xiaohao.command"));

        this.addSubCommand(new BiliCode());
        this.addSubCommand(new Add());
        this.addSubCommand(new Remove());
        this.addSubCommand(new Dahao());
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
                plugin.sendError(commandSender, "BilibiliBindApi不可用，服务器插件未安装？");
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                // 检查自己的小号数量
                final int c;
                try {
                    c = plugin.queryCountByDa(player.getUniqueId());
                } catch (SQLException e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (c != 0) {
                    plugin.sendWarning(commandSender, "你已经有%d个小号，每人至多一个小号，添加更多小号请联系服主".formatted(c));
                    return;
                }


                // 取出验证码
                final BilibiliBindApi.BindCodeInfo bindCodeInfo;

                try {
                    bindCodeInfo = bilibiliBindApi.getBindCodeApi().takeByCode(code);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (bindCodeInfo == null) {
                    plugin.sendWarning(commandSender, "验证码 %d 不存在或已经过期失效".formatted(code));
                    return;
                }

                try {
                    plugin.addXiaoHao(player.getUniqueId(), player.getName(), bindCodeInfo.uuid(), bindCodeInfo.name());
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                plugin.sendInfo(commandSender, "添加小号成功，小号：%s，大号：%s".formatted(bindCodeInfo.name(), player.getName()));
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
            final String argPlayerDa = strings.length > 0 ? strings[0] : null;
            final String argPlayerXiao = strings.length > 1 ? strings[1] : null;

            if (argPlayerDa == null) {
                plugin.sendError(commandSender, "必须提供参数：大号游戏名或UUID");
                return true;
            }

            if (argPlayerXiao == null) {
                plugin.sendError(commandSender, "必须提供参数：小号游戏名或UUID");
                return true;
            }


            final UUID da = plugin.parseArgPlayer(argPlayerDa);
            if (da == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayerDa));
                return true;
            }

            final UUID xiao = plugin.parseArgPlayer(argPlayerXiao);
            if (xiao == null) {
                plugin.sendError(commandSender, "找不到该玩家：%s".formatted(argPlayerXiao));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final String nameDa = plugin.getPlayerNameOrNull(da);
                final String nameXiao = plugin.getPlayerNameOrNull(xiao);

                try {
                    plugin.addXiaoHao(da, nameDa, xiao, nameXiao);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                plugin.sendInfo(commandSender, "添加小号成功，小号: %s，大号: %s".formatted(nameXiao, nameDa));

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
                final boolean removed;

                try {
                    removed = plugin.removeXiaoHao(uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (!removed) {
                    plugin.sendWarning(commandSender, "没有删除任何数据，应该没有这个小号");
                    return;
                }

                plugin.sendInfo(commandSender, "已删除小号：%s".formatted(plugin.getPlayerNameOrNull(uuid)));
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

    class Dahao extends TheMcCommand {

        private final @NotNull Permission permission;

        protected Dahao() {
            super("dahao");
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
            if (argPlayer == null) {
                if (commandSender instanceof final Player player) {
                    uuid = player.getUniqueId();
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
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                final UUID dahao;

                try {
                    dahao = plugin.queryDaHao(uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.sendError(commandSender, e.toString());
                    return;
                }

                if (dahao == null) {
                    plugin.sendWarning(commandSender, "%s 不是小号".formatted(plugin.getPlayerNameOrNull(uuid)));
                    return;
                }

                plugin.sendInfo(commandSender, "%s 的大号是：%s".formatted(
                        plugin.getPlayerNameOrNull(uuid),
                        plugin.getPlayerNameOrNull(dahao)
                ));
            });


            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String arg = strings[0];
                return plugin.tabCompletePlayerName(arg, "[小号游戏名或UUID]");
            }

            return null;
        }
    }
}
