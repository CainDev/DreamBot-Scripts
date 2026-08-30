import org.dreambot.api.utilities.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Utility {
    public final Random Rnd = new Random();

    public int GenerateRandomBound(int lower, int higher) {
        if (lower > higher) {
            lower = 300;
            higher = 700;
        } // Kinda Pointless Failsafe
        return Rnd.nextInt((higher - lower) + 1) + lower;
    }

    public void CreateTxtFile(String AccountName, String TargetLevel, String TargetSkill, String AccountIdentifier) {
        // Define folder name
        File dir = new File("Shrimp Fisher Completions");

        // Create folder if it doesn’t exist
        if (!dir.exists()) {
            Logger.log("Creating script folder: " + dir.getAbsolutePath());
            dir.mkdir();
        }

        // Define the output file path inside the folder
        File file = new File(dir, AccountName + "-" + TargetLevel + ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            // Write formatted data
            writer.write("Account: " + AccountName +
                    " Completed: " + TargetLevel +
                    " " + TargetSkill +
                    " @ " + ReturnUKTime(0) +
                    " (" + AccountIdentifier + ")"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String ReturnUKTime(int daysToAdd) {
        ZonedDateTime ukTime = ZonedDateTime.now(ZoneId.of("Europe/London")).plusDays(daysToAdd);
        return ukTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss z"));
    }

    public boolean GenerateRandomBoolean() {
        return Rnd.nextBoolean();
    }

    public int RoundToNearest5(int number) {
        return 5 * Math.round(number / 5.0f);
    }

    public boolean RollRandomChance(int lower, int upper) {
        int chance = GenerateRandomBound(lower, upper + 1);
        int roll = Rnd.nextInt(100) + 1;
        return roll <= chance;
    }

    public String FormatMillis(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
