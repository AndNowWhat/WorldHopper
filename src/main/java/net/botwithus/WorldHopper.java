package net.botwithus;

import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.login.World;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.PlayerQuery;
import net.botwithus.rs3.game.queries.builders.worlds.WorldQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.Coordinate;

import java.util.*;

public class WorldHopper extends LoopingScript {

    private Random random = new Random();
    private List<String> logMessages = new ArrayList<>();
    private boolean checkPlayerInRange = true;
    private boolean hopOnPlayerMod = true;
    private int playerCheckRange = 8;
    private boolean inCombat = false;
    private boolean hopF2PWorlds = false;
    private boolean hopP2PWorlds = true;
    public int minAllowedPlayers = 0;
    public int maxAllowedPlayers = 300;
    public int minPing = 0;
    public int maxPing = 150;

    private static final List<Integer> MEMBERS_WORLDS = Arrays.asList(
            1, 4, 5, 6, 9, 10, 12, 14, 16, 21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 35, 36, 37, 39, 40, 42, 44, 45,
            46, 49, 50, 51, 53, 54, 56, 58, 59, 60, 62, 63, 64, 65, 67, 68, 69, 70, 71, 72, 73, 74, 76, 77, 78, 79, 82, 83,
            85, 87, 88, 89, 91, 92, 96, 98, 99, 100, 103, 104, 105, 106, 116, 117, 119, 123, 124, 138, 139, 140, 252, 257, 258, 259
    );

    private static final List<Integer> FREE_WORLDS = Arrays.asList(
            135, 136, 57, 61, 11, 29, 8, 80, 81, 136, 7, 120, 20, 41, 141, 3, 17, 19, 34, 43, 108, 141, 210, 215, 225, 236, 239, 245, 249, 250, 255, 256
    );

    private List<Integer> specialRegions = Arrays.asList(12078, 9011, 12598, 13111, 13214);
    private List<WorldHopTask> worldHopTasks = new ArrayList<>();
    private ScriptGraphicsContext graphicsContext;

    public WorldHopper(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.isBackgroundScript = true;
        ScriptConsole console = getConsole();
        graphicsContext = new WorldHopperGraphicsContext(console, this);
    }

    public List<String> getLogMessages() {
        return logMessages;
    }


    public List<WorldHopTask> getWorldHopTasks() {
        return worldHopTasks;
    }

    private void updateCombatState() {
        if (Client.getLocalPlayer() != null) {
            this.inCombat = Client.getLocalPlayer().inCombat();
        }
    }

    private boolean isInSpecialArea(int currentRegionId) {
        return specialRegions.contains(currentRegionId);
    }

    @Override
    public void onLoop() {
        this.loopDelay = 600;
        updateCombatState();
        checkAndExecuteTasks();

        int currentRegionId = Client.getLocalPlayer().getCoordinate().getRegionId();

        if (!isInSpecialArea(currentRegionId) && !inCombat) {
            if (checkPlayerInRange && isPlayerWithinRange(playerCheckRange)) {
                println("Player in range, hopping");
                hopToNextWorld();
            }

            if (hopOnPlayerMod && isPlayerModNearby()) {
                println("Player mod near, hopping");
                hopToNextWorld();
            }
        } else if (isInSpecialArea(currentRegionId)) {
            println("Player is in a special region. Skipping world hop.");
        }
    }

    private void checkAndExecuteTasks() {
        if (!worldHopTasks.isEmpty() && !inCombat) {
            WorldHopTask currentTask = worldHopTasks.get(0);
            int currentRegionId = Client.getLocalPlayer().getCoordinate().getRegionId();

            if (isInSpecialArea(currentRegionId)) {
                println("Player is in a special region. Delaying world hop.");
                return;
            }

            if (currentTask.getStartTime() == -1) {
                currentTask.start();
            }

            if (currentTask.isTimeToHop()) {
                println("Task delay reached. Preparing to hop world to " + currentTask.getTargetWorld());
                executeWorldHop(currentTask);
            } else {
                long remainingTime = (currentTask.getStartTime() + currentTask.getDelayMinutes() * 60 * 1000 - System.currentTimeMillis()) / 1000;
                println("Time remaining for next hop: " + remainingTime + " seconds.");
            }
        } else if (inCombat) {
            println("Player is in combat. Delaying world hop.");
        }
    }

    private void executeWorldHop(WorldHopTask task) {
        println("Hopping world to " + task.getTargetWorld());
        hopWorld(task.getTargetWorld());
        worldHopTasks.remove(task);
        if (!worldHopTasks.isEmpty()) {
            worldHopTasks.get(0).start();
        }
    }

    private boolean isPlayerModNearby() {
        Coordinate playerCoordinate = Client.getLocalPlayer().getCoordinate();
        EntityResultSet<Player> nearbyPlayers = PlayerQuery.newQuery().results();

        for (Player player : nearbyPlayers) {
            if (!player.getName().equals(Client.getLocalPlayer().getName()) && player.getCoordinate().distanceTo(playerCoordinate) <= 40) {
                if (player.getName().startsWith("Mod")) {
                    println("Detected Mod: " + player.getName() + ". Hopping world.");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPlayerWithinRange(int range) {
        Coordinate playerCoordinate = Client.getLocalPlayer().getCoordinate();
        EntityResultSet<Player> nearbyPlayers = PlayerQuery.newQuery().results();

        for (Player player : nearbyPlayers) {
            if (!player.getName().equals(Client.getLocalPlayer().getName()) && player.getCoordinate().distanceTo(playerCoordinate) <= range) {
                return true;
            }
        }
        return false;
    }


    public void hopWorld(int targetWorld) {
        long delaySmall = 600 + random.nextInt(1200);
        LocalPlayer player = Client.getLocalPlayer();

        println("Opening world selection interface.");
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 7, 93782016);
        Execution.delay(delaySmall);
        println("Navigating to the world list.");
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93913153);
        Execution.delay(delaySmall);

        int interfaceId = 1587;
        println("Logging available worlds from interface ID " + interfaceId);

        if (Execution.delayUntil(5000, () -> Interfaces.isOpen(interfaceId))) {
            println("Attempting to interact with world: " + targetWorld);
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, targetWorld, 104005640);
            println("World hop executed.");
            Execution.delayUntil(150000, () -> LoginManager.getLoginStatus() == 1);
        }
    }



    private void hopToNextWorld() {
        int currentRegionId = Client.getLocalPlayer().getCoordinate().getRegionId();

        if (isInSpecialArea(currentRegionId)) {
            println("Player is in Excluded area. Skipping world hop.");
            return;
        }

        List<World> availableWorlds = new ArrayList<>();

        if (Client.isMember()) {
            if (hopP2PWorlds) {
                availableWorlds = WorldQuery.newQuery()
                        .members()
                        .population(minAllowedPlayers, maxAllowedPlayers)
                        .ping(minPing, maxPing)
                        .results().stream().toList();
            }
        } else {
            if (hopF2PWorlds) {
                availableWorlds = WorldQuery.newQuery()
                        .population(minAllowedPlayers, maxAllowedPlayers)
                        .ping(minPing, maxPing)
                        .results().stream().toList();
            }
        }

        if (!availableWorlds.isEmpty()) {
            println("Filtered worlds meeting criteria:");
            for (World world : availableWorlds) {
                println("World ID: " + world.getId() + ", Population: " + world.getPopulation() + ", Ping: " + world.getPing());
            }

            World randomWorld = availableWorlds.get(random.nextInt(availableWorlds.size()));

            println("Selected world to hop: " + randomWorld.getId() + " with " + randomWorld.getPopulation() + " players and ping: " + randomWorld.getPing());

            hopWorld(randomWorld.getId());
        } else {
            println("No worlds available that meet the player count, ping, and membership criteria.");
        }
    }

    public void addWorldHopTask(int delayMinutes, int targetWorld) {
        if (targetWorld < 1 || targetWorld > 259) {
            println("Invalid world specified: " + targetWorld);
            return;
        }
        WorldHopTask newTask = new WorldHopTask(delayMinutes, targetWorld);
        worldHopTasks.add(newTask);
        println("Added new world hop task with a delay of " + delayMinutes + " minutes to world " + targetWorld);
    }

    public void removeWorldHopTask(WorldHopTask task) {
        if (worldHopTasks.remove(task)) {
            println("Removed world hop task to world " + task.getTargetWorld());
            if (!worldHopTasks.isEmpty()) {
                worldHopTasks.get(0).resetStartTime();
            }
        } else {
            println("Failed to remove world hop task");
        }
    }

    public boolean isHopOnPlayerMod() {
        return hopOnPlayerMod;
    }

    public void setHopOnPlayerMod(boolean hopOnPlayerMod) {
        this.hopOnPlayerMod = hopOnPlayerMod;
    }

    public boolean isCheckPlayerInRange() {
        return checkPlayerInRange;
    }

    public void setCheckPlayerInRange(boolean checkPlayerInRange) {
        this.checkPlayerInRange = checkPlayerInRange;
    }

    public int getPlayerCheckRange() {
        return playerCheckRange;
    }

    public void setPlayerCheckRange(int playerCheckRange) {
        this.playerCheckRange = playerCheckRange;
    }

    public void setHopF2PWorlds(boolean hopF2PWorlds) {
        this.hopF2PWorlds = hopF2PWorlds;
    }

    public void setHopP2PWorlds(boolean hopP2PWorlds) {
        this.hopP2PWorlds = hopP2PWorlds;
    }

    @Override
    public ScriptGraphicsContext getGraphicsContext() {
        return graphicsContext;
    }
}
