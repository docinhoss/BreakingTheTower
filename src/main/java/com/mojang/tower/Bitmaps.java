package com.mojang.tower;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import javax.imageio.ImageIO;

public class Bitmaps
{
    public BufferedImage[] trees;
    public BufferedImage[] farmPlots;
    public BufferedImage[] rocks;
    public BufferedImage[] carriedResources;
    public BufferedImage[][] peons;
    public BufferedImage island;
    public BufferedImage towerTop;
    public BufferedImage towerMid;
    public BufferedImage towerBot;
    public BufferedImage[] smoke;
    public BufferedImage[] infoPuffs;
    public BufferedImage[][] houses;
    public BufferedImage delete, help;
    public BufferedImage[] soundButtons;

    public BufferedImage logo, wonScreen;

    // Path to res folder for hot-reload (null = use classpath)
    private static String resPath = null;
    private static WatchService watchService = null;
    private static Thread watchThread = null;
    private static Bitmaps watchTarget = null;
    private static long lastReloadTime = 0;
    private static final long DEBOUNCE_MS = 100; // Ignore rapid successive changes

    public static void setResPath(String path)
    {
        resPath = path;
    }

    public void startWatching()
    {
        if (resPath == null) return;

        watchTarget = this;

        try
        {
            watchService = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(resPath);
            dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            watchThread = new Thread(Bitmaps::watchLoop, "SpriteWatcher");
            watchThread.setDaemon(true);
            watchThread.start();

            System.out.println("Auto-reload enabled: watching res/ for changes");
        }
        catch (IOException e)
        {
            System.err.println("Failed to start file watcher: " + e.getMessage());
        }
    }

    private static void watchLoop()
    {
        while (true)
        {
            try
            {
                WatchKey key = watchService.take(); // Blocks until event

                for (WatchEvent<?> event : key.pollEvents())
                {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path changed = (Path) event.context();
                    String filename = changed.toString().toLowerCase();

                    if (filename.endsWith(".gif"))
                    {
                        long now = System.currentTimeMillis();
                        if (now - lastReloadTime > DEBOUNCE_MS)
                        {
                            lastReloadTime = now;

                            // Small delay to let file finish writing
                            Thread.sleep(50);

                            System.out.println("Detected change: " + changed + " - reloading sprites...");
                            try
                            {
                                watchTarget.loadAll();
                                System.out.println("Sprites reloaded!");
                            }
                            catch (IOException e)
                            {
                                System.err.println("Reload failed: " + e.getMessage());
                            }
                        }
                    }
                }

                if (!key.reset())
                {
                    System.err.println("Watch key invalid, stopping watcher");
                    break;
                }
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }

    public static void stopWatching()
    {
        if (watchService != null)
        {
            try
            {
                watchService.close();
            }
            catch (IOException e) {}
        }
        if (watchThread != null)
        {
            watchThread.interrupt();
        }
    }

    private BufferedImage loadImage(String name) throws IOException
    {
        if (resPath != null)
        {
            File file = new File(resPath, name);
            if (file.exists())
            {
                return ImageIO.read(file);
            }
        }
        // Fallback to classpath
        return ImageIO.read(Bitmaps.class.getResource("/" + name));
    }

    public void loadAll() throws IOException
    {
        logo = loadImage("logo.gif");
        wonScreen = loadImage("winscreen.gif");
        BufferedImage src = loadImage("sheet.gif");
        trees = new BufferedImage[16];
        for (int i=0; i<16; i++)
            trees[i] = clip(src, 32+i*8, 0, 8, 16);

        farmPlots = new BufferedImage[9];
        for (int i=0; i<9; i++)
            farmPlots[i] = clip(src, 32+i*8, 11*8, 8, 8);

        rocks = new BufferedImage[4];
        for (int i=0; i<4; i++)
            rocks[i] = clip(src, 32+12*8+i*8, 16, 8, 8);
        
        carriedResources = new BufferedImage[4];
        for (int i=0; i<4; i++)
            carriedResources[i] = clip(src, 32+12*8+i*8, 16+16, 8, 8);

        delete = clip(src, 32+16*8+3*16, 0, 16, 16);
        help = clip(src, 32+16*8+3*16, 16, 16, 16);
        soundButtons = new BufferedImage[2];
        for (int i=0; i<2; i++)
            soundButtons[i] = clip(src, 32+16*8+3*16, 32+i*16, 16, 16);
        
        houses = new BufferedImage[3][8];
        for (int x=0; x<3; x++)
            for (int y=0; y<8; y++)
                houses[x][y] = clip(src, 32+16*8+x*16, y*16, 16, 16);
        
        peons = new BufferedImage[4][3*4];
        for (int i=0; i<4; i++)
            for (int j=0; j<3*4; j++)
                peons[i][j] = clip(src, 32+j*8, 16+i*8, 8, 8);
        
        towerTop = clip(src, 0, 0, 32, 16);
        towerMid = clip(src, 0, 16, 32, 8);
        towerBot = clip(src, 0, 24, 32, 8);
        
        smoke = new BufferedImage[5];
        for (int i=0; i<5; i++)
            smoke[i] = clip(src, 256-8, i*8, 8, 8);
        
        infoPuffs = new BufferedImage[5];
        for (int i=0; i<5; i++)
            infoPuffs[i] = clip(src, 256-8-16, i*8, 16, 8);
        
        island = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[256*256];
        loadImage("island.gif").getRGB(0, 0, 256, 256, pixels, 0, 256);
        island.setRGB(0, 0, 256, 256, pixels, 0, 256);
    }

    public static BufferedImage clip(BufferedImage src, int x, int y, int w, int h)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        BufferedImage newImage = null;

        try
        {
            GraphicsDevice screen = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = screen.getDefaultConfiguration();
            newImage = gc.createCompatibleImage(w, h, Transparency.BITMASK);
        }
        catch (Exception e)
        {
        }

        if (newImage == null)
        {
            newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        int[] pixels = new int[w * h];
        src.getRGB(x, y, w, h, pixels, 0, w);
        newImage.setRGB(0, 0, w, h, pixels, 0, w);

        return newImage;
    }
}