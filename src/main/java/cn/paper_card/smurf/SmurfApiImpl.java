package cn.paper_card.smurf;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.smurf.api.SmurfApi;
import org.jetbrains.annotations.NotNull;

class SmurfApiImpl implements SmurfApi {

    private final @NotNull SmurfServiceImpl smurfService;

    SmurfApiImpl(@NotNull DatabaseApi.MySqlConnection connection) {
        this.smurfService = new SmurfServiceImpl(connection);
    }

    @Override
    public @NotNull SmurfServiceImpl getSmurfService() {
        return this.smurfService;
    }
}
