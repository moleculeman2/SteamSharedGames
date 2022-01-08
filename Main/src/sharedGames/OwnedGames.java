package sharedGames;

import java.util.List;

class OwnedGames {
    public Response response;

    class Response {
        String game_count;
        public List<Games> games;

        class Games {
            String appid;
            String name;
            String playtime_forever;
            String img_icon_url;
            String img_logo_url;
        }
    }
}
