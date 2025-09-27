package net.runelite.client.plugins.bingo;

import com.google.gson.annotations.SerializedName;

public class Tile
{
    private int id;
    private String name;
    private int itemId;
    private boolean completed;
    private String objective;

    @SerializedName("screenshotPath")
    private String screenshotPath;

    public Tile(int id, String name, int itemId, boolean completed, String objective)
    {
        this.id = id;
        this.name = name;
        this.itemId = itemId;
        this.completed = completed;
        this.objective = objective;
        this.screenshotPath = null;
    }

    // ---------------------
    // Getters & Setters
    // ---------------------
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }
}
