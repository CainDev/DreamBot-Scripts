import org.dreambot.api.Client;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
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

    public void Screenshot(String status) {
        String scriptName = ScriptManager.getScriptManager().getCurrentScript().getSDNName();
        File dir = new File(scriptName);

        try {
            if (!dir.exists() || !dir.isDirectory()) {
                Logger.log("Creating script folder: " + dir.getAbsolutePath());
                dir.mkdir();
            }

            BufferedImage image = Client.getCanvasImage();
            Graphics2D g = image.createGraphics();

            // --- Draw text ---
            g.setFont(new Font("Arial", Font.BOLD, 14));
            g.setColor(new Color(255, 255, 255)); // white text
            g.drawString("Script: " + scriptName, 10, 100);
            g.drawString("Time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 10, 120);
            g.drawString("Script Status: " + status, 10, 140);
            g.dispose();

            LocalDateTime now = LocalDateTime.now();
            String formattedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd - HH.mm.ss"))
                    + String.format(".%02d", now.getNano() / 10000000);

            File output = new File(dir, formattedDateTime + ".png");
            ImageIO.write(image, "png", output);

            Logger.log("Saved screenshot to: " + output.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
