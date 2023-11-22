package cn.paper_card.xiaohao;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface XiaoHaoApi {

    void addXiaoHao(@NotNull UUID main, @NotNull String name, @NotNull UUID xiaohao, @NotNull String name2) throws Exception;

    boolean removeXiaoHao(@NotNull UUID xiaohao) throws Exception;

    @Nullable UUID queryDaHao(@NotNull UUID xiaohao) throws Exception;

    int queryCountByDa(@NotNull UUID dahao) throws Exception;

}
