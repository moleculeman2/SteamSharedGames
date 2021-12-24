import com.google.gson.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.swing.*;
import java.io.IOException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SteamSharedGamesApp {
    public static void main(String[] args) throws IOException, InterruptedException {

        Reader reader = Files.newBufferedReader(Paths.get("/home/mgw/SteamSharedGames/Main/src/config.json"));
        Gson gson = new Gson();
        IdConfig idConfig = gson.fromJson(reader, IdConfig.class);
        String steamKey = idConfig.apiKey;
        List<String> steamUsernameList = new ArrayList<String>();
        List<String> steamIdsList = new ArrayList<String>();
        steamIdsList.add(idConfig.yourId);
        steamIdsList.add(idConfig.friendIds.get(2));
        steamIdsList.add(idConfig.friendIds.get(4));

        GameListBuilder gameListBuilder = new GameListBuilder(steamIdsList, idConfig);

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
            gson = builder.create();
            OwnedGames ownedGames = gson.fromJson(json, OwnedGames.class);
            gameListBuilder.addOwnedGames(ownedGames);
            //json = gson.toJson(ownedGames);

            sURL = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="+steamKey+"&steamids="+e;
            url = new URL(sURL);
            request = url.openConnection();
            request.connect();
            root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            json = rootObj.toString();
            String username = json.substring(json.indexOf("personaname\":\"")+14,json.indexOf("\",\"",json.indexOf("personaname\":\"")+14));
            System.out.println(username);
            steamUsernameList.add(username);
        }
        gameListBuilder.setSteamUsernameList(steamUsernameList);
        //call method to parse owned games for both and create new list
        gameListBuilder.FindSharedGames();
        //call method to get achievements for each player for each game and find shared chievos
        gameListBuilder.FindSharedAchievements();
        spreadSheetBuilder(gameListBuilder, steamUsernameList);

    }

    private static void spreadSheetBuilder(GameListBuilder gameListBuilder, List<String> steamUsernameList) {
        TreeMap<String, GameInfo> sorted = new TreeMap<String, GameInfo>();
        for (Map.Entry<String, GameInfo> entry : gameListBuilder.finalGameList.entrySet()){
            sorted.put(entry.getValue().getName(), entry.getValue());
        }
        String savePath = "/home/mgw/test.xls";
        gameListBuilder.finalGameList.clear();
        gameListBuilder.finalGameList.putAll(sorted);
        try (OutputStream fileOut = new FileOutputStream(savePath)) {
            Workbook wb = new HSSFWorkbook();
            Sheet sheet = wb.createSheet("Sheet");
            sheet.setColumnWidth(0, 25*256);
            Row row = sheet.createRow(0);
            row.setHeight((short) 525);
            Cell cell = row.createCell(0);
            cell.setCellValue("Game");
            CellStyle style = wb.createCellStyle();
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.THICK);
            Font font = wb.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
            for (int i = 1; i < steamUsernameList.size(); i++){
                sheet.setColumnWidth(i,steamUsernameList.get(i).length()*256 + 8*256);
                cell = row.createCell(i);
                cell.setCellValue("w/ " + steamUsernameList.get(i) +"?");
                cell.setCellStyle(style);
            }
            for (int i = 0; i < steamUsernameList.size(); i++){
                sheet.setColumnWidth(i+steamUsernameList.size(),steamUsernameList.get(i).length()*256 + 12*256);
                cell = row.createCell(i+steamUsernameList.size());
                cell.setCellValue(steamUsernameList.get(i) + " playtime");
                cell.setCellStyle(style);
            }

            int rowNum = 1;
            for (GameInfo g: gameListBuilder.finalGameList.values()){
                style = wb.createCellStyle();
                style.setBorderLeft(BorderStyle.THIN);
                style.setBorderRight(BorderStyle.THIN);
                CellStyle separatorStyle = wb.createCellStyle();
                separatorStyle.setBorderLeft(BorderStyle.THIN);
                separatorStyle.setBorderRight(BorderStyle.THICK);
                row = sheet.createRow(rowNum);
                cell = row.createCell(0);
                cell.setCellValue(g.getName());
                cell.setCellStyle(separatorStyle);
                /**
                 double average = 0;
                 for (int i = 1; i < steamUsernameList.size(); i++){
                 average += g.playedBefore.get(i);
                 }
                 average /= steamUsernameList.size()-1;
                 average = Math.round(average);
                 System.out.println((int)average);
                 switch ((int) average) {
                 case 0 -> {style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                 separatorStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());}
                 case 1 -> {style.setFillForegroundColor(IndexedColors.GOLD.getIndex());
                 separatorStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());}
                 case 2 -> {style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
                 separatorStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());}
                 case 3 -> {style.setFillForegroundColor(IndexedColors.RED.getIndex());
                 separatorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());}
                 default -> {style.setFillForegroundColor(IndexedColors.LIME.getIndex());
                 separatorStyle.setFillForegroundColor(IndexedColors.LIME.getIndex());}
                 }
                 style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                 separatorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                 **/
                for (int i = 1; i < steamUsernameList.size(); i++){
                    cell = row.createCell(i);
                    cell.setCellValue(playedBeforeConverter(g.playedBefore.get(i)));
                    if (i == steamUsernameList.size() -1 ){
                        cell.setCellStyle(separatorStyle);
                    }
                    else {cell.setCellStyle(style);}
                }
                for (int i = 0; i < steamUsernameList.size(); i++){
                    cell = row.createCell(i+steamUsernameList.size());
                    double hours = Math.floor(Double.parseDouble(g.getPlaytimes().get(i))/60);
                    double minutes = ((Double.parseDouble(g.getPlaytimes().get(i)) - (hours*60)));
                    cell.setCellValue(Math.round(hours)+" hrs "+Math.round(minutes)+" min");
                    if (i == steamUsernameList.size() -1 ){
                        cell.setCellStyle(separatorStyle);
                    }
                    else {cell.setCellStyle(style);}
                }
                rowNum++;
            }
            wb.write(fileOut);
        }catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static String playedBeforeConverter(int friend){
        switch (friend) {
            case 0 -> {return "Unlikely";}
            case 1 -> {return "Possibly";}
            case 2 -> {return "Likely";}
            case 3 -> {return "Very Likely";}
            default -> {return "No";}
        }
    }
}

class GameListBuilder {
    IdConfig idConfig;
    List<String> steamIdsList;
    List<String> steamUsernameList;
    List<OwnedGames> ownedGamesList = new ArrayList<OwnedGames>();
    List<String> potentialErrors = new ArrayList<String>();
    SortedMap<String, GameInfo> finalGameList = new TreeMap<String, GameInfo>();
    List<HashSet<String>> sharedGameNames = new ArrayList<HashSet<String>>();
    HashMap<String, String> penultimateGameList = new HashMap<>();
    List<OwnedGames.Response> allGamesList = new ArrayList<OwnedGames.Response>();

    public GameListBuilder(List<String> steamIdsList, IdConfig idConfig){
        this.steamIdsList = steamIdsList;
        this.idConfig = idConfig;
    }

    public void setSteamUsernameList(List<String> steamUsernameList){
        this.steamUsernameList = steamUsernameList;
    }

    public void FindSharedGames() throws IOException, InterruptedException {
        //gets the games list from each OwnedGames and puts em in a list
        for (int i = 0; i < ownedGamesList.size(); i++) {
            System.out.println(steamIdsList.get(i));
            allGamesList.add(ownedGamesList.get(i).response);
            sharedGameNames.add(new HashSet<String>());
        }
        //loops through each user's games list and puts the appid and name (separated by a "~") in a hashset for each user
        for (int i = 0; i < allGamesList.size(); i++) {
            for (int j = 0; j < allGamesList.get(i).games.size(); j++) {
                sharedGameNames.get(i).add(allGamesList.get(i).games.get(j).appid.concat("~").concat(allGamesList.get(i).games.get(j).name));
            }
        }
        //compares all users' games and only keeps those that they share
        sharedGameNames.forEach(sharedGameName -> sharedGameNames.get(0).retainAll(sharedGameName));
        //separates the appid and app name, putting them as key and value in a hashmap
        for ( String s: sharedGameNames.get(0)) {
            penultimateGameList.put(s.substring(0,s.indexOf("~")), s.substring(s.indexOf("~")+1));
        }

        System.out.println("Before co-op sorting, penultimateGameList has size of: " + penultimateGameList.size());
        for (Map.Entry<String, String> entry : penultimateGameList.entrySet()){
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
        double minutes = Math.floor((penultimateGameList.size()*1.55)/60);
        double seconds = (((penultimateGameList.size()*1.55)/60) - minutes) * 60;
        System.out.println("Sorting will take ~"+Math.round(minutes)+" minutes and "+Math.round(seconds)+" seconds. (must query slowly or steam will block the connection)");

        HashMap<String, String> removed = new HashMap<>();

        Iterator<Map.Entry<String, String>> entries = penultimateGameList.entrySet().iterator();

        while (entries.hasNext()){
            TimeUnit.MILLISECONDS.sleep(1550); //must delay to avoid, "429 Too Many Requests"
            Map.Entry<String, String> entry = entries.next();
            //search for tags (multiplayer/coop), remove if not there "https://store.steampowered.com/api/appdetails?appids=xxxx"
            String sURL = "https://store.steampowered.com/api/appdetails?appids="+entry.getKey();
            URL url = new URL(sURL);
            URLConnection request = url.openConnection();
            request.connect();
            JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
            String json = rootObj.toString();
            if (json.contains("success\":false")) {
                potentialErrors.add(entry.getValue());
                System.out.println("steam gave this game url an error: " + entry.getValue());
            }
            if ((!json.contains("Co-op") && !json.contains("Multi-player") && !json.contains("PvP")) && !json.contains("Multijugador") || !json.contains("type\":\"game")){
                System.out.println("removing singleplayer game: " + entry.getValue());
                removed.put(entry.getKey(), entry.getValue());
                entries.remove();
            }
            else {
                //loops through each user, searching for current game id. Saves the playtime for that game and user.
                for (int i = 0; i < steamIdsList.size(); i++){
                    for (int j = 0; j < allGamesList.get(i).games.size(); j++) {
                        if (Integer.parseInt(allGamesList.get(i).games.get(j).appid) == Integer.parseInt(entry.getKey())){
                            if (i == 0){
                                System.out.println("adding co-op game time to user1: " + entry.getValue());
                                this.finalGameList.put(entry.getKey(), new GameInfo(entry.getKey(), entry.getValue(),
                                        allGamesList.get(i).games.get(j).playtime_forever, ownedGamesList.size()));
                                j = allGamesList.get(i).games.size() + 1; //end loop, skip to next user
                            }
                            else{
                                System.out.println("adding co-op game time to friend" + i +": " + entry.getValue());
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
        for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
            for (int i = 1; i < steamIdsList.size(); i++){
                float user1 = Float.parseFloat(entry.getValue().getPlaytimes().get(0));
                float user2 = Float.parseFloat(entry.getValue().getPlaytimes().get(i));
                if (user1 > 0 && user2 > 0){
                    if (Math.abs(user1-user2)<600){ //difference in playtime less than 10 hours
                        entry.getValue().playedBefore.set(i, entry.getValue().playedBefore.get(i).intValue() + 1);
                    }
                }
                else { entry.getValue().playedBefore.set(i, entry.getValue().playedBefore.get(i).intValue() - 10);}
            }
        }
    }

    public void FindSharedAchievements() throws IOException{
        //gets each player's achievements for each game and saves them
        for (String e : steamIdsList) {
            System.out.println("current user is: " + e);
            System.out.println(steamIdsList.isEmpty());
            System.out.println(finalGameList.isEmpty());
            for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
                String sURL = "http://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/?appid="+entry.getKey()+
                        "&key="+idConfig.apiKey+"&steamid="+e;
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
                    System.out.println(gson.fromJson(json, PlayerAchievements.class) + " : " + json);
                    if(playerAchievements.playerstats.achievements != null && playerAchievements.playerstats.achievements.size() > 0){
                        entry.getValue().addAchievements(playerAchievements.playerstats.achievements);
                    }
                    else {System.out.println("no chievies for:  " + entry.getValue().getName());}
                }
                catch (IOException io){
                    System.out.println("IOerror: no chievies for (or private profile):  " + entry.getValue().getName());
                    entry.getValue().addAchievements(new ArrayList<PlayerAchievements.PlayerStats.Achievements>());
                }
                catch (NullPointerException ne){
                    System.out.println("NPerror: no chievies for:  " + entry.getValue().getName());
                }
            }
        }
        //compares each player's achievements to each other to see if they've played together based on achieved dates
        for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
            for (int i = 1; i < steamIdsList.size(); i++){
                try {
                    if (entry.getValue().achievements == null || entry.getValue().getAchievements().isEmpty() || entry.getValue().getAchievements().get(0).isEmpty()) {continue;}
                    for (int j = 0; j < entry.getValue().getAchievements().get(i).size(); j++) {
                        if (Integer.parseInt(entry.getValue().getAchievements().get(0).get(j).achieved) == 1) {
                            if (Integer.parseInt(entry.getValue().getAchievements().get(i).get(j).achieved) == 1) {
                                int user1 = Integer.parseInt(entry.getValue().getAchievements().get(0).get(j).unlocktime);
                                int user2 = Integer.parseInt(entry.getValue().getAchievements().get(i).get(j).unlocktime);
                                if (Math.abs(user1 - user2) < 86400) {
                                    if (entry.getValue().playedBefore.get(i) < 0){
                                        entry.getValue().playedBefore.set(i, 2);
                                    }
                                    else{
                                        entry.getValue().playedBefore.set(i, entry.getValue().playedBefore.get(i).intValue() + 2);
                                    }
                                    System.out.println("chievies close together " +
                                            steamUsernameList.get(0) +"/"+steamUsernameList.get(i) + "for: " + entry.getValue().getName());
                                    j = entry.getValue().getAchievements().get(0).size() + 1;
                                }
                            }
                        }
                    }
                }
                catch (NullPointerException ne){
                    System.out.println("got a null pointer here for: " + entry.getValue().getName());}
            }
        }
        for (Map.Entry<String, GameInfo> entry : finalGameList.entrySet()){
            System.out.println("Game: " + entry.getValue().getName());
            for (int i = 1; i < steamIdsList.size(); i++){
                int friend = entry.getValue().playedBefore.get(i);
                System.out.print("    Played with " + steamUsernameList.get(i) + "? ");
                switch (friend) {
                    case 0 -> System.out.println("Unlikely");
                    case 1 -> System.out.println("Possibly");
                    case 2 -> System.out.println("Likely");
                    case 3 -> System.out.println("Very Likely");
                    default -> System.out.println("No");
                }
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
    public ArrayList<List<PlayerAchievements.PlayerStats.Achievements>> getAchievements()
    {
        if (achievements.isEmpty()){
            return null;
        }
        return achievements;
    }

}

class IdConfig{
    String apiKey;
    String yourId;
    ArrayList<String> friendIds;
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

