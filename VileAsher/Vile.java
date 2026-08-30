import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.randoms.RandomEvent;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;

import java.util.List;

@ScriptManifest(
        name = "Vile Ashes",
        description = "Uses Vile Ashes",
        author = "ImMac",
        version = 1.0,
        category = Category.PRAYER,
        image = "")


public class Vile extends AbstractScript {

    private final Utility Util = new Utility();
    private boolean Complete = false;
    private int ReWalkValue = 0;

    // - Areas
    Area VarrockWestBank = new Area(3180, 3447, 3185, 3433);

    @Override
    public void onStart() {
        if (ReWalkValue == 0) ReWalkValue = Util.GenerateRandomBound(8, 16);

        // Smart Mouse
        Mouse.setMouseAlgorithm(new SmartMouseMultiDir());
    }

    @Override
    public int onLoop() {

        if (Complete) {
            if (Bank.isOpen()) {
                Logger.log("Finishing Up - Closing Bank");
                getRandomManager().disableSolver(RandomEvent.LOGIN);

                if (Bank.close()) {
                    Sleep.sleepUntil(() -> !Bank.isOpen(), Util.GenerateRandomBound(2500, 5000));
                    return Util.GenerateRandomBound(400, 600);
                }
            }

            if (Client.isLoggedIn()) {
                Logger.log("Finishing Up - Logging Out");

                if (Client.logout()) {
                    Sleep.sleepUntil(() -> !Client.isLoggedIn(), Util.GenerateRandomBound(2500, 5000));
                    getRandomManager().disableSolver(RandomEvent.LOGIN);
                    return Util.GenerateRandomBound(382, 645);
                }
            }

            if (!Client.isLoggedIn()) {
                Logger.log("Finishing Up - Stopping script");
                getScriptManager().stop();
            }
        }

        switch (States()) {
            case USING_ASHES:
                Logger.log("Using Ashes");

                List<Item> InventoryAshes = Inventory.all();

                for (Item Ashes : InventoryAshes) {

                    if (Ashes != null) {
                        for (int i = 0; i < Util.GenerateRandomBound(1, 4); i++) {

                            // 7-15%~ Chance for a cheeky extra click
                            if (Util.GenerateRandomBound(1, 100) < Util.GenerateRandomBound(7, 15)) {
                                Ashes.interact("Scatter");
                                Sleep.sleep(20, 50);
                            }


                            if (Ashes.interact("Scatter")) {
                                Sleep.sleep(Util.GenerateRandomBound(50, 100), Util.GenerateRandomBound(125, 200));
                            }
                        }

                        Sleep.sleep(Util.GenerateRandomBound(125, 200), Util.GenerateRandomBound(250, 350));
                        break;
                    }
                }

                Logger.log("Ashes Left: " + Inventory.count("Vile ashes"));
                return Util.GenerateRandomBound(200, 500);

            case CLOSING_BANK:
                Logger.log("Closing Bank");

                if (Bank.close()) {
                    Sleep.sleepUntil(() -> !Bank.isOpen(), Util.GenerateRandomBound(2500, 5000));
                    break;
                }

                break;
            case BANKING:
                Logger.log("Banking");

                if (!Bank.contains("Vile ashes") && !Inventory.contains("Vile ashes")) {
                    Complete = true;
                    break;
                }

                if (Inventory.isEmpty()) {
                    if (Bank.withdrawAll("Vile ashes")) {
                        Sleep.sleepUntil(() -> Inventory.contains("Vile ashes"), Util.GenerateRandomBound(3000, 5000));
                        break;
                    }
                }

                if (!Inventory.isEmpty() && !Inventory.contains("Vile ashes")) {
                    if (Bank.depositAllItems()) {
                        Sleep.sleepUntil(() -> Inventory.isEmpty(), Util.GenerateRandomBound(3000, 5000));
                    }
                    break;
                }

                break;
            case LOCATING_BANK:
                Logger.log("Locating Bank");

                GameObject Booth = GameObjects.closest("Bank Booth");
                if (Booth != null && Booth.canReach()) {
                    if (Booth.interact("Bank")) {
                        Sleep.sleepUntil(() -> Bank.isOpen(), Util.GenerateRandomBound(3000, 5000));
                        break;
                    }
                }

                break;
            case WALKING_TO_BANK:
                Logger.log("Walking to Bank");

                if (!VarrockWestBank.contains(Walking.getDestination())) {
                    if (Walking.shouldWalk(ReWalkValue)) {
                        if (Walking.walk(VarrockWestBank.getRandomTile())) {
                            ReWalkValue = Util.GenerateRandomBound(4, 15);
                            Sleep.sleep(200, Util.GenerateRandomBound(350, 700));
                            break;
                        }
                    }
                }

                break;
        }


        return Util.GenerateRandomBound(Util.GenerateRandomBound(200, 300), Util.GenerateRandomBound(400, 650));
    }

    private BotState States() {
        Tile PlayerTile = Players.getLocal().getTile();

        if (!Client.isLoggedIn())
            return BotState.WAITING;
        
        if (Inventory.contains("Vile ashes") && VarrockWestBank.contains(PlayerTile) && !Bank.isOpen())
            return BotState.USING_ASHES;

        if (Bank.isOpen() && Inventory.contains("Vile ashes"))
            return BotState.CLOSING_BANK;

        if (Bank.isOpen())
            return BotState.BANKING;

        if (!Inventory.contains("Vile ashes") && VarrockWestBank.contains(PlayerTile))
            return BotState.LOCATING_BANK;

        if (!VarrockWestBank.contains(PlayerTile))
            return BotState.WALKING_TO_BANK;

        return BotState.WAITING;
    }

    private enum BotState {
        WAITING,

        WALKING_TO_BANK,
        LOCATING_BANK,
        BANKING,
        CLOSING_BANK,

        USING_ASHES
    }
}
