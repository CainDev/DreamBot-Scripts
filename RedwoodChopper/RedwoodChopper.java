import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.*;
import org.dreambot.api.script.listener.ItemContainerListener;
import org.dreambot.api.utilities.*;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@ScriptManifest(
        name = "Redwood Chopper",
        description = "Chops Redwoods @ Woodcutting Guild",
        author = "Cain",
        version = 1.0,
        category = Category.MONEYMAKING,
        image = "https://i.ibb.co/tTTpgR9W/redwood.png"
)

public class RedwoodChopper extends AbstractScript implements ItemContainerListener {

    private long worldHopSet = 960_000;
    private long lastWorldHop = 0;
    private long nextWorldHop = 960_000;
    private int hopWorldOffset = 30_000;

    private int reWalk = 7;
    private Timer Timer = new Timer();
    private int logCount = 0;
    private int nestCount = 0;

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Calibri", Font.BOLD, 15));
        g.drawString("Cain's Redwood Chopper", 10, 35);

        g.setFont(new Font("Calibri", Font.PLAIN, 15));
        g.drawString("Elapsed: " + Timer.formatTime(), 10, 47);

        g.drawString("Total Logs: " + String.format("%,d", logCount), 10, 59);
        g.drawString("Logs/hr: " + String.format("%,d", Timer.getHourlyRate(logCount)), 10, 71);
        g.drawString("Total Nests: " + String.format("%,d", nestCount), 10, 85);
        g.drawString("Nests/hr: " + String.format("%,d", Timer.getHourlyRate(nestCount)), 10, 97);
        g.drawString("Total GP: " + String.format("%,d", totalGp()), 10, 109);
        g.drawString("GP/hr: " + String.format("%,d", Timer.getHourlyRate(totalGp())), 10, 121);
    }

    private int totalGp() {
        int nestGold = LivePrices.get(Items.CRUSHED_NEST) * nestCount;
        int logGold = LivePrices.get(Items.REDWOOD_LOGS) * logCount;

        return nestGold + logGold;
    }

    @Override
    public void onInventoryItemAdded(Item item) {
        if (item == null) return;
        if (item.getName().equals("Redwood logs")) logCount++;
        if (item.getName().equals("Bird nest")) nestCount++;
    }

    @Override
    public int onLoop() {
        // Wait until Logged In
        if (!Client.isLoggedIn() || Client.getGameState() == GameState.HOPPING) {
            Logger.log("Hopping/Not logged in, waiting...");
            return Calculations.random(750, 1500);
        }

        // World Hop
        if (shouldWorldHop()) {
            worldHop();
            return Calculations.random(300, 550);
        }

        // Walk to Bank & Open Bank
        if (shouldWalkToBank()) {
            walkToBank();
            return Calculations.random(300, 550);
        }

        // Handle Banking
        if (shouldHandleBank()) {
            handleBanking();
            return Calculations.random(300, 550);
        }

        // Drop Scroll Boxes
        if (shouldDropScrollBox()) {
            dropScrollBox();
            return Calculations.random(300, 550);
        }

        // Walk to Redwoods
        if (shouldWalkToRedwood()) {
            walkToRedwood();
            return Calculations.random(300, 550);
        }

        // loot Birds nest
        if (shouldLootBirdsNest()) {
            lootBirdsNest();
            return Calculations.random(200, 300);
        }

        // Cut Trees
        if (shouldCutTree()) {
            cutTree();
            return Calculations.random(300, 550);
        }

        return Calculations.random(300, 500);
    }

    // DROP SCROLLBOX
    private boolean shouldDropScrollBox() {
        return Inventory.contains(Items.ALL_SCROLL_BOXES);
    }

    private void dropScrollBox() {
        for (int boxes : Items.ALL_SCROLL_BOXES) {
            if (!Inventory.contains(boxes)) continue;
            int boxCount = Inventory.count(boxes);
            if (Inventory.drop(boxes)) {
                Logger.log("Dropping: " + new Item(boxes, 0).getName());
                Sleep.sleepUntil(() -> Inventory.count(boxes) == boxCount - 1, Calculations.random(750, 1250));
            }
            return;
        }
    }

    // LOOT NEST
    private boolean shouldLootBirdsNest() {
        List<GroundItem> birdNests = GroundItems.all(groundItem -> groundItem.getName().equals("Bird nest") && groundItem.canReach());
        return !birdNests.isEmpty();
    }

    private void lootBirdsNest() {
        if (Inventory.isFull()) {
            List<Item> redwoodLogs = Inventory.all(item -> item.getId() == Items.REDWOOD_LOGS);
            Collections.shuffle(redwoodLogs);

            if (redwoodLogs.get(0) == null) return;
            if (Inventory.interact(redwoodLogs.get(0), "Drop")) {
                Logger.log("Dropping Log");
                Sleep.sleepUntil(() -> !Inventory.isFull(), Calculations.random(750, 1250));
            }

            return;
        }

        List<GroundItem> birdNests = GroundItems.all(groundItem -> groundItem.getName().equals("Bird nest") && groundItem.canReach());
        if (birdNests.get(0).interact("Take")) {
            Logger.log("Looting Nest");
            Sleep.sleepUntil(() -> !birdNests.get(0).exists(), () -> Players.getLocal().isMoving(), Calculations.random(750, 1250), 100);
        }
    }

    // CUT TREE
    private boolean shouldCutTree() {
        return Locations.REDWOOD_TREE.contains(Players.getLocal().getServerTile());
    }

    private void cutTree() {
        GameObject redwoodTree = GameObjects.closest(tree -> tree.hasAction("Cut") && tree.canReach());
        if (redwoodTree == null) return;
        if (Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == 2846) {
            Logger.log("Chopping Tree");

            if (Mouse.isMouseInScreen()) {
                if (Mouse.moveOutsideScreen(true)) {
                    Logger.log("Moving mouse outside screen");
                    Sleep.sleepUntil(() -> !Mouse.isMouseInScreen(), Calculations.random(750, 1250));
                }
            }

            Sleep.sleep(Calculations.random(200, 400), Calculations.random(600, 1000));
            return;
        }

        if (redwoodTree.interact("Cut")) {
            Logger.log("Clicking Tree");
            Sleep.sleepUntil(() -> Players.getLocal().isAnimating() && Players.getLocal().getAnimation() == 2846, () -> Players.getLocal().isMoving(), Calculations.random(750, 1250), 100);
            Sleep.sleepTicks(Calculations.random(1, 3));
        }
    }

    // World Hop
    private boolean shouldWorldHop() {
        if (lastWorldHop == 0) {
            lastWorldHop = Timer.elapsed();
            return false;
        }

        return Timer.elapsed() >= lastWorldHop + nextWorldHop && !Bank.isOpen();
    }

    private void worldHop() {
        Logger.log("Hopping World");
        Random random = new Random();

        long offset = random.nextInt(hopWorldOffset + 1);
        boolean hopLater = random.nextBoolean();

        nextWorldHop = hopLater
                ? worldHopSet + offset
                : worldHopSet - offset;

        lastWorldHop = Timer.elapsed();

        Logger.log("Next hop in: " + nextWorldHop + " ms");

        WorldHopper.hopWorld(Worlds.getRandomWorld(w ->
                w.isNormal()
                        && w.isMembers()
                        && !w.isPVP()
                        && w.getMinimumLevel() <= Skills.getTotalLevel()
                        && w.getPopulation() < 1800
        ));
    }

    // HANDLE BANKING
    private boolean shouldHandleBank() {
        return Bank.isOpen();
    }

    private void handleBanking() {
        if (!Inventory.onlyContains(Items.EXCLUDED_ITEMS)) {
            if (Bank.depositAllExcept(Items.EXCLUDED_ITEMS)) {
                Logger.log("Depositing Logs");
                Sleep.sleepUntil(() -> Inventory.onlyContains(Items.EXCLUDED_ITEMS), Calculations.random(750, 1250));
            }
            return;
        }

        if (Bank.contains(Items.ALL_SCROLL_BOXES)) {
            List<Integer> allBoxes = Arrays.stream(Items.ALL_SCROLL_BOXES).boxed().collect(Collectors.toList());
            Collections.shuffle(allBoxes);

            for (Integer boxes : allBoxes) {
                if (!Bank.contains(boxes)) continue;
                if (Bank.withdrawAll(boxes)) {
                    Logger.log("Withdrawing: " + new Item(boxes, 0).getName());
                    Sleep.sleepUntil(() -> Inventory.contains(boxes), Calculations.random(750, 1250));
                }
                return;
            }
        }

        if (Bank.isOpen()) {
            if (Bank.close()) {
                Logger.log("Closing Bank");
                Sleep.sleepUntil(() -> !Bank.isOpen(), Calculations.random(750, 1250));
            }
        }
    }

    // WALK TO BANK
    private boolean shouldWalkToBank() {
        return Inventory.isFull() && !Bank.isOpen();
    }

    private void walkToBank() {
        if (shouldLootBirdsNest()) {
            lootBirdsNest();
            return;
        }

        GameObject ladder = GameObjects.closest(obj -> obj.getName().equals("Rope ladder") && obj.hasAction("Climb-down"));
        if (ladder != null && Locations.REDWOOD_TREE.contains(Players.getLocal())) {
            if (ladder.interact("Climb-down")) {
                Logger.log("Clicked on Ladder, Climbing down the ladder");
                Sleep.sleepUntil(() -> !Locations.REDWOOD_TREE.contains(Players.getLocal().getServerTile()), () -> Players.getLocal().isMoving(), Calculations.random(750, 1250), 100);
            }
        }

        GameObject bankChest = GameObjects.closest(obj -> obj.getName().equals("Bank chest"));
        if (bankChest == null || bankChest.distance(Players.getLocal().getServerTile()) > 15) {
            if (Walking.shouldWalk(reWalk)) {
                Logger.log("Walking to Bank");
                if (Locations.WOODCUTTING_GUILD_BANK.contains(Walking.getDestination())) return;
                if (Walking.walk(Locations.WOODCUTTING_GUILD_BANK.getRandomTile())) {
                    reWalk = Calculations.random(5, 12);
                    Sleep.sleep(150, 500);
                }
            }
        } else {
            if (bankChest.interact("Use")) {
                Logger.log("Opening Bank");
                Sleep.sleepUntil(() -> Bank.isOpen(), () -> Players.getLocal().isMoving(), Calculations.random(750, 1250), 100);
            }
        }
    }

    // WALK TO REDWOOD TREE
    private boolean shouldWalkToRedwood() {
        return !Inventory.isFull() && !Locations.REDWOOD_TREE.contains(Players.getLocal().getServerTile());
    }

    private void walkToRedwood() {
        GameObject ladder = GameObjects.closest(obj -> obj.getName().equals("Rope ladder") && obj.hasAction("Climb-up"));
        if (ladder != null) {
            if (ladder.interact("Climb-up")) {
                Logger.log("Clicked on Ladder, Climbing up the ladder");
                Sleep.sleepUntil(() -> Locations.REDWOOD_TREE.contains(Players.getLocal().getServerTile()), () -> Players.getLocal().isMoving(), Calculations.random(750, 1250), 100);
            }
        }
    }
}
