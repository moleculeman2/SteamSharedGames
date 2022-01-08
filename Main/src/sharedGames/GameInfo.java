package sharedGames;

import java.util.ArrayList;
import java.util.List;

class GameInfo {
    String id;
    String name;
    ArrayList<String> playtimes = new ArrayList<String>();
    ArrayList<List<PlayerAchievements.PlayerStats.Achievements>> achievements = new ArrayList<List<PlayerAchievements.PlayerStats.Achievements>>();
    public ArrayList<Integer> playedBefore = new ArrayList<Integer>();

    public GameInfo(String id, String name, String playtime, int users) {
        this.id = id;
        this.name = name;
        this.playtimes.add(playtime);
        for (int i = 0; i < users; i++) {
            this.playedBefore.add(0);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addPlaytime(String playtime) {
        this.playtimes.add(playtime);
    }

    public ArrayList<String> getPlaytimes() {
        return playtimes;
    }

    public void addAchievements(List<PlayerAchievements.PlayerStats.Achievements> achievements) {
        this.achievements.add(achievements);
    }

    public ArrayList<List<PlayerAchievements.PlayerStats.Achievements>> getAchievements() {
        if (achievements.isEmpty()) {
            return null;
        }
        return achievements;
    }

}
