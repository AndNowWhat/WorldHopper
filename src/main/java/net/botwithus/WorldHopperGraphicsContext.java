package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class WorldHopperGraphicsContext extends ScriptGraphicsContext {

    private WorldHopper script;
    private int newTaskDelayMinutes = 0;
    private int targetWorld = 1;

    public WorldHopperGraphicsContext(ScriptConsole scriptConsole, WorldHopper script) {
        super(scriptConsole);
        this.script = script;
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("World Hopper Settings and Log", ImGuiWindowFlag.None.getValue())) {
            ImGui.Text("Settings");
            ImGui.Separator();
            boolean checkPlayerInRange = script.isCheckPlayerInRange();
            checkPlayerInRange = ImGui.Checkbox("Hop when Player in Range", checkPlayerInRange);
            script.setCheckPlayerInRange(checkPlayerInRange);

            int playerCheckRange = script.getPlayerCheckRange();
            playerCheckRange = ImGui.InputInt("Player Check Range (tiles)", playerCheckRange);
            script.setPlayerCheckRange(playerCheckRange);

            ImGui.Separator();
            
            boolean hopOnPlayerMod = script.isHopOnPlayerMod();
            hopOnPlayerMod = ImGui.Checkbox("Hop on Player Mod (40 tiles check)", hopOnPlayerMod);
            script.setHopOnPlayerMod(hopOnPlayerMod);
            
            ImGui.Separator();

            ImGui.Text("Add World Hop Task");
            newTaskDelayMinutes = ImGui.InputInt("Task Delay (minutes)", newTaskDelayMinutes);
            targetWorld = ImGui.InputInt("Target World", targetWorld);
            if (ImGui.Button("Add Task")) {
                script.addWorldHopTask(newTaskDelayMinutes, targetWorld);
            }
            
            ImGui.Separator();

            ImGui.Text("Upcoming World Hop Tasks:");
            for (WorldHopTask task : script.getWorldHopTasks()) {
                ImGui.Text("World hop in " + task.getRemainingTime() + " to world " + task.getTargetWorld());
            }

            ImGui.Separator();

           // ImGui.Text("Log");
           // for (String log : script.getLogMessages()) {
           //     ImGui.Text(log);
           // }
            
            script.manageLogSize();

            ImGui.End();
        }

      
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}