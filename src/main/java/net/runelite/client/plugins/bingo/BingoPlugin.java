package net.runelite.client.plugins.bingo;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "Shall We Slay Bingo",
        description = "Track team objectives, unique drops, and completed tiles for Shall We Slay events.",
        tags = {"bingo", "team", "events"},
        enabledByDefault = false
)
public class BingoPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientToolbar clientToolbar;
    @Inject private BingoConfig config;
    @Inject private ItemManager itemManager;
    @Inject private ConfigManager configManager;

    private BingoPanel panel;

    @Provides
    BingoConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BingoConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("✅ Bingo plugin starting up...");

        panel = new BingoPanel(null, config, itemManager, configManager);
        panel.init();

        clientToolbar.addNavigation(panel.getNavigationButton());
    }

    @Override
    protected void shutDown()
    {
        log.info("🛑 Bingo plugin shutting down...");

        if (panel != null)
        {
            panel.shutdown();
            clientToolbar.removeNavigation(panel.getNavigationButton());
            panel = null;
        }
    }
}
