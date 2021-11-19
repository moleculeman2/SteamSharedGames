import com.google.gson.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SteamSharedGamesApp {
    public static void main(String[] args) throws IOException, InterruptedException {

        String steamKey = "***REMOVED***";
        String yourId = "***REMOVED***";
        String steamFriendId1 = "***REMOVED***"; //***REMOVED***'s ID
        String steamFriendId2 = "***REMOVED***"; //***REMOVED***'s ID
        List<String> steamIdsList = Arrays.asList(yourId, steamFriendId1, steamFriendId2);
        String getPlayerSummary = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="+steamKey+"&steamids="+yourId;
        String getFriendList = "http://api.steampowered.com/ISteamUser/GetFriendList/v0001/?key="+steamKey+"&steamid="+yourId+"&relationship=friend";
        String getOwnedGames = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+yourId+"&include_appinfo=true&format=json";
        String getPlayerAchievements = "http://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/?appid=440&key="+steamKey+"&steamid="+yourId;
        FinalList finalList = new FinalList();

        for (String e: steamIdsList) {
            String sURL = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+e+"&include_appinfo=true&format=json";;
            URL url = new URL(sURL);
            URLConnection request = url.openConnection();
            request.connect();

            JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            String json = rootObj.toString();
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            Gson gson = builder.create();
            OwnedGames ownedGames = gson.fromJson(json, OwnedGames.class);
            finalList.addOwnedGames(ownedGames);
            json = gson.toJson(ownedGames);
        }

        //call method to parse owned games for both and create new list
        finalList.SharedOwnedGames();

        /**
        //run achievement command for all ids, store in list like allGamesList
        for (String e: steamIdsList) {
            String sURL = getPlayerAchievements;
            URL url = new URL(sURL);
            URLConnection request = url.openConnection();
            request.connect();

            JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            String json = rootObj.toString();
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            Gson gson = builder.create();
            PlayerAchievements playerAchievements = gson.fromJson(json, PlayerAchievements.class);
            json = gson.toJson(playerAchievements);
            //json = gson.toJson(ownedGames);
        }

        finalList.SharedAchievements();
        **/

        /**
        String sURL = getOwnedGames;
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
        //PlayerAchievements playerAchievements = gson.fromJson(json, PlayerAchievements.class);
        //json = gson.toJson(playerAchievements);
        OwnedGames playerAchievements = gson.fromJson(json, OwnedGames.class);
        json = gson.toJson(playerAchievements);
        System.out.println(json);
        **/

    }
}

class FinalList{
    List<OwnedGames> ownedGamesList = new ArrayList<OwnedGames>();
    List<PlayerAchievements> playerAchievementsList = new ArrayList<PlayerAchievements>();
    List<String> steamIdsList = new ArrayList<String>();
    List<String> potentialErrors = new ArrayList<String>();

    List<HashSet<String>> sharedGameNames = new ArrayList<HashSet<String>>();
    HashMap<String, String> finalGameList = new HashMap<>();
    List<OwnedGames.Response> allGamesList = new ArrayList<OwnedGames.Response>();
    List<PlayerAchievements.PlayerStats> allAchievementsList = new ArrayList<PlayerAchievements.PlayerStats>();
    public FinalList(){}

    public void SharedOwnedGames() throws IOException, InterruptedException {
        //gets the games list from each OwnedGames and puts em in a list
        for (int i = 0; i < ownedGamesList.size(); i++) {
            allGamesList.add(ownedGamesList.get(i).response);
            sharedGameNames.add(new HashSet<String>());
        }
        //loops through each games list and puts them in hashmap (doesnt allow dupes)
        for (int i = 0; i < allGamesList.size(); i++) {
            for (int j = 0; j < allGamesList.get(i).games.size(); j++) {
                sharedGameNames.get(i).add(allGamesList.get(i).games.get(j).appid.concat("~").concat(allGamesList.get(i).games.get(j).name));
            }
        }

        for (int i = 0; i < sharedGameNames.size(); i++) {
            sharedGameNames.get(0).retainAll(sharedGameNames.get(i));
        }
        for ( String s: sharedGameNames.get(0)) {
            finalGameList.put(s.substring(0,s.indexOf("~")), s.substring(s.indexOf("~")+1));
        }

        System.out.println("Before coop sorting, Games hashmap has size of: " + sharedGameNames.get(0).size());
        //iterates through shared games, searches steam for coop tag, removes if not there
        for (Map.Entry<String, String> entry : finalGameList.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        HashMap<String, String> removed = new HashMap<>();

        Iterator<Map.Entry<String, String>> entries = finalGameList.entrySet().iterator();
        while (entries.hasNext()){
            Map.Entry<String, String> entry = entries.next();
            //search for tags (multiplayer/coop), remove if not there "https://store.steampowered.com/api/appdetails?appids=xxxx"
            String sURL = "https://store.steampowered.com/api/appdetails?appids="+entry.getKey();
            URL url = new URL(sURL);
            URLConnection request = url.openConnection();
            request.connect();
            JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            String json = rootObj.toString();
            if ((!json.contains("Co-op") && !json.contains("Multi-player") && !json.contains("PvP")) || !json.contains("type\":\"game")){
                if (json.contains("success\":false")) {potentialErrors.add(entry.getValue());}
                removed.put(entry.getKey(), entry.getValue());
                //System.out.println(entry.getValue());
                //System.out.println(json);
                TimeUnit.SECONDS.sleep(10);
                entries.remove();
            }
        }
        System.out.println("After coop sorting, Games hashmap has size of: " + sharedGameNames.size());
        for (String s: finalGameList.values()){
            System.out.println(s);
        }
    }

    public void SharedAchievements() {
        //gets the games list from each OwnedGames and puts em in a list
        for (int i = 0; i < playerAchievementsList.size(); i++) {
            allAchievementsList.add((PlayerAchievements.PlayerStats) playerAchievementsList.get(i).playerstats.achievements);
        }
    }

    public List<OwnedGames> getOwnedGamesList() {
        return ownedGamesList;
    }

    public void setOwnedGamesList(List<OwnedGames> ownedGamesList) {
        this.ownedGamesList = ownedGamesList;
    }

    public void addOwnedGames(OwnedGames ownedGames){
        this.ownedGamesList.add(ownedGames);
    }

    public List<String> getSteamIdsList() {
        return steamIdsList;
    }

    public void setSteamIdsList(List<String> steamIdsList) {
        this.steamIdsList = steamIdsList;
    }
}

class PlayerAchievements{
    public PlayerStats playerstats;

    class PlayerStats {
        String steamID;
        String gameName;
        List<Achievements> achievements;

        class Achievements{
            String apiname;
            String achieved;
            String unlocktime;
        }
    }

}

class OwnedGames{
    public Response response;

    class Response{
        String game_count;
        public List<Games> games;

        class Games{
            String appid;
            String name;
            String playtime_forever;
            String img_icon_url;
            String img_logo_url;
        }
    }
}