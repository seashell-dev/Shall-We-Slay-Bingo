package net.runelite.client.plugins.bingo;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bingo")
public interface BingoConfig extends Config
{
    @ConfigItem(
            keyName = "offlineMode",
            name = "Offline Mode",
            description = "Allow completing tiles without requiring a screenshot or Discord upload",
            position = 1
    )
    default boolean offlineMode() { return false; }

    @ConfigItem(
            keyName = "teamAWebhook",
            name = "Discord Webhook",
            description = "Webhook for proof of drop",
            position = 2
    )
    default String teamAWebhook() { return ""; }

    @ConfigItem(
            keyName = "gridSize",
            name = "Grid Size",
            description = "Size of the bingo board grid. Changing this requires disabling and then re-enabling the plugin to take effect.",
            position = 3
    )
    default int gridSize() { return 5; }
}
