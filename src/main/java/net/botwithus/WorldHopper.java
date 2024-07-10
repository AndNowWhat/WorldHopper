package net.botwithus;

import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.queries.builders.characters.PlayerQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.script.ImmutableScript;
import net.botwithus.rs3.script.Script;
import net.botwithus.rs3.script.ScriptController;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.Coordinate;

import java.util.*;

public class WorldHopper extends LoopingScript {


    private Random random = new Random();
    private List<String> logMessages = new ArrayList<>();
    private ScriptGraphicsContext graphicsContext;
    private boolean checkPlayerInRange = false; 
    private boolean hopOnPlayerMod = false;
    private int playerCheckRange = 8;

    private Queue<WorldHopTask> worldHopTasks = new LinkedList<>();

    public WorldHopper(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.isBackgroundScript = true;
        ScriptConsole console = getConsole();
        graphicsContext = new WorldHopperGraphicsContext(console, this);
    }

    public List<String> getLogMessages() {
        return logMessages;
    }

    public void addLogMessage(String message) {
        logMessages.add(message);
        manageLogSize();
    }
    
    public void clearLogMessages() {
        logMessages.clear();
    }
    
    public void manageLogSize() {
        if (logMessages.size() > 15) {
            clearLogMessages();
        }
    }


    @Override
    public void onLoop() {
        checkAndExecuteTasks();

        if (checkPlayerInRange && isPlayerWithinRange(playerCheckRange)) {
            pauseMainScript();
            hopToNextWorld();
            resumeMainScript();
        }

        if (hopOnPlayerMod && isPlayerModNearby()) {
            pauseMainScript();
            hopToNextWorld();
            resumeMainScript();
        }
    }
    private void checkAndExecuteTasks() {
        if (!worldHopTasks.isEmpty()) {
            WorldHopTask currentTask = worldHopTasks.peek();
            if (currentTask.isTimeToHop()) {
                addLogMessage("Task delay reached. Hopping world to " + currentTask.getTargetWorld());
                hopWorld(currentTask.getTargetWorld());
                worldHopTasks.poll();

                if (!worldHopTasks.isEmpty()) {
                    worldHopTasks.peek().start();
                }
            }
        }
    }
    
    private boolean isPlayerModNearby() {
        Coordinate playerCoordinate = Client.getLocalPlayer().getCoordinate();
        EntityResultSet<Player> nearbyPlayers = PlayerQuery.newQuery().results();

        for (Player player : nearbyPlayers) {
            if (!player.getName().equals(Client.getLocalPlayer().getName()) && player.getCoordinate().distanceTo(playerCoordinate) <= 40) {
                if (player.getName().startsWith("Mod")) {
                    addLogMessage("Detected Mod: " + player.getName() + ". Hopping world.");
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

    private void pauseMainScript() {
        ImmutableScript mainScript = ScriptController.getActiveScript();
        if (mainScript instanceof Script && !mainScript.equals(this) && !((Script) mainScript).isPaused()) {
            ((Script) mainScript).pause();
        }
    }

    private void resumeMainScript() {
        ImmutableScript mainScript = ScriptController.getActiveScript();
        if (mainScript instanceof Script && !mainScript.equals(this) && ((Script) mainScript).isPaused()) {
            ((Script) mainScript).resume();
        }
    }

    public void hopWorld(int targetWorld) {
        long delaySmall = 600 + random.nextInt(1200);

        addLogMessage("Bot state: HOPPING_WORLD");
        addLogMessage("Opening world selection interface.");
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, 7, 93782016);
        Execution.delay(delaySmall);
        addLogMessage("Navigating to the world list.");
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93913153);
        Execution.delay(delaySmall);

        int interfaceId = 1587;
        addLogMessage("Logging available worlds from interface ID " + interfaceId);

        if (Execution.delayUntil(5000, () -> Interfaces.isOpen(interfaceId))) {
            addLogMessage("Attempting to interact with world: " + targetWorld);
            MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, targetWorld, 104005640);
            addLogMessage("World hop executed.");

            long delay = 10000 + random.nextInt(5000);
            Execution.delay(delay);
        }
    }

    public void addWorldHopTask(int delayMinutes, int targetWorld) {
        if (targetWorld < 1 || targetWorld > 259) {
            addLogMessage("Invalid world specified: " + targetWorld);
            return;
        }
        WorldHopTask newTask = new WorldHopTask(delayMinutes, targetWorld);
        worldHopTasks.add(newTask);
        addLogMessage("Added new world hop task with a delay of " + delayMinutes + " minutes to world " + targetWorld);

        if (worldHopTasks.size() == 1) {
            worldHopTasks.peek().start();
        }
    }
    
    private void hopToNextWorld() {
        if (!worldHopTasks.isEmpty()) {
            hopWorld(worldHopTasks.peek().getTargetWorld());
        } else {
            int randomWorld = 1 + random.nextInt(259);
            hopWorld(randomWorld);
        }
    }
    
    public boolean isHopOnPlayerMod() {
        return hopOnPlayerMod;
    }

    public void setHopOnPlayerMod(boolean hopOnPlayerMod) {
        this.hopOnPlayerMod = hopOnPlayerMod;
    }
    
    public Queue<WorldHopTask> getWorldHopTasks() {
        return worldHopTasks;
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

    @Override
    public ScriptGraphicsContext getGraphicsContext() {
        return graphicsContext;
    }
}
