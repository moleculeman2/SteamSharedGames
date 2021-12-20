import com.google.gson.*;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SteamSharedGamesApp {
    public static void main(String[] args) throws IOException, InterruptedException {

        String steamKey = "***REMOVED***";
        String yourId = "***REMOVED***";
        String steamFriendId1 = "76561198028272777"; //***REMOVED*** ID
        String steamFriendId2 = "***REMOVED***"; //***REMOVED*** ID
        //String steamFriendId1 = "***REMOVED***"; //***REMOVED***'s ID
        //String steamFriendId2 = "***REMOVED***"; //***REMOVED***'s ID
        List<String> steamIdsList = Arrays.asList(yourId, steamFriendId1, steamFriendId2);
        String getPlayerSummary = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="+steamKey+"&steamids="+yourId;
        String getFriendList = "http://api.steampowered.com/ISteamUser/GetFriendList/v0001/?key="+steamKey+"&steamid="+yourId+"&relationship=friend";
        String getOwnedGames = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+yourId+"&include_appinfo=true&format=json";
        String getPlayerAchievements = "http://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/?appid=440&key="+steamKey+"&steamid="+yourId;
        GameListBuilder gameListBuilder = new GameListBuilder(steamIdsList);

        for (String e: steamIdsList) {
            String sURL = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+e+"&include_appinfo=true&format=json";
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
            gameListBuilder.addOwnedGames(ownedGames);
            json = gson.toJson(ownedGames);
            //System.out.println(json);
        }

        //call method to parse owned games for both and create new list
        gameListBuilder.FindSharedGames();
        //call method to get achievements for each player for each game and find shared chievos
        gameListBuilder.FindSharedAchievements();


    }
}

class GameListBuilder {
    List<OwnedGames> ownedGamesList = new ArrayList<OwnedGames>();
    List<String> steamIdsList;
    List<String> potentialErrors = new ArrayList<String>();

    HashMap<String, GameInfo> finalGameList = new HashMap<String, GameInfo>();

    List<HashSet<String>> sharedGameNames = new ArrayList<HashSet<String>>();
    HashMap<String, String> penultimateGameList = new HashMap<>();
    List<OwnedGames.Response> allGamesList = new ArrayList<OwnedGames.Response>();

    public GameListBuilder(List<String> steamIdsList){
        this.steamIdsList = steamIdsList;
    }

    public void FindSharedGames() throws IOException, InterruptedException {
        //gets the games list from each OwnedGames and puts em in a list
        for (int i = 0; i < ownedGamesList.size(); i++) {
            allGamesList.add(ownedGamesList.get(i).response);
            sharedGameNames.add(new HashSet<String>());
        }
        //loops through each user's games list and puts the appid and name (separated by a "~") in a hashset for each user
        //for (int i = 0; i < allGamesList.get(1).games.size(); i++){
            //System.out.println(allGamesList.get(1).games.get(i).name);
           // System.out.println(allGamesList.get(1).games.get(i).appid);
       // }
        for (int i = 0; i < allGamesList.size(); i++) {
            for (int j = 0; j < allGamesList.get(i).games.size(); j++) {
                //System.out.println("i: " + i + "  j: " + j);
                //System.out.println(allGamesList.get(i).games.get(j).name);
                //System.out.println(allGamesList.get(i).games.get(j).appid);
                sharedGameNames.get(i).add(allGamesList.get(i).games.get(j).appid.concat("~").concat(allGamesList.get(i).games.get(j).name));
            }
        }
        //compares all users' games and only keeps those that they share
        for (int i = 0; i < sharedGameNames.size(); i++) {
            sharedGameNames.get(0).retainAll(sharedGameNames.get(i));
        }
        //separates the appid and app name, putting them as key and value in a hashmap
        for ( String s: sharedGameNames.get(0)) {
            penultimateGameList.put(s.substring(0,s.indexOf("~")), s.substring(s.indexOf("~")+1));
        }

        System.out.println("Before co-op sorting, penultimateGameList has size of: " + penultimateGameList.size());
        for (Map.Entry<String, String> entry : penultimateGameList.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        System.out.printf("Sorting will take %.2f minutes. (must query slowly or steam will block the connection) %n", (penultimateGameList.size()*1.75)/60);

        HashMap<String, String> removed = new HashMap<>();

        Iterator<Map.Entry<String, String>> entries = penultimateGameList.entrySet().iterator();

        while (entries.hasNext()){
            TimeUnit.MILLISECONDS.sleep(1750); //must delay to avoid, "429 Too Many Requests"
            Map.Entry<String, String> entry = entries.next();
            //search for tags (multiplayer/coop), remove if not there "https://store.steampowered.com/api/appdetails?appids=xxxx"
            String sURL = "https://store.steampowered.com/api/appdetails?appids="+entry.getKey();
            URL url = new URL(sURL);
            URLConnection request = url.openConnection();
            request.connect();
            JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            String json = rootObj.toString();
            if (json.contains("success\":false")) {potentialErrors.add(entry.getValue());}
            if ((!json.contains("Co-op") && !json.contains("Multi-player") && !json.contains("PvP")) || !json.contains("type\":\"game")){
                removed.put(entry.getKey(), entry.getValue());
                entries.remove();
            }
            else {
                for (int i = 0; i < steamIdsList.size(); i++){
                    for (int j = 0; j < allGamesList.get(i).games.size(); j++) {
                        //System.out.println("List ID is: " + allGamesList.get(i).games.get(j).appid + "  and entry key is: " + entry.getKey());
                        if (Integer.parseInt(allGamesList.get(i).games.get(j).appid) == Integer.parseInt(entry.getKey())){
                            if (i == 0){
                                this.finalGameList.put(entry.getKey(), new GameInfo(entry.getKey(), entry.getValue(),
                                        allGamesList.get(i).games.get(j).playtime_forever, ownedGamesList.size()));
                                j = allGamesList.get(i).games.size() + 1;
                            }
                            else{
                                this.finalGameList.get(entry.getKey()).addPlaytime(allGamesList.get(i).games.get(j).playtime_forever);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("After coop sorting, finalGameList has size of: " + finalGameList.size());
        for (GameInfo g: finalGameList.values()){
            System.out.println(g.getName() + " : " + g.getId());
        }

    }

    public void FindSharedAchievements() throws IOException{
        //gets each player's achievements for each game and saves them
        for (String e : steamIdsList) {
            System.out.println(steamIdsList.isEmpty());
            System.out.println(finalGameList.isEmpty());
            for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
                String sURL = "http://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/?appid="+entry.getKey()+
                        "&key=***REMOVED***&steamid="+e;
                URL url = new URL(sURL);
                URLConnection request = url.openConnection();
                request.connect();
                try {
                    JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
                    JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
                    String json = rootObj.toString();
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();
                    PlayerAchievements playerAchievements = gson.fromJson(json, PlayerAchievements.class);
                    entry.getValue().addAchievements(playerAchievements.playerstats.achievements);
                }
                catch (IOException io){
                    System.out.println("no chievies for:  " + sURL);
                }
            }
        }
        //compares each player's achievements to each other to see if they've played together.
        for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
            for (int i = 1; i < steamIdsList.size(); i++){
                System.out.println(entry.getValue().getName());
                System.out.println(entry.getValue().getAchievments().size());
                try {
                    if (entry.getValue().getAchievments().isEmpty() || entry.getValue().getAchievments().get(i).isEmpty()) {continue;}
                    for (int j = 0; j < entry.getValue().getAchievments().get(i).size(); j++) {
                        if (Integer.parseInt(entry.getValue().getAchievments().get(0).get(j).achieved) == 1) {
                            if (Integer.parseInt(entry.getValue().getAchievments().get(i).get(j).achieved) == 1) {
                                int user1 = Integer.parseInt(entry.getValue().getAchievments().get(0).get(j).unlocktime);
                                int user2 = Integer.parseInt(entry.getValue().getAchievments().get(i).get(j).unlocktime);
                                System.out.println(Math.abs(user1 - user2));
                                if (Math.abs(user1 - user2) < 86400) {
                                    entry.getValue().playedBefore.set(i, entry.getValue().playedBefore.get(i).intValue() + 1);
                                    System.out.println("chievies close together");
                                    j = entry.getValue().getAchievments().get(0).size() + 1;
                                }
                            }
                        }
                    }
                }
                catch (NullPointerException ne){continue;}
            }
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

class GameInfo{
    String id;
    String name;
    ArrayList<String> playtimes = new ArrayList<String>();
    ArrayList<List<PlayerAchievements.PlayerStats.Achievements>> achievements = new ArrayList<List<PlayerAchievements.PlayerStats.Achievements>>();
    public ArrayList<Integer> playedBefore = new ArrayList<Integer>();


    public GameInfo(String id, String name, String playtime, int users) {
        this.id = id;
        this.name = name;
        this.playtimes.add(playtime);
        for (int i = 0; i < users; i++){
            this.playedBefore.add(0);
        }
    }

    public String getId() {return id;}
    public void setId(String id) {this.id = id;}

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

    public void addPlaytime(String playtime) {this.playtimes.add(playtime);}
    public ArrayList<String>  getPlaytimes() {return playtimes;}

    public void addAchievements(List<PlayerAchievements.PlayerStats.Achievements> achievements)
        {this.achievements.add(achievements);}
    public ArrayList<List<PlayerAchievements.PlayerStats.Achievements>> getAchievments() {return achievements;}

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
            String name;
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

