package net.botwithus;

public class WorldHopTask {
    private int delayMinutes;
    private long startTime;
    private int targetWorld;
    private String scriptName;

    public WorldHopTask(int delayMinutes, int targetWorld, String scriptName) { 
        this.delayMinutes = delayMinutes;
        this.targetWorld = targetWorld;
        this.scriptName = scriptName;
        this.startTime = -1;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    public int getTargetWorld() {
        return targetWorld;
    }

    public String getScriptName() {  
        return scriptName;
    }

    public boolean isTimeToHop() {
        if (startTime == -1) return false;
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long delayInMillis = delayMinutes * 60 * 1000;
        return elapsedTime >= delayInMillis;
    }

    public String getRemainingTime() {
        if (startTime == -1) return String.format("%02d:%02d", delayMinutes, 0);

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        long delayInMillis = delayMinutes * 60 * 1000;
        long remainingTime = delayInMillis - elapsedTime;

        if (remainingTime <= 0) {
            return "00:00";
        }

        long remainingSeconds = (remainingTime / 1000) % 60;
        long remainingMinutes = (remainingTime / (1000 * 60)) % 60;
        long remainingHours = (remainingTime / (1000 * 60 * 60)) % 24;

        if (remainingHours > 0) {
            return String.format("%02d:%02d:%02d", remainingHours, remainingMinutes, remainingSeconds);
        } else {
            return String.format("%02d:%02d", remainingMinutes, remainingSeconds);
        }
    }

    public void start() {
        this.startTime = System.currentTimeMillis();
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }
}
