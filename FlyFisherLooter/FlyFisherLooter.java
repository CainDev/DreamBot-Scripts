import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.World;
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
        name = "Fish Looter",
        description = "Loots fishies at Barbarian Outpost.",
        author = "ImMac",
        version = 1.0,
        category = Category.MONEYMAKING,
        image = ""
)

public class FlyFisherLooter extends AbstractScript implements ItemContainerListener {

    // -- Constants
    private final Utility Util = new Utility();
    private final Timer Timer = new Timer();

    private int ReWalkValue = 0;
    private int ReRunValue = 0;
    private boolean SmartMouseEnabled = true;

    private final String[] ItemsToLoot = {"Raw trout", "Salmon", "Trout", "Raw salmon"};
    private final List<String> LootList = Arrays.asList(ItemsToLoot);
    private final List<Tile> TilesToLoot = Arrays.asList(
            // -- Fishing Spots
            new Tile(3109, 3434, 0),
            new Tile(3109, 3433, 0),
            new Tile(3109, 3432, 0),

            new Tile(3103, 3425, 0),
            new Tile(3103, 3424, 0),

            // -- Fire Spots
            new Tile(3106, 3433, 0),
            new Tile(3105, 3432, 0)
    );

    private int TotalBankedFish = 0;
    private int TotalFishValue = 0;

    private int TotalRawTrout = 0;
    private int TotalCollectedRawTrout = 0;

    private int TotalCookedTrout = 0;
    private int TotalCollectedTrout = 0;

    private int TotalRawSalmon = 0;
    private int TotalCollectedRawSalmon = 0;

    private int TotalCookedSalmon = 0;
    private int TotalCollectedSalmon = 0;

    private int TotalFishCollected = 0;

    // -- Areas
    private final Area BarbFishingArea = new Area(3109, 3444, 3098, 3422);
    private final Area EdgevilleBank = new Area(3098, 3494, 3091, 3488);


    // -- Core
    @Override
    public void onStart(String... args) {
        Timer.start();

        // SmartMouse V2
        if (args[0] != null)
            SmartMouseEnabled = Boolean.parseBoolean(args[0]);

        if (SmartMouseEnabled) {
            Mouse.setMouseAlgorithm(new SmartMouseMultiDir());
            Logger.log("Smart Mouse Enabled!");
        }
    }

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Calibri", Font.BOLD, 15));
        g.drawString("Mac's Fish Looter", 10, 35);

        g.setFont(new Font("Calibri", Font.PLAIN, 15));
        g.drawString("Elapsed: " + Timer.formatTime(), 10, 47);

        g.drawString("Total Fish: " + String.format("%,d", TotalBankedFish), 10, 59);
        g.drawString("Total Fish Value: " + String.format("%,d", TotalFishValue), 10, 71);

        g.drawString("Banked Raw Trout: " + String.format("%,d", TotalRawTrout), 10, 83);
        g.drawString("Banked Trout: " + String.format("%,d", TotalCookedTrout), 10, 95);

        g.drawString("Banked Raw Salmon: " + String.format("%,d", TotalRawSalmon), 10, 107);
        g.drawString("Banked Salmon: " + String.format("%,d", TotalCookedSalmon), 10, 119);

        // --

        g.setFont(new Font("Calibri", Font.BOLD, 15));
        g.drawString("Fish Collection Stats", 10, 143);

        g.setFont(new Font("Calibri", Font.PLAIN, 15));
        g.drawString("Total Fish Collected: " + String.format("%,d", TotalFishCollected), 10, 155);
        g.drawString("GP/hr: " + String.format("%,d", GetGoldPerHour()), 10, 167);

        g.drawString("Raw Trout Collected: " + String.format("%,d", TotalCollectedRawTrout), 10, 179);
        g.drawString("Raw Trout/hr: " + String.format("%,d", Timer.getHourlyRate(TotalCollectedRawTrout)), 10, 191);

        g.drawString("Trout Collected: " + String.format("%,d", TotalCollectedTrout), 10, 203);
        g.drawString("Trout/hr: " + String.format("%,d", Timer.getHourlyRate(TotalCollectedTrout)), 10, 215);

        g.drawString("Raw Salmon Collected: " + String.format("%,d", TotalCollectedRawSalmon), 10, 227);
        g.drawString("Raw Salmon/hr: " + String.format("%,d", Timer.getHourlyRate(TotalCollectedRawSalmon)), 10, 239);

        g.drawString("Salmon Collected: " + String.format("%,d", TotalCollectedSalmon), 10, 251);
        g.drawString("Salmon/hr: " + String.format("%,d", Timer.getHourlyRate(TotalCollectedSalmon)), 10, 263);
    }

    @Override
    public void onInventoryItemAdded(Item item) {
        if (item == null) return;

        switch (item.getName()) {
            case "Raw trout":
                TotalCollectedRawTrout++;
                TotalFishCollected++;
                break;
            case "Trout":
                TotalCollectedTrout++;
                TotalFishCollected++;
                break;
            case "Raw salmon":
                TotalCollectedRawSalmon++;
                TotalFishCollected++;
                break;
            case "Salmon":
                TotalCollectedSalmon++;
                TotalFishCollected++;
                break;

        }
    }

    @Override
    public int onLoop() {
        if (ReWalkValue == 0) ReWalkValue = Util.GenerateRandomBound(6, 20);
        if (ReRunValue == 0) ReRunValue = Util.GenerateRandomBound(5, 35);

        if (Client.isLoggedIn()) {
            switch (CurrentState()) {
                case CHANGING_WORLD:
                    Logger.log("Hopping to better World");
                    if (!WorldHopper.isWorldHopperOpen()) {
                        if (WorldHopper.openWorldHopper()) {
                            Sleep.sleepUntil(() -> WorldHopper.isWorldHopperOpen(), Util.GenerateRandomBound(2500, 5000));
                            break;
                        }
                    }

                    if (WorldHopper.isWorldHopperOpen()) {
                        int World301Pop = 0;
                        int World308Pop = 0;

                        for (World world : Worlds.all()) {
                            if (world.getRealId() == 301)
                                World301Pop = world.getPopulation();

                            if (world.getRealId() == 308)
                                World308Pop = world.getPopulation();
                        }

                        if (World301Pop >= World308Pop) {
                            if (WorldHopper.hopWorld(301)) {
                                Sleep.sleepUntil(() -> Worlds.getCurrentWorld() == 301, Util.GenerateRandomBound(5000, 10000));
                                break;
                            }
                        } else {
                            if (WorldHopper.hopWorld(308)) {
                                Sleep.sleepUntil(() -> Worlds.getCurrentWorld() == 308, Util.GenerateRandomBound(5000, 10000));
                                break;
                            }
                        }
                    }

                    break;

                case LOCATING_FISH:
                    Logger.log("Locating & Looting Fish");

                    ShouldToggleRun();

                    if (!Tabs.isOpen(Tab.INVENTORY)) {
                        if (Tabs.openWithMouse(Tab.INVENTORY)) {
                            Sleep.sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), Util.GenerateRandomBound(2000, 5000));
                            break;
                        }
                    }

                    // Grab all Ground Items
                    List<GroundItem> FloorItems = GroundItems.all(i -> TilesToLoot.contains(i.getTile()) && LootList.contains(i.getName()));

                    // Group Them by Tile
                    Map<Tile, List<GroundItem>> ItemsByTile = FloorItems.stream().collect(Collectors.groupingBy(GroundItem::getTile));

                    // Sort Tiles
                    Tile BestTile = ItemsByTile.entrySet().stream().max(Comparator.comparingInt(e -> e.getValue().size())).map(Map.Entry::getKey).orElse(null);

                    if (BestTile != null) {
                        List<GroundItem> ItemsOnTile = ItemsByTile.get(BestTile);

                        for (GroundItem Fish : ItemsOnTile) {
                            if (Fish != null && Fish.canReach() && !Players.getLocal().isMoving()) {
                                if (Inventory.isFull()) {
                                    break;
                                }

                                if (Fish.interact("Take")) {
                                    Sleep.sleepUntil(() -> !Fish.exists(), Util.GenerateRandomBound(Util.GenerateRandomBound(50, 125), Util.GenerateRandomBound(150, 275)));
                                }
                            }
                        }
                    } else {
                        Logger.log("Waiting for Fish to Spawn");
                    }

                    break;

                case WALKING_TO_FISHING:
                    Logger.log("Walking to Fishing Spots");

                    ShouldToggleRun();

                    if (!Tabs.isOpen(Tab.INVENTORY)) {
                        if (Tabs.openWithMouse(Tab.INVENTORY)) {
                            Sleep.sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), Util.GenerateRandomBound(2000, 5000));
                            break;
                        }
                    }

                    if (!BarbFishingArea.contains(Walking.getDestination())) {
                        if (Walking.shouldWalk(ReWalkValue)) {
                            if (Walking.walk(BarbFishingArea.getRandomTile())) {
                                ReWalkValue = Util.GenerateRandomBound(4, 15);
                                ReRunValue = Util.GenerateRandomBound(10, 40);
                                Sleep.sleep(300, Util.GenerateRandomBound(500, 900));
                            }
                        }
                    }

                    break;

                case BANKING:
                    Logger.log("Banking");

                    if (Inventory.contains(ItemsToLoot) || !Inventory.isEmpty()) {
                        if (Bank.depositAllItems()) {
                            Sleep.sleepUntil(() -> Inventory.isEmpty(), Util.GenerateRandomBound(2000, 5000));
                            TotalRawTrout = Bank.count("Raw trout");
                            TotalCookedTrout = Bank.count("Trout");
                            TotalRawSalmon = Bank.count("Raw salmon");
                            TotalCookedSalmon = Bank.count("Salmon");
                            TotalBankedFish = TotalRawTrout + TotalCookedTrout + TotalRawSalmon + TotalCookedSalmon;
                            ReturnFishValue();
                            break;
                        }
                    }

                    break;

                case LOCATING_BANK:
                    Logger.log("Locating Bank");

                    List<GameObject> BankBooths = GameObjects.all("Bank Booth");
                    Collections.shuffle(BankBooths); // Small randomisation

                    for (GameObject BankBooth : BankBooths) {
                        if (BankBooth.distance() <= 3 && BankBooth.canReach() && EdgevilleBank.contains(BankBooth.getTile())) {
                            if (BankBooth.interact("Bank")) {
                                Sleep.sleepUntil(() -> Bank.isOpen(), Util.GenerateRandomBound(3500, 7000));
                                return Util.GenerateRandomBound(700, 1000);
                            }
                        }
                    }

                    // Fallback if no banks are closer than 5 tiles, very unlikely
                    if (BankBooths.get(0).interact("Bank")) {
                        Sleep.sleepUntil(() -> Bank.isOpen(), Util.GenerateRandomBound(2000, 5000));
                        break;
                    }

                    break;

                case WALKING_TO_BANK:
                    Logger.log("Walking to Bank");

                    ShouldToggleRun();

                    if (!Tabs.isOpen(Tab.INVENTORY)) {
                        if (Tabs.openWithMouse(Tab.INVENTORY)) {
                            Sleep.sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), Util.GenerateRandomBound(2000, 5000));
                            break;
                        }
                    }

                    if (!EdgevilleBank.contains(Walking.getDestination())) {
                        if (Walking.shouldWalk(ReWalkValue)) {
                            if (Walking.walk(EdgevilleBank.getRandomTile())) {
                                ReWalkValue = Util.GenerateRandomBound(4, 15);
                                ReRunValue = Util.GenerateRandomBound(10, 40);
                                Sleep.sleep(300, Util.GenerateRandomBound(500, 900));
                            }
                        }
                    }

                    break;

                case WAITING:
                    Logger.log("Waiting");
                    break;
            }

        }

        return Util.GenerateRandomBound(400, 600);
    }

    private BotState CurrentState() {
        Tile PlayerTile = Players.getLocal().getTile();

        // -- Hop World
//        if (!LootingWorld())
//            return BotState.CHANGING_WORLD;

        // -- Loot Fish
        if (BarbFishingArea.contains(PlayerTile) && !Inventory.isFull())
            return BotState.LOCATING_FISH;

        if (!BarbFishingArea.contains(PlayerTile) && !Inventory.isFull())
            return BotState.WALKING_TO_FISHING;

        // -- Bank Fish
        if (EdgevilleBank.contains(PlayerTile) && Bank.isOpen())
            return BotState.BANKING;

        if (EdgevilleBank.contains(PlayerTile) && Inventory.isFull() && !Bank.isOpen())
            return BotState.LOCATING_BANK;

        if (!EdgevilleBank.contains(PlayerTile) && Inventory.isFull())
            return BotState.WALKING_TO_BANK;


        return BotState.WAITING;
    }

    private enum BotState {
        WAITING,
        CHANGING_WORLD,

        WALKING_TO_BANK,
        LOCATING_BANK,
        BANKING,

        WALKING_TO_FISHING,
        LOCATING_FISH
    }

    // ---- Utility ---- //
    private boolean LootingWorld() {
        return Worlds.getCurrentWorld() == 301 || Worlds.getCurrentWorld() == 308;
    }

    private void ReturnFishValue() {
        if (TotalBankedFish > 0) {
            int RawTroutPrice = LivePrices.get("Raw trout") * TotalRawTrout;
            int CookedTroutPrice = LivePrices.get("Trout") * TotalCookedTrout;
            int RawSalmonPrice = LivePrices.get("Raw salmon") * TotalRawSalmon;
            int CookedSalmonPrice = LivePrices.get("Salmon") * TotalCookedSalmon;
            TotalFishValue = RawTroutPrice + CookedTroutPrice + RawSalmonPrice + CookedSalmonPrice;
        }
    }

    private int GetGoldPerHour() {
        if (TotalFishCollected > 0) {
            int RawTroutValue = LivePrices.get("Raw trout") * Timer.getHourlyRate(TotalCollectedRawTrout);
            int CookedTroutValue = LivePrices.get("Trout") * Timer.getHourlyRate(TotalCollectedTrout);
            int RawSalmonValue = LivePrices.get("Raw salmon") * Timer.getHourlyRate(TotalCollectedRawSalmon);
            int CookedSalmonValue = LivePrices.get("Salmon") * Timer.getHourlyRate(TotalCollectedSalmon);
            return RawTroutValue + CookedTroutValue + RawSalmonValue + CookedSalmonValue;
        }

        return 0;
    }

    private void ShouldToggleRun() {
        if (!Walking.isRunEnabled()) {
            if (Walking.getRunEnergy() >= ReRunValue) {
                if (Walking.toggleRun()) {
                    Sleep.sleepUntil(() -> Walking.isRunEnabled(), Util.GenerateRandomBound(2000, 5000));
                }
            }
        }
    }
}
