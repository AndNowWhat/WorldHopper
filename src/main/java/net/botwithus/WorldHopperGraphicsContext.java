package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.imgui.NativeInteger;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;
import net.botwithus.rs3.script.ScriptController;
import net.botwithus.rs3.script.ImmutableScript;
public class WorldHopperGraphicsContext extends ScriptGraphicsContext {

    private WorldHopper script;
    private int newTaskDelayMinutes = 0;
    private int targetWorld = 1;
    private NativeInteger selectedScriptIndex = new NativeInteger(-1);
    private boolean hopF2PWorlds = false; 
    private boolean hopP2PWorlds = true;

    public WorldHopperGraphicsContext(ScriptConsole scriptConsole, WorldHopper script) {
        super(scriptConsole);
        this.script = script;
    }

    @Override
    public void drawSettings() {
        ImGui.PushStyleVar(ImGuiStyleVar.WindowRounding, 10.0f);
        ImGui.PushStyleVar(ImGuiStyleVar.FrameRounding, 5.0f);
        ImGui.PushStyleVar(ImGuiStyleVar.FrameBorderSize, 1.0f);
        ImGui.PushStyleVar(ImGuiStyleVar.WindowBorderSize, 1.0f);
        ImGui.PushStyleVar(ImGuiStyleVar.WindowPadding, 10.0f, 10.0f);

        // Set colors for a professional dark theme
        ImGui.PushStyleColor(ImGuiCol.WindowBg, 0.1f, 0.1f, 0.1f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.FrameBg, 0.2f, 0.2f, 0.2f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.FrameBgHovered, 0.3f, 0.3f, 0.3f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.FrameBgActive, 0.4f, 0.4f, 0.4f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.TitleBg, 0.1f, 0.1f, 0.1f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.TitleBgActive, 0.1f, 0.1f, 0.1f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.TitleBgCollapsed, 0.1f, 0.1f, 0.1f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.Button, 0.2f, 0.2f, 0.2f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.3f, 0.3f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.ButtonActive, 0.4f, 0.4f, 0.4f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.CheckMark, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.Text, 0.9f, 0.9f, 0.9f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.TextDisabled, 0.5f, 0.5f, 0.5f, 1.0f);
        ImGui.PushStyleColor(ImGuiCol.Border, 0.3f, 0.3f, 0.3f, 1.0f);
        
        if (ImGui.Begin("World Hopper Task Scheduler", ImGuiWindowFlag.None.getValue())) {
            ImGui.Text("Settings");
            ImGui.Separator();

            boolean checkPlayerInRange = script.isCheckPlayerInRange();
            checkPlayerInRange = ImGui.Checkbox("Hop when Player in Range", checkPlayerInRange);
            script.setCheckPlayerInRange(checkPlayerInRange);

            int playerCheckRange = script.getPlayerCheckRange();
            playerCheckRange = ImGui.InputInt("Player Check Range (tiles)", playerCheckRange);
            script.setPlayerCheckRange(playerCheckRange);
            
            hopF2PWorlds = ImGui.Checkbox("Hop to F2P Worlds", hopF2PWorlds);
            hopP2PWorlds = ImGui.Checkbox("Hop to P2P Worlds", hopP2PWorlds);
            
            script.setHopF2PWorlds(hopF2PWorlds);
            script.setHopP2PWorlds(hopP2PWorlds);

            ImGui.Separator();
            
            boolean hopOnPlayerMod = script.isHopOnPlayerMod();
            hopOnPlayerMod = ImGui.Checkbox("Hop on Player Mod (40 tiles check)", hopOnPlayerMod);
            script.setHopOnPlayerMod(hopOnPlayerMod);
            
            ImGui.Separator();

            ImGui.Text("Add World Hop Task");
            newTaskDelayMinutes = ImGui.InputInt("Task Delay (minutes)", newTaskDelayMinutes);
            targetWorld = ImGui.InputInt("Target World", targetWorld);

            String[] scriptNames = script.getScriptNames();
            String[] comboItems = new String[scriptNames.length + 1];
            comboItems[0] = "No Script";
            System.arraycopy(scriptNames, 0, comboItems, 1, scriptNames.length);
            ImGui.Combo("Select Script", selectedScriptIndex, comboItems);

            if (ImGui.Button("Add Task")) {
                String selectedScriptName = selectedScriptIndex.get() == 0 ? null : scriptNames[selectedScriptIndex.get() - 1];
                script.addWorldHopTask(newTaskDelayMinutes, targetWorld, selectedScriptName);
            }

            ImGui.Separator();

            ImGui.Text("Upcoming World Hop Tasks:");
            for (WorldHopTask task : script.getWorldHopTasks()) {
                ImGui.Text("World hop in " + task.getRemainingTime() + " to world " + task.getTargetWorld() + " running script " + (task.getScriptName() != null ? task.getScriptName() : "none"));
                if (ImGui.Button("Remove Task##" + task.hashCode())) {
                    script.removeWorldHopTask(task);
                }
            }
            
            ImGui.Separator();
            
            ImmutableScript activeScript = ScriptController.getActiveScript();
            ImGui.Text("Currently Active Script: " + (activeScript != null ? activeScript.getName() : "None"));

            ImGui.Separator();
            ImGui.Text("Log");
            for (String log : script.getLogMessages()) {
                ImGui.Text(log);
            }
            script.manageLogSize();

            ImGui.End();
        }

        ImGui.PopStyleVar(4);
        ImGui.PopStyleColor(12);
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
    
    public class ImGuiCol {
        public static final int Text = 0;
        public static final int TextDisabled = 1;
        public static final int WindowBg = 2;
        public static final int ChildBg = 3;
        public static final int PopupBg = 4;
        public static final int Border = 5;
        public static final int BorderShadow = 6;
        public static final int FrameBg = 7;
        public static final int FrameBgHovered = 8;
        public static final int FrameBgActive = 9;
        public static final int TitleBg = 10;
        public static final int TitleBgActive = 11;
        public static final int TitleBgCollapsed = 12;
        public static final int MenuBarBg = 13;
        public static final int ScrollbarBg = 14;
        public static final int ScrollbarGrab = 15;
        public static final int ScrollbarGrabHovered = 16;
        public static final int ScrollbarGrabActive = 17;
        public static final int CheckMark = 18;
        public static final int SliderGrab = 19;
        public static final int SliderGrabActive = 20;
        public static final int Button = 21;
        public static final int ButtonHovered = 22;
        public static final int ButtonActive = 23;
        public static final int Header = 24;
        public static final int HeaderHovered = 25;
        public static final int HeaderActive = 26;
        public static final int Separator = 27;
        public static final int SeparatorHovered = 28;
        public static final int SeparatorActive = 29;
        public static final int ResizeGrip = 30;
        public static final int ResizeGripHovered = 31;
        public static final int ResizeGripActive = 32;
        public static final int Tab = 33;
        public static final int TabHovered = 34;
        public static final int TabActive = 35;
        public static final int TabUnfocused = 36;
        public static final int TabUnfocusedActive = 37;
        public static final int DockingPreview = 38;
        public static final int DockingEmptyBg = 39;
        public static final int PlotLines = 40;
        public static final int PlotLinesHovered = 41;
        public static final int PlotHistogram = 42;
        public static final int PlotHistogramHovered = 43;
        public static final int TableHeaderBg = 44;
        public static final int TableBorderStrong = 45;
        public static final int TableBorderLight = 46;
        public static final int TableRowBg = 47;
        public static final int TableRowBgAlt = 48;
        public static final int TextSelectedBg = 49;
        public static final int DragDropTarget = 50;
        public static final int NavHighlight = 51;
        public static final int NavWindowingHighlight = 52;
        public static final int NavWindowingDimBg = 53;
        public static final int ModalWindowDimBg = 54;
    }

    public class ImGuiStyleVar {
        public static final int Alpha = 0;
        public static final int DisabledAlpha = 1;
        public static final int WindowPadding = 2;
        public static final int WindowRounding = 3;
        public static final int WindowBorderSize = 4;
        public static final int WindowMinSize = 5;
        public static final int WindowTitleAlign = 6;
        public static final int ChildRounding = 7;
        public static final int ChildBorderSize = 8;
        public static final int PopupRounding = 9;
        public static final int PopupBorderSize = 10;
        public static final int FramePadding = 11;
        public static final int FrameRounding = 12;
        public static final int FrameBorderSize = 13;
        public static final int ItemSpacing = 14;
        public static final int ItemInnerSpacing = 15;
        public static final int IndentSpacing = 16;
        public static final int CellPadding = 17;
        public static final int ScrollbarSize = 18;
        public static final int ScrollbarRounding = 19;
        public static final int GrabMinSize = 20;
        public static final int GrabRounding = 21;
        public static final int TabRounding = 22;
        public static final int ButtonTextAlign = 23;
        public static final int SelectableTextAlign = 24;
        public static final int COUNT = 25;
    }
}
