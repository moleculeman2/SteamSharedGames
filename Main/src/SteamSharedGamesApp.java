import com.google.gson.*;
import com.lukaspradel.steamapi.core.exception.SteamApiException;
import com.lukaspradel.steamapi.data.json.friendslist.Friend;
import com.lukaspradel.steamapi.data.json.friendslist.GetFriendList;
import com.lukaspradel.steamapi.data.json.playersummaries.GetPlayerSummaries;
import com.lukaspradel.steamapi.webapi.client.SteamWebApiClient;
import com.lukaspradel.steamapi.webapi.request.GetFriendListRequest;
import com.lukaspradel.steamapi.webapi.request.GetPlayerSummariesRequest;
import com.lukaspradel.steamapi.webapi.request.builders.SteamWebApiRequestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SteamSharedGamesApp {
    public static void main(String[] args) throws IOException, SteamApiException {

        String steamKey = "***REMOVED***";
        String yourId = "***REMOVED***";
        String steamIdFriend = "xxxxx";
        String getPlayerSummary = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="+steamKey+"&steamids="+yourId;
        String getFriendList = "http://api.steampowered.com/ISteamUser/GetFriendList/v0001/?key="+steamKey+"&steamid="+yourId+"&relationship=friend";
        String getOwnedGames = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+yourId+"&include_appinfo=true&format=json";
        String getPlayerAchievements = "http://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/?appid=440&key="+steamKey+"&steamid="+yourId;
        /**
        SteamWebApiClient client = new SteamWebApiClient.SteamWebApiClientBuilder(steamKey).build();
        GetFriendListRequest request = SteamWebApiRequestFactory.createGetFriendListRequest(yourId);
        GetFriendList getFriendList = client.<GetFriendList> processRequest(request);
        System.out.println(getFriendList.toString());
        List<Friend> friendsArray = getFriendList.getFriendslist().getFriends();
        for (Friend f: friendsArray) {
            System.out.println(f.getSteamid());
            GetPlayerSummariesRequest request1 = SteamWebApiRequestFactory.createGetPlayerSummariesRequest(Collections.singletonList(f.getSteamid()));
            GetPlayerSummaries getPlayerSummaries = client.<GetPlayerSummaries> processRequest(request1);
        }
        //allFriends = friendsList.getFriends()
        **/


        String sURL = getPlayerAchievements;
        URL url = new URL(sURL);
        URLConnection request = url.openConnection();
        request.connect();

        // Convert to a JSON object to print data
        JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
        String json = rootObj.toString();
        System.out.println(json);

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        PlayerAchievements playerAchievements = gson.fromJson(json, PlayerAchievements.class);
        System.out.println(playerAchievements);

        json = gson.toJson(playerAchievements);
        System.out.println(json);

    }
}

class PlayerAchievements{
    public Playerstats playerstats;

}

class Playerstats{
    String steamID;
    String gameName;
    ArrayList<Achievements> achievements;
}

class Achievements{
    String apiname;
    String achieved;
    String unlocktime;
}

class OwnedGames{
    public int game_count;
    private String[] games;
    private String appid;
    private String name;
    private String playtime_2weeks;
    private String playtime_forever;
    private String img_icon_url;
    private String img_logo_url;
    private String has_community_visible_stats;

    public OwnedGames(){}

    public int getGame_count() {
        return game_count;
    }

    public void setGame_count(int game_count) {
        this.game_count = game_count;
    }

    public String[] getGames() {
        return games;
    }

    public void setGames(String[] games) {
        this.games = games;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaytime_2weeks() {
        return playtime_2weeks;
    }

    public void setPlaytime_2weeks(String playtime_2weeks) {
        this.playtime_2weeks = playtime_2weeks;
    }

    public String getPlaytime_forever() {
        return playtime_forever;
    }

    public void setPlaytime_forever(String playtime_forever) {
        this.playtime_forever = playtime_forever;
    }

    public String getImg_icon_url() {
        return img_icon_url;
    }

    public void setImg_icon_url(String img_icon_url) {
        this.img_icon_url = img_icon_url;
    }

    public String getImg_logo_url() {
        return img_logo_url;
    }

    public void setImg_logo_url(String img_logo_url) {
        this.img_logo_url = img_logo_url;
    }

    public String getHas_community_visible_stats() {
        return has_community_visible_stats;
    }

    public void setHas_community_visible_stats(String has_community_visible_stats) {
        this.has_community_visible_stats = has_community_visible_stats;
    }
}
