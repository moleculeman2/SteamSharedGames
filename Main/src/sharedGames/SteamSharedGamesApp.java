package sharedGames;

import com.google.gson.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.apache.logging.log4j.*;

public class SteamSharedGamesApp {
    public static void main(String[] args) throws IOException, InterruptedException {

        Reader reader = Files.newBufferedReader(Paths.get("/home/mgw/SteamSharedGames/Main/src/config.json"));
        Gson gson = new Gson();
        IdConfig idConfig = gson.fromJson(reader, IdConfig.class);
        String steamKey = idConfig.apiKey;
        List<String> steamUsernameList = new ArrayList<String>();
        List<String> steamIdsList = new ArrayList<String>();
        steamIdsList.add(idConfig.yourId);
        steamIdsList.add(idConfig.friendIds.get(0));
        steamIdsList.add(idConfig.friendIds.get(1));

        GameListBuilder gameListBuilder = new GameListBuilder(steamIdsList, idConfig);

        for (String e: steamIdsList) {
            String stringURL = "http://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/?key="+steamKey+"&steamid="+e+"&include_appinfo=true&format=json";
            String json = getJsonStringFromUrlRequest(stringURL);
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            gson = builder.create();
            OwnedGames ownedGames = gson.fromJson(json, OwnedGames.class);
            gameListBuilder.addOwnedGames(ownedGames);
            //json = gson.toJson(ownedGames);

            stringURL = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key="+steamKey+"&steamids="+e;
            json = getJsonStringFromUrlRequest(stringURL);
            String username = json.substring(json.indexOf("personaname\":\"")+14,json.indexOf("\",\"",json.indexOf("personaname\":\"")+14));
            System.out.println(username);
            steamUsernameList.add(username);
        }
        gameListBuilder.setSteamUsernameList(steamUsernameList);
        gameListBuilder.FindSharedGames();
        gameListBuilder.FindSharedAchievements();
        buildSpreadsheet(gameListBuilder, steamUsernameList);

    }

    static String getJsonStringFromUrlRequest(String sURL) throws IOException {
        URL url = new URL(sURL);
        URLConnection request = url.openConnection();
        request.connect();
        JsonElement root = JsonParser.parseReader(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
        JsonObject rootObj = root.getAsJsonObject(); //May be an array, may be an object.
        return rootObj.toString();
    }

    private static void buildSpreadsheet(GameListBuilder gameListBuilder, List<String> steamUsernameList) {
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

