import org.dreambot.api.Client;
import org.dreambot.api.ClientSettings;
import org.dreambot.api.data.ActionMode;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.grandexchange.LivePrices;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.ItemContainerListener;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.items.Item;

import java.awt.*;
import java.util.Collections;
import java.util.List;

@ScriptManifest(
        name = "Monk Robe Loota",
        description = "Loots Monk Robes at the Monk's Monastery.",
        author = "ImMac",
        version = 1.0,
        category = Category.MONEYMAKING,
        image = "https://i.ibb.co/Kp8qMpT7/monk-robes.png"
)

public class MonkRobeLoota extends AbstractScript implements ItemContainerListener {

    // Constants
    private final Utility Util = new Utility();
    private final Timer Timer = new Timer();
    private Tile currentPlayerTile;

    // Behavioural
    private int reWalkHigher = 12;
    private int reWalkLower = 3;
    private int reWalk = 0;
    private int toggleRun = 20;
    private boolean useSpace = false;
    private boolean useFKeys = false;

    // Areas
    private final Area MONASTERY = new Area(3059, 3492, 3056, 3486, 1);
    private final Area EDGEVILLE_BANK = new Area(3091, 3499, 3098, 3488);
    private final Area MONASTERY_LADDER = new Area(3056, 3484, 3059, 3482);

    // Items
    private final int MONK_ROBE_BOTTOM = 542;
    private final int MONK_ROBE_TOP = 544;

    // Paint
    private int monkBottoms = 0;
    private int monkTops = 0;

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.white);
        g.setFont(new Font("Calibri", Font.BOLD, 15));
        g.drawString("Mac's Monk Robe Looter", 10, 35);

        g.setFont(new Font("Calibri", Font.PLAIN, 15));
        g.drawString("Elapsed: " + Timer.formatTime(), 10, 47);

        g.drawString("Total Robe Tops: " + String.format("%,d", monkBottoms), 10, 59);
        g.drawString("Total Robe Bottoms: " + String.format("%,d", monkTops), 10, 71);
        g.drawString("Total GP Value: " + String.format("%,d", calculateProfit()), 10, 83);

        g.drawString("Bottoms/Hour " + String.format("%,d", Timer.getHourlyRate(monkTops)), 10, 95);
        g.drawString("Tops/Hour: " + String.format("%,d", Timer.getHourlyRate(monkBottoms)), 10, 107);
        g.drawString("GP/Hour: " + String.format("%,d", Timer.getHourlyRate(calculateProfit())), 10, 119);

        g.drawString("Next Run Value: " + toggleRun, 10, 131);
    }

    private int calculateProfit() {
        int robeBottomsValue = LivePrices.get(MONK_ROBE_BOTTOM) * monkBottoms;
        int robeTopsValue = LivePrices.get(MONK_ROBE_TOP) * monkTops;
        return robeBottomsValue + robeTopsValue;
    }

    @Override
    public void onInventoryItemAdded(Item item) {
        if (item == null) return;
        switch (item.getId()) {
            case MONK_ROBE_BOTTOM:
                monkBottoms++;
                break;
            case MONK_ROBE_TOP:
                monkTops++;
                break;
        }
    }

    // Core
    @Override
    public void onStart() {
        Timer.start();

        // Behavioural
        reWalkLower = Util.GenerateRandomBound(3, 5);
        reWalkHigher = Util.GenerateRandomBound(7, 12);
        reWalk = Util.GenerateRandomBound(reWalkLower, reWalkHigher);
        toggleRun = Util.GenerateRandomBound(15, 30);
        useSpace = Util.GenerateRandomBoolean();
        useFKeys = Util.GenerateRandomBoolean();
    }

    @Override
    public int onLoop() {
        if (Client.isLoggedIn()) {
            currentPlayerTile = Players.getLocal().getServerTile();

            switch (getState()) {
                case WALK_TO_MONASTERY:
                    walkToMonastery();
                    break;

                case EQUIPPING_MONK_ROBES:
                    equipRobes();
                    break;

                case LOOTING_MONK_ROBES:
                    lootRobes();
                    break;

                case WALKING_TO_BANK:
                    walkToBank();
                    break;

                case LOCATING_BANK:
                    locateBank();
                    break;

                case BANKING:
                    useBank();
                    return Util.GenerateRandomBound(250, 370);

                case HOPPING_WORLD:
                    worldHop();
                    break;

                case TALKING_TO_MONK:
                    talkToMonk();
                    break;

                case CHANGING_SETTINGS:
                    changeSettings();
                    break;
            }
        }

        return Util.GenerateRandomBound(300, 600);
    }

    // Talking to Monk
    private boolean shouldTalkToMonk() {
        return Dialogues.inDialogue() && MONASTERY_LADDER.contains(currentPlayerTile);
    }

    private void talkToMonk() {
        if (!Dialogues.isProcessing()) {
            Logger.log("Talking to Monk.");
            if (useSpace) {
                if (Dialogues.spaceToContinue()) {
                    Sleep.sleepUntil(() -> !Dialogues.isProcessing(), Util.GenerateRandomBound(350, 500));
                }
            } else {
                if (Dialogues.clickContinue()) {
                    Sleep.sleepUntil(() -> !Dialogues.isProcessing(), Util.GenerateRandomBound(350, 500));
                }
            }
        }
    }

    // Change Settings
    private boolean shouldChangeSettings() {
        boolean settingsCorrect = ClientSettings.getNPCAttackOptionsMode() == ActionMode.HIDDEN;

        if (ClientSettings.isOpen() && settingsCorrect) {
            if (ClientSettings.closeSettingsInterface()) {
                Sleep.sleepUntil(() -> ClientSettings.isOpen(), Util.GenerateRandomBound(1000, 3000));
            }

            return false;
        }

        return !settingsCorrect;
    }

    private void changeSettings() {
        Logger.log("Changing Settings");
        if (ClientSettings.getNPCAttackOptionsMode() != ActionMode.HIDDEN) {
            Logger.log("Changing NPC Attack Mode");
            if (ClientSettings.setNPCAttackOptionsMode(ActionMode.HIDDEN)) {
                Sleep.sleepUntil(() -> ClientSettings.getNPCAttackOptionsMode() == ActionMode.HIDDEN, Util.GenerateRandomBound(2000, 3000));
            }
        }
    }

    // World Hopping
    private boolean shouldWorldHop() {
        List<GroundItem> robes = GroundItems.all(item -> item.getId() == MONK_ROBE_BOTTOM || item.getId() == MONK_ROBE_TOP);
        return MONASTERY.contains(currentPlayerTile) && robes.isEmpty() && !Inventory.isFull();
    }

    private void worldHop() {
        Logger.log("Hopping world!");
        WorldHopper.hopWorld(Worlds.getRandomWorld(w ->
                w.isF2P()
                        && !w.isPVP()
                        && w.getMinimumLevel() == 0
                        && w.getPopulation() >= 30
                        && w.getPopulation() <= 1200
                        && w.isNormal()
        ));
    }

    // Walk to Bank
    private boolean shouldWalkToBank() {
        return Inventory.isFull() && !EDGEVILLE_BANK.contains(currentPlayerTile);
    }

    private void walkToBank() {
        if (!Tabs.isOpen(Tab.INVENTORY)) {
            if (useFKeys) {
                if (Tabs.openWithFKey(Tab.INVENTORY)) {
                    Sleep.sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), Util.GenerateRandomBound(300, 500));
                }
            } else {
                if (Tabs.openWithMouse(Tab.INVENTORY)) {
                    Sleep.sleepUntil(() -> Tabs.isOpen(Tab.INVENTORY), Util.GenerateRandomBound(300, 500));
                }
            }

            return;
        }

        if (shouldToggleRun()) {
            toggleRun();
            return;
        }

        if (!EDGEVILLE_BANK.contains(Walking.getDestination())) {
            if (Walking.shouldWalk(reWalk)) {
                if (Walking.walk(EDGEVILLE_BANK.getRandomTile())) {
                    Logger.log("Walking to Edgeville Bank.");
                    reWalk = Util.GenerateRandomBound(reWalkLower, reWalkHigher);
                    Sleep.sleep(350, 700);
                }
            }
        }
    }

    // Banking
    private boolean shouldBank() {
        return Inventory.isFull() && Bank.isOpen();
    }

    private void useBank() {
        if (Equipment.getItemInSlot(EquipmentSlot.CHEST) != null) {
            if (Bank.depositAllEquipment()) {
                Logger.log("Depositing Equipment.");
                Sleep.sleepUntil(() -> Equipment.getItemInSlot(EquipmentSlot.CHEST) == null, Util.GenerateRandomBound(350, 500));
                return;
            }
        }

        if (Bank.depositAllItems()) {
            Logger.log("Depositing Inventory.");
            Sleep.sleepUntil(() -> Inventory.isEmpty(), Util.GenerateRandomBound(500, 1000));
        }
    }

    // Locate Bank
    private boolean shouldLocateBank() {
        return EDGEVILLE_BANK.contains(currentPlayerTile) && !Bank.isOpen() && Inventory.isFull();
    }

    private void locateBank() {
        GameObject booth = GameObjects.closest(obj -> obj.getName().equals("Bank booth") && obj.canReach());
        if (booth == null) return;

        if (booth.interact("Bank")) {
            Logger.log("Locating Bank.");
            Sleep.sleepUntil(() -> Bank.isOpen(), () -> Players.getLocal().isMoving(), Util.GenerateRandomBound(350, 500), 100);
        }
    }

    // Looting Robes
    private boolean shouldLootRobes() {
        List<GroundItem> robes = GroundItems.all(item -> item.getId() == MONK_ROBE_BOTTOM || item.getId() == MONK_ROBE_TOP);
        return MONASTERY.contains(currentPlayerTile) && !robes.isEmpty();
    }

    private void lootRobes() {
        List<GroundItem> robes = GroundItems.all(
                item ->
                        item.getId() == MONK_ROBE_BOTTOM || item.getId() == MONK_ROBE_TOP &&
                                item.getTile().equals(new Tile(3059, 3488, 1)) || item.getTile().equals(new Tile(3059, 3487, 1))
        );

        Collections.shuffle(robes);

        for (GroundItem robe : robes) {
            if (robe.interact("Take")) {
                Sleep.sleepUntil(() -> !robe.exists(), Util.GenerateRandomBound(500, 1000));
            }
            break;
        }
    }

    // Equipping Robes
    private boolean shouldWearRobes() {
        boolean inventoryHasRobes = Inventory.contains(MONK_ROBE_BOTTOM) && Inventory.contains(MONK_ROBE_TOP);
        boolean missingAnyRobe = Equipment.getItemInSlot(EquipmentSlot.LEGS) == null || Equipment.getItemInSlot(EquipmentSlot.CHEST) == null;
        return inventoryHasRobes && missingAnyRobe;
    }

    private void equipRobes() {
        if (Equipment.getItemInSlot(EquipmentSlot.LEGS) == null) {
            if (Inventory.interact(MONK_ROBE_BOTTOM, "Wear")) {
                Logger.log("Equipping Robe Bottoms.");
                Sleep.sleepUntil(() -> Equipment.getItemInSlot(EquipmentSlot.LEGS) != null, Util.GenerateRandomBound(1000, 2000));
            }
            return;
        }

        if (Equipment.getItemInSlot(EquipmentSlot.CHEST) == null) {
            if (Inventory.interact(MONK_ROBE_TOP, "Wear")) {
                Logger.log("Equipping Robe Top.");
                Sleep.sleepUntil(() -> Equipment.getItemInSlot(EquipmentSlot.LEGS) != null, Util.GenerateRandomBound(1000, 2000));
            }
        }
    }

    // Walk to Monastery
    private boolean shouldWalkToMonastery() {
        return Inventory.isEmpty() && !MONASTERY.contains(currentPlayerTile) && !Dialogues.inDialogue();
    }

    private void walkToMonastery() {
        if (shouldToggleRun()) {
            toggleRun();
            return;
        }

        if (!MONASTERY.contains(Walking.getDestination())) {
            if (Walking.shouldWalk(reWalk)) {
                if (Walking.walk(MONASTERY.getRandomTile())) {
                    Logger.log("Walking to Monastery.");
                    reWalk = Util.GenerateRandomBound(reWalkLower, reWalkHigher);
                    Sleep.sleep(350, 700);
                }
            }
        }
    }

    // Toggle Run
    private boolean shouldToggleRun() {
        return Walking.getRunEnergy() >= toggleRun && !Walking.isRunEnabled();
    }

    private void toggleRun() {
        toggleRun = 20 + Util.GenerateRandomBound(-10, 20);
        if (Walking.toggleRun()) {
            Sleep.sleepUntil(() -> Walking.isRunEnabled(), Util.GenerateRandomBound(1000, 2000));
        }
    }

    // States
    private State getState() {
        if (shouldChangeSettings())
            return State.CHANGING_SETTINGS;

        if (shouldWorldHop())
            return State.HOPPING_WORLD;

        if (shouldWalkToBank())
            return State.WALKING_TO_BANK;

        if (shouldLocateBank())
            return State.LOCATING_BANK;

        if (shouldBank())
            return State.BANKING;

        if (shouldWalkToMonastery())
            return State.WALK_TO_MONASTERY;

        if (shouldTalkToMonk())
            return State.TALKING_TO_MONK;

        if (shouldWearRobes())
            return State.EQUIPPING_MONK_ROBES;

        if (shouldLootRobes())
            return State.LOOTING_MONK_ROBES;

        return State.WAITING;
    }

    private enum State {
        CHANGING_SETTINGS,
        HOPPING_WORLD,

        WALK_TO_MONASTERY,
        TALKING_TO_MONK,
        LOOTING_MONK_ROBES,
        EQUIPPING_MONK_ROBES,

        WALKING_TO_BANK,
        LOCATING_BANK,
        BANKING,

        WAITING
    }
}
