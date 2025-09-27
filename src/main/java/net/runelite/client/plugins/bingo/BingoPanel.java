package net.runelite.client.plugins.bingo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BingoPanel extends PluginPanel
{
    private final List<Tile> tiles;
    private final BingoConfig config;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private NavigationButton navButton;
    private JPanel gridPanel;

    private static final File SAVE_FILE =
            new File(System.getProperty("user.home"), ".runelite/bingo/bingo_tiles.json");

    public BingoPanel(List<Tile> tiles, BingoConfig config, ItemManager itemManager, ConfigManager configManager)
    {
        super();
        this.tiles = (tiles != null ? tiles : new ArrayList<>());
        this.config = config;
        this.itemManager = itemManager;
        this.configManager = configManager;

        setLayout(new BorderLayout());
        setBackground(new Color(32, 32, 32));
        setPreferredSize(new Dimension(600, 400));
    }

    public void init()
    {
        removeAll();
        buildHeader();
        loadOrCreateTiles();
        buildGridPanel();
        buildControls();
        buildNavigationButton();
        revalidate();
        repaint();
    }

    public NavigationButton getNavigationButton() { return navButton; }

    public void shutdown()
    {
        saveTiles();
        removeAll();
        revalidate();
        repaint();
    }

    // ---------------------
    // UI Setup
    // ---------------------
    private void buildHeader()
    {
        JLabel header = new JLabel("Shall We Slay Bingo!", SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(header, BorderLayout.NORTH);
    }

    private void loadOrCreateTiles()
    {
        List<Tile> loaded = loadTiles();
        if (!loaded.isEmpty())
        {
            tiles.clear();
            tiles.addAll(loaded);
        }
        else if (tiles.isEmpty())
        {
            tiles.addAll(getDefaultTiles());
            saveTiles();
        }
    }

    private void buildGridPanel()
    {
        gridPanel = new JPanel();
        gridPanel.setBackground(getBackground());
        gridPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(gridPanel, BorderLayout.CENTER);
        buildGrid();
    }

    private void buildControls()
    {
        JButton exportBtn = new JButton("Export JSON");
        exportBtn.addActionListener(e -> exportTiles());

        JButton importBtn = new JButton("Import JSON");
        importBtn.addActionListener(e -> importTiles());

        JButton resetBoardBtn = new JButton("Reset Board");
        resetBoardBtn.addActionListener(e -> resetBoard());

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBackground(getBackground());
        controls.add(exportBtn);
        controls.add(importBtn);
        controls.add(resetBoardBtn);
        add(controls, BorderLayout.SOUTH);
    }

    private void buildNavigationButton()
    {
        navButton = NavigationButton.builder()
                .tooltip("Bingo")
                .icon(ImageUtil.loadImageResource(getClass(), "icon.png"))
                .priority(5)
                .panel(this)
                .build();
    }

    // ---------------------
    // Grid + Tiles
    // ---------------------
    private void buildGrid()
    {
        gridPanel.removeAll();
        int size = Math.max(1, config.gridSize());
        gridPanel.setLayout(new GridLayout(size, size, 4, 4));

        int total = size * size;
        for (int i = 0; i < total; i++)
        {
            Tile tile = (i < tiles.size()) ? tiles.get(i) : new Tile(i, "Empty", -1, false, "");
            gridPanel.add(createTileButton(tile));
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JToggleButton createTileButton(Tile tile)
    {
        JToggleButton button = new JToggleButton();
        button.setLayout(new BorderLayout());
        button.setFocusPainted(false);
        button.setBackground(tile.isCompleted() ? new Color(0, 128, 0) : new Color(40, 40, 40));
        button.setBorder(new LineBorder(new Color(90, 90, 90)));
        button.setSelected(tile.isCompleted());

        JLabel imgLabel = new JLabel("?", SwingConstants.CENTER);
        button.add(imgLabel, BorderLayout.CENTER);

        button.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e)
            {
                loadTileIcon(tile, imgLabel, button.getWidth(), button.getHeight());
            }
        });

        String tooltip = "<html><b>" + safe(tile.getName()) + "</b>";
        if (tile.getObjective() != null && !tile.getObjective().isEmpty())
        {
            tooltip += "<br/>" + safe(tile.getObjective());
        }
        tooltip += "</html>";
        button.setToolTipText(tooltip);

        button.addActionListener(e -> handleTileClick(tile, button));

        return button;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void loadTileIcon(Tile tile, JLabel imgLabel, int width, int height)
    {
        try
        {
            int s = Math.max(16, Math.min(width, height) - 12);

            if (tile.getItemId() > 0)
            {
                BufferedImage icon = itemManager.getImage(tile.getItemId());
                if (icon != null)
                {
                    Image scaled = icon.getScaledInstance(s, s, Image.SCALE_SMOOTH);
                    imgLabel.setText(null);
                    imgLabel.setIcon(new ImageIcon(scaled));
                    return;
                }
            }

            imgLabel.setIcon(null);
            imgLabel.setText("?");
        }
        catch (Exception e)
        {
            imgLabel.setIcon(null);
            imgLabel.setText("?");
        }
    }

    private void handleTileClick(Tile tile, JToggleButton button)
    {
        if (button.isSelected())
        {
            openScreenshotChooser(tile);

            if (tile.isCompleted())
            {
                button.setBackground(new Color(0, 128, 0));
                saveTiles();
                sendWebhook("Tile completed: " + tile.getName(), tile.getScreenshotPath() != null ? new File(tile.getScreenshotPath()) : null);
            }
            else
            {
                button.setSelected(false);
            }
        }
        else
        {
            tile.setCompleted(false);
            tile.setScreenshotPath(null);
            button.setBackground(new Color(40, 40, 40));
            saveTiles();
        }
    }

    // ---------------------
    // Screenshots
    // ---------------------
    private File getRuneLiteScreenshotDir()
    {
        String dir = configManager.getConfiguration("screenshot", "directory");
        if (dir == null || dir.isEmpty())
        {
            dir = System.getProperty("user.home") + File.separator + ".runelite" + File.separator + "screenshots";
        }

        File folder = new File(dir);
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    private void openScreenshotChooser(Tile tile)
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(getRuneLiteScreenshotDir());
        chooser.setDialogTitle("Attach Screenshot for " + tile.getName());
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION)
        {
            File selectedFile = chooser.getSelectedFile();
            tile.setScreenshotPath(selectedFile.getAbsolutePath());
            tile.setCompleted(true);
            saveTiles();
        }
    }

    // ---------------------
    // Discord Webhook
    // ---------------------
    private void sendWebhook(String message, File file)
    {
        String webhookUrl = config.teamAWebhook();
        if (webhookUrl == null || webhookUrl.isEmpty() || file == null || !file.exists()) return;

        String boundary = "===" + System.currentTimeMillis() + "===";
        String LINE_FEED = "\r\n";

        try
        {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true))
            {
                // Text field
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"content\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(message).append(LINE_FEED);
                writer.flush();

                // File part
                String fileName = file.getName();
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"")
                        .append(LINE_FEED);
                writer.append("Content-Type: application/octet-stream").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                try (FileInputStream inputStream = new FileInputStream(file))
                {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1)
                    {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                }

                writer.append(LINE_FEED).flush();
                writer.append("--").append(boundary).append("--").append(LINE_FEED);
                writer.flush();
            }

            int status = conn.getResponseCode();
            if (status != 200 && status != 204)
            {
                System.err.println("Discord webhook upload failed. HTTP " + status);
            }

            conn.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // ---------------------
    // Persistence
    // ---------------------
    private void saveTiles()
    {
        try
        {
            if (!SAVE_FILE.getParentFile().exists()) SAVE_FILE.getParentFile().mkdirs();
            try (Writer writer = new FileWriter(SAVE_FILE))
            {
                gson.toJson(tiles, writer);
            }
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    private List<Tile> loadTiles()
    {
        if (!SAVE_FILE.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(SAVE_FILE))
        {
            Type type = new TypeToken<List<Tile>>(){}.getType();
            List<Tile> loaded = gson.fromJson(reader, type);
            return loaded != null ? loaded : new ArrayList<>();
        }
        catch (Exception e) { e.printStackTrace(); return new ArrayList<>(); }
    }

    // ---------------------
    // Import/Export
    // ---------------------
    private void exportTiles()
    {
        String json = gson.toJson(tiles);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);

        JTextArea textArea = new JTextArea(json, 20, 50);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(this, scroll, "Tiles JSON (copied to clipboard)", JOptionPane.INFORMATION_MESSAGE);
    }

    private void importTiles()
    {
        JTextArea input = new JTextArea(15, 40);
        JScrollPane scroll = new JScrollPane(input);
        int result = JOptionPane.showConfirmDialog(this, scroll, "Paste Tiles JSON", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
        {
            String json = input.getText().trim();
            if (!json.isEmpty())
            {
                try
                {
                    Type type = new TypeToken<List<Tile>>(){}.getType();
                    List<Tile> imported = gson.fromJson(json, type);
                    if (imported == null || imported.isEmpty())
                    {
                        JOptionPane.showMessageDialog(this, "Invalid or empty JSON provided.", "Import Failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    tiles.clear();
                    tiles.addAll(imported);
                    saveTiles();
                    buildGrid();
                    JOptionPane.showMessageDialog(this, "Tiles imported successfully!");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to import JSON: " + e.getMessage(), "Import Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void resetBoard()
    {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to reset the board to default tiles?\n(This will delete your saved board file.)",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION)
        {
            tiles.clear();
            tiles.addAll(getDefaultTiles());
            if (SAVE_FILE.exists()) SAVE_FILE.delete();
            saveTiles();
            buildGrid();
            JOptionPane.showMessageDialog(this, "Board has been reset to default!");
        }
    }

    // ---------------------
    // Default Tiles
    // ---------------------
    private List<Tile> getDefaultTiles()
    {
        List<Tile> defaults = new ArrayList<>();
        defaults.add(new Tile(0, "DT2 Unique", 26243, false, "Obtain a unique drop from a Desert Treasure 2 boss (excluding Ingot, Tablet, and Awakener's Orb)."));
        defaults.add(new Tile(1, "Twinflame Staff", 30634, false, "Obtain the Twinflame Staff drop."));
        defaults.add(new Tile(2, "DK Ring Set", 6737, false, "As a team, collect all four Dagannoth King rings."));
        defaults.add(new Tile(3, "Any Godsword", 11804, false, "Craft any Godsword from scratch: Hilt + Shards 1, 2, 3."));
        defaults.add(new Tile(4, "Barrows Set", 12877, false, "As a team, obtain a complete Barrows armor set."));
        defaults.add(new Tile(5, "Moons of Peril Set", 31136, false, "As a team, collect a full Moons of Peril gear set."));
        defaults.add(new Tile(6, "Slayer Uniques", 4151, false, "Obtain 5 different Slayer task uniques as a team."));
        defaults.add(new Tile(7, "Zenyte Shard", 19493, false, "Obtain an uncut Zenyte."));
        defaults.add(new Tile(8, "Blood Shard", 24777, false, "Get a Blood Shard by pickpocketing or killing Vyrewatch."));
        defaults.add(new Tile(9, "Tormented Demons Unique", 29580, false, "Obtain any unique drop from Tormented Demons."));
        defaults.add(new Tile(10, "Wilderness Weapon", 22547, false, "Obtain a unique Wilderness weapon (Craw's, Thammaron's, Viggora's or Voidwaker)."));
        defaults.add(new Tile(11, "Zulrah Unique", 12921, false, "Obtain a unique drop from Zulrah."));
        defaults.add(new Tile(12, "Gauntlet Unique", 23842, false, "Obtain a unique drop from The Gauntlet."));
        defaults.add(new Tile(13, "Yama Unique", 30753, false, "Obtain a Yama unique drop."));
        defaults.add(new Tile(14, "Dragon Pickaxe", 11920, false, "Obtain a Dragon Pickaxe."));
        defaults.add(new Tile(15, "Armadyl Unique", 12649, false, "Obtain a unique drop from Kree'arra (Armadyl)."));
        defaults.add(new Tile(16, "KQ Head", 7981, false, "Obtain the Kalphite Queen Head."));
        defaults.add(new Tile(17, "Venator Bow", 27614, false, "Collect 5 Venator Shards from Phantom Muspah."));
        defaults.add(new Tile(18, "Bandos Unique", 12650, false, "Obtain a unique drop from General Graardor (Bandos)."));
        defaults.add(new Tile(19, "Nex Unique", 26382, false, "Obtain a Nex Unique."));
        defaults.add(new Tile(20, "Saradomin Unique", 12651, false, "Obtain a unique from Commander Zilyana, excluding the Saradomin Sword."));
        defaults.add(new Tile(21, "Doom of Mokhaiotl Unique", 31097, false, "Obtain a unique from Doom of Mokhaiotl (Avernic Treads, Eye of Ayak, Mokhaiotl Cloth)."));
        defaults.add(new Tile(22, "Zamorak Unique", 12652, false, "Obtain a unique from K'ril Tsutsaroth, excluding Steam Battlestaff."));
        defaults.add(new Tile(23, "Raids Unique", 20997, false, "Obtain any tradeable unique from Raids (COX/TOA/TOB)."));
        defaults.add(new Tile(24, "Fire Cape Challenge", 6570, false, "All team members must complete Fight Caves, Inferno, or obtain Dizana’s Quiver."));
        return defaults;
    }
}
