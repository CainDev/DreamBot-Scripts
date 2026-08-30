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

    public String FormatMillis(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
