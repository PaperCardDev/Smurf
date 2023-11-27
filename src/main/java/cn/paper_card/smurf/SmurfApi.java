package cn.paper_card.smurf;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface SmurfApi {

    record SmurfInfo(
            UUID mainUuid,
            String mainName,
            UUID smurfUuid,
            String smurfName,

            long time,
            String remark
    ) {
    }

    class IsAlreadySmurf extends Exception {
        private final SmurfInfo smurfInfo;

        IsAlreadySmurf(SmurfInfo smurfInfo) {
            this.smurfInfo = smurfInfo;
        }

        public @NotNull SmurfInfo getSmurfInfo() {
            return this.smurfInfo;
        }

        @Override
        public String getMessage() {
            return "%s (%s) 已经是 %s (%s) 的小号！".formatted(
                    this.smurfInfo.smurfName(), this.smurfInfo.smurfUuid().toString(),
                    this.smurfInfo.mainName(), this.smurfInfo.mainUuid().toString()
            );
        }
    }

    class TheSmurfIsMain extends Exception {
        private final SmurfInfo smurfInfo;

        TheSmurfIsMain(SmurfInfo smurfInfo) {
            this.smurfInfo = smurfInfo;
        }

        public @NotNull SmurfInfo getSmurfInfo() {
            return this.smurfInfo;
        }

        @Override
        public String getMessage() {
            return "%s (%s) 无法作为一个小号，因为他已经是 %s (%s) 的大号".formatted(
                    this.smurfInfo.mainName(), this.smurfInfo.mainUuid().toString(),
                    this.smurfInfo.smurfName(), this.smurfInfo.smurfUuid().toString()
            );
        }
    }

    class TheSmurfIsSelf extends Exception {
    }

    // 添加小号
    void addSmurf(@NotNull SmurfInfo info) throws Exception;

    // 删除小号
    boolean removeSmurf(@NotNull UUID main, @NotNull UUID smurf) throws Exception;

    // 根据小号UUID查询
    @Nullable SmurfInfo queryBySmurfUuid(@NotNull UUID uuid) throws Exception;

    @NotNull List<SmurfInfo> queryByMainUuid(@NotNull UUID uuid, int limit, int offset) throws Exception;
}
