package sharedGames;

import java.util.List;

class PlayerAchievements {
    public PlayerStats playerstats;

    class PlayerStats {
        String steamID;
        String gameName;
        List<Achievements> achievements;

        class Achievements {
            String apiname;
            String achieved;
            String unlocktime;
            String name;
        }
    }
}
