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
import net.botwithus.rs3.script.ScriptController;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.script.events.PropertyUpdateRequestEvent;
import net.botwithus.rs3.events.EventBus;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.Coordinate;

import java.util.*;

public class WorldHopper extends LoopingScript {

    private Random random = new Random();
    private List<String> logMessages = new ArrayList<>();
    private ScriptGraphicsContext graphicsContext;
    private boolean checkPlayerInRange = true;
    private boolean hopOnPlayerMod = true;
    private int playerCheckRange = 8;
    private boolean inCombat = false;
    private boolean hopF2PWorlds = false;
    private boolean hopP2PWorlds = true;
    private boolean preventHoppingInWarsRetreat = true;

    private static final List<Integer> MEMBERS_WORLDS = Arrays.asList(
    	    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 16, 17, 21, 22, 23, 25, 26, 27, 28, 31, 35, 36, 43, 44, 45, 46, 
    	    50, 55, 56, 59, 60,  63, 67, 68, 70, 71, 72, 74, 75, 76, 77, 78, 79, 100, 101, 102, 103, 104, 105, 106, 107, 
    	    108, 110, 111, 112, 113, 116, 117, 118, 119, 121, 122, 123, 124, 125, 126, 128, 130, 131,
    	    133, 134, 138, 139, 140, 142, 143, 144, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 
    	    156, 157, 158, 159, 160
    	);
    private static final List<Integer> FREE_WORLDS = Arrays.asList(135, 136, 57, 61, 11, 29, 8, 80, 81, 136, 7, 120, 20, 41, 141, 3, 17, 19, 34, 43, 108, 141, 210, 215, 225, 236, 239, 245, 249, 250,255, 256);

    private List<Integer> specialRegions = Arrays.asList(12078, 9011, 12598, 13111, 13214);

    private List<WorldHopTask> worldHopTasks = new ArrayList<>();

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
    
    private void updateCombatState() {
        LocalPlayer player = Client.getLocalPlayer();
        if (player != null) {
            this.inCombat = player.inCombat();
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
        addLogMessage("Current region ID: " + currentRegionId);

        if (!isInSpecialArea(currentRegionId) && !inCombat && (worldHopTasks.isEmpty() || inCombat)) {
            if (checkPlayerInRange && isPlayerWithinRange(playerCheckRange)) {
                hopToNextWorld();
            }

            if (hopOnPlayerMod && isPlayerModNearby()) {
                hopToNextWorld();
            }
        } else if (isInSpecialArea(currentRegionId)) {
            addLogMessage("Player is in a special region. Skipping world hop.");
        }
    }

    private void checkAndExecuteTasks() {
        if (!worldHopTasks.isEmpty() && !inCombat) {
            WorldHopTask currentTask = worldHopTasks.get(0);
            int currentRegionId = Client.getLocalPlayer().getCoordinate().getRegionId();

            if (isInSpecialArea(currentRegionId)) {
                addLogMessage("Player is in a special region. Delaying world hop.");
                return;
            }

            if (currentTask.getStartTime() == -1) {
                currentTask.start();
            }

            if (currentTask.isTimeToHop()) {
                addLogMessage("Task delay reached. Preparing to hop world to " + currentTask.getTargetWorld());
                prepareForWorldHop(currentTask);
            } else {
                long remainingTime = (currentTask.getStartTime() + currentTask.getDelayMinutes() * 60 * 1000 - System.currentTimeMillis()) / 1000;
                addLogMessage("Time remaining for next hop: " + remainingTime + " seconds.");
            }
        } else if (inCombat) {
            addLogMessage("Player is in combat. Delaying world hop.");
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

    public void addWorldHopTask(int delayMinutes, int targetWorld, String scriptName) {
        if (targetWorld < 1 || targetWorld > 259) {
            addLogMessage("Invalid world specified: " + targetWorld);
            return;
        }
        WorldHopTask newTask = new WorldHopTask(delayMinutes, targetWorld, scriptName);
        worldHopTasks.add(newTask);
        addLogMessage("Added new world hop task with a delay of " + delayMinutes + " minutes to world " + targetWorld + " running script " + (scriptName != null ? scriptName : "none"));
    }


    public void removeWorldHopTask(WorldHopTask task) {
        if (worldHopTasks.remove(task)) {
            addLogMessage("Removed world hop task to world " + task.getTargetWorld() + " running script " + (task.getScriptName() != null ? task.getScriptName() : "none"));
            if (!worldHopTasks.isEmpty()) {
                worldHopTasks.get(0).resetStartTime();
            }
        } else {
            addLogMessage("Failed to remove world hop task");
        }
    }

    private void hopToNextWorld() {
    	
        int currentRegionId = Client.getLocalPlayer().getCoordinate().getRegionId();
        
        if (!worldHopTasks.isEmpty()) {
            prepareForWorldHop(worldHopTasks.get(0));
        } else {
            if (isInSpecialArea(currentRegionId)) {
                addLogMessage("Player is in Excluded area. Skipping world hop.");
                return;
            }

            int randomWorld = -1;

            if (hopF2PWorlds && hopP2PWorlds) {
                List<Integer> combinedWorlds = new ArrayList<>();
                combinedWorlds.addAll(FREE_WORLDS);
                combinedWorlds.addAll(MEMBERS_WORLDS);
                randomWorld = combinedWorlds.get(random.nextInt(combinedWorlds.size()));
            } else if (hopF2PWorlds) {
                randomWorld = FREE_WORLDS.get(random.nextInt(FREE_WORLDS.size()));
            } else if (hopP2PWorlds) {
                randomWorld = MEMBERS_WORLDS.get(random.nextInt(MEMBERS_WORLDS.size()));
            }

            if (randomWorld != -1) {
                hopWorld(randomWorld);
            } else {
                addLogMessage("No worlds available to hop based on the current selection.");
            }
        }
    }



    private void prepareForWorldHop(WorldHopTask task) {
        if (inCombat) {
            addLogMessage("Player is in combat. Delaying world hop.");
            return;
        }

        ImmutableScript activeScript = ScriptController.getActiveScript();
        if (activeScript != null && (task.getScriptName() == null || !activeScript.getName().equals(task.getScriptName()))) {
            setActiveScript(activeScript.getName(), false);

            Execution.delayUntil(5000, () -> ScriptController.getActiveScript() == null);

            if (ScriptController.getActiveScript() == null) {
                addLogMessage("Active script stopped successfully.");
                executeWorldHop(task);
            } else {
                addLogMessage("Failed to stop active script. World hop aborted.");
            }
        } else {
            executeWorldHop(task);
        }
    }


    private void executeWorldHop(WorldHopTask task) {
        addLogMessage("Hopping world to " + task.getTargetWorld());

        hopWorld(task.getTargetWorld());

        if (task.getScriptName() != null) {
            ImmutableScript currentActiveScript = ScriptController.getActiveScript();
            if (currentActiveScript != null && currentActiveScript.getName().equals(task.getScriptName()) && currentActiveScript.isActive()) {
                addLogMessage("Script " + task.getScriptName() + " is already active.");
            } else {
                setActiveScript(task.getScriptName(), true);
                boolean activated = ScriptController.getActiveScript() != null 
                                    && ScriptController.getActiveScript().getName().equals(task.getScriptName()) 
                                    && ScriptController.getActiveScript().isActive();
                
                if (activated) {
                    addLogMessage("Script " + task.getScriptName() + " activated successfully.");
                } else {
                    addLogMessage("Failed to activate script: " + task.getScriptName() + " after hopping.");
                    return;
                }
            }
        }

        worldHopTasks.remove(task);

        if (!worldHopTasks.isEmpty()) {
            worldHopTasks.get(0).start();
        }
    }


    private boolean setActiveScript(String name, boolean active) {
        ImmutableScript script = ScriptController.getScripts().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
        if (script == null) {
            addLogMessage("No script found with name: " + name);
            return false;
        }
        EventBus.EVENT_BUS.publish(script, new PropertyUpdateRequestEvent(script, "isActive", Boolean.toString(active)));
        addLogMessage("Attempting to set script " + name + " to " + (active ? "active" : "inactive"));
        return script.isActive() == active;
    }

    public boolean isHopOnPlayerMod() {
        return hopOnPlayerMod;
    }

    public void setHopOnPlayerMod(boolean hopOnPlayerMod) {
        this.hopOnPlayerMod = hopOnPlayerMod;
    }

    public List<WorldHopTask> getWorldHopTasks() {
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

    public String[] getScriptNames() {
        List<ImmutableScript> scripts = ScriptController.getScripts();
        return scripts.stream().map(ImmutableScript::getName).toArray(String[]::new);
    }
}
