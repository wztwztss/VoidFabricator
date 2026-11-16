import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

/**
 * Deterministic static preview for the current 325x309 GT-style pattern UI.
 *
 * <p>This is a layout preview only. It loads GregTech slot/button/logo/circuit textures when the
 * configured instance jar is available, and otherwise falls back to painted approximations. It is
 * not an in-game screenshot.
 */
public final class RenderPatternUi {

    private static final int S = 3;
    private static final int PANEL_WIDTH = 325;
    private static final int PANEL_HEIGHT = 309;
    private static final int MARGIN = 10;
    private static final int TITLE_HEIGHT = 15;
    private static final int CANVAS_WIDTH = PANEL_WIDTH + MARGIN * 2;
    private static final int CANVAS_HEIGHT = PANEL_HEIGHT + MARGIN * 2 + TITLE_HEIGHT;
    private static final int PANEL_X = MARGIN;
    private static final int PANEL_Y = MARGIN + TITLE_HEIGHT;

    private static final int CONTENT_X = 7;
    private static final int CONTENT_Y = 6;
    private static final int CONTENT_WIDTH = 311;
    private static final int CONTENT_HEIGHT = 217;
    private static final int COLUMNS = 13;
    private static final int VISIBLE_ROWS = 7;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_Y = 38;
    private static final int LIST_HEIGHT = 154;
    private static final int TIER_WIDTH = 40;
    private static final int CELL_START_X = 42;
    private static final int CELL_STEP = 20;
    private static final int CELL_SIZE = 20;
    private static final int SCROLL_X = 302;
    private static final int SEARCH_X = 204;
    private static final int RESTORE_X = 122;
    private static final int ENABLE_ALL_X = 144;
    private static final int DISABLE_ALL_X = 216;
    private static final int GT_LOGO_X = 288;
    private static final int BOTTOM_Y = 197;

    private static final String GT_JAR =
        "E:\\wzt\\MC\\PL genmulu\\GT New Horizons 2.9.0_migrated\\.minecraft\\mods\\gregtech-5.09.54.265.jar";
    private static final String OUT_DIR = "E:\\wzt\\MC\\modcreater\\VoidFabricator\\docs";

    private static final Color PANEL_TOP = new Color(0xD8D8D8);
    private static final Color PANEL_BOTTOM = new Color(0xBDBDBD);
    private static final Color PANEL_EDGE = new Color(0x343434);
    private static final Color PANEL_LIGHT = new Color(0xF4F4F4);
    private static final Color PANEL_SHADOW = new Color(0x858585);
    private static final Color TEXT_DARK = new Color(0x303030);
    private static final Color TEXT_MUTED = new Color(0x5A5A5A);
    private static final Color TEXT_GREEN = new Color(0x1E7A1E);
    private static final Color TEXT_RED = new Color(0xB02020);
    private static final Color FIELD_BG = new Color(0xFAFAFA);
    private static final Color FIELD_EDGE = new Color(0x4A4A4A);
    private static final Color ROW_DARK = new Color(0, 0, 0, 22);
    private static final Color ROW_DARK_ALT = new Color(0, 0, 0, 42);
    private static final Color TIER_HIGHLIGHT = new Color(0x38, 0x79, 0xB5, 0x46);
    private static final Color DIM = new Color(0, 0, 0, 112);

    private static final String[] TIER_NAMES = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM",
        "UV", "UHV", "UEV", "UIV", "UMV", "UXV", "MAX" };

    private static Font BASE;
    private static Graphics2D G;
    private static ZipFile GT_ZIP;

    private static final Map<String, Image> TEXTURES = new HashMap<>();
    private static final Map<String, Image> TINTED = new HashMap<>();

    private RenderPatternUi() {}

    public static void main(String[] args) throws Exception {
        BASE = loadFont();
        GT_ZIP = openGtJar();
        BufferedImage image = render();
        G.dispose();
        closeGtJar();

        Path out = Paths.get(OUT_DIR);
        Files.createDirectories(out);
        File target = out.resolve("ui-preview-pattern-ui-325x309.png")
            .toFile();
        ImageIO.write(image, "png", target);
        System.out.println("RENDER=" + target.getAbsolutePath());
        System.out.println("SIZE=" + image.getWidth() + "x" + image.getHeight());
    }

    private static BufferedImage render() {
        BufferedImage image = new BufferedImage(CANVAS_WIDTH * S, CANVAS_HEIGHT * S, BufferedImage.TYPE_INT_RGB);
        G = image.createGraphics();
        G.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        G.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        G.scale(S, S);

        G.setColor(new Color(0x101010));
        G.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        drawPanel();
        drawTitle();
        drawHeader();
        drawToolbar();
        drawList();
        drawScrollbar();
        drawBottom();
        drawPlayerInventory();
        return image;
    }

    private static void drawPanel() {
        int x = PANEL_X;
        int y = PANEL_Y;
        Image panel = texture("assets/gregtech/textures/gui/background/singleblock_default.png");
        if (panel != null) {
            drawNinePatch(panel, x, y, PANEL_WIDTH, PANEL_HEIGHT, 4);
            return;
        }
        G.setColor(PANEL_EDGE);
        G.fillRoundRect(x - 2, y - 2, PANEL_WIDTH + 4, PANEL_HEIGHT + 4, 4, 4);
        G.setPaint(new GradientPaint(x, y, PANEL_TOP, x, y + PANEL_HEIGHT, PANEL_BOTTOM));
        G.fillRoundRect(x, y, PANEL_WIDTH, PANEL_HEIGHT, 3, 3);

        G.setColor(PANEL_LIGHT);
        G.drawLine(x + 1, y + 1, x + PANEL_WIDTH - 2, y + 1);
        G.drawLine(x + 1, y + 1, x + 1, y + PANEL_HEIGHT - 2);
        G.setColor(PANEL_SHADOW);
        G.drawLine(x + 1, y + PANEL_HEIGHT - 2, x + PANEL_WIDTH - 2, y + PANEL_HEIGHT - 2);
        G.drawLine(x + PANEL_WIDTH - 2, y + 1, x + PANEL_WIDTH - 2, y + PANEL_HEIGHT - 2);
    }

    private static void drawTitle() {
        int x = PANEL_X;
        int y = MARGIN;
        Image title = texture("assets/gregtech/textures/gui/tab/title.png");
        if (title != null) {
            drawNinePatch(title, x, y, 86, TITLE_HEIGHT, 4);
        } else {
            G.setColor(PANEL_EDGE);
            G.fillRect(x - 1, y - 1, 88, TITLE_HEIGHT + 2);
            G.setColor(new Color(0xCBCBCB));
            G.fillRect(x, y, 86, TITLE_HEIGHT);
            G.setColor(PANEL_LIGHT);
            G.drawLine(x, y, x + 85, y);
            G.setColor(PANEL_SHADOW);
            G.drawLine(x, y + TITLE_HEIGHT - 1, x + 85, y + TITLE_HEIGHT - 1);
        }
        centered(x, y, 86, TITLE_HEIGHT, "机器配方池样板", TEXT_DARK, font(7));
    }

    private static void drawHeader() {
        int x = CONTENT_X + PANEL_X;
        int y = CONTENT_Y + PANEL_Y;

        drawSlot(x, y + 7, 18);
        drawMachineIcon(x + 1, y + 8, 16);

        String machine = "机器：工业高炉";
        text(machine, x + 23, y + 12, new Color(0x00AAAA), font(8));
        String voltage = "电压：HV";
        textRight(voltage, CONTENT_X + PANEL_X + CONTENT_WIDTH, y + 12, new Color(0x55FF55), font(8));

        String status = "就绪";
        text(status, x, y + 31, new Color(0x00AA00), font(6));
    }

    private static void drawToolbar() {
        int x = CONTENT_X + PANEL_X;
        int y = CONTENT_Y + PANEL_Y;

        drawButton(x + 58, y + 26, 38, 11, "黑名单", false, 5);
        drawButton(x + 98, y + 26, 44, 11, "筛选相同", false, 5);
        drawButton(x + 144, y + 26, 17, 11, "删", false, 5);
        drawButton(x + 163, y + 26, 38, 11, "倍增", false, 5);

        int searchX = x + SEARCH_X;
        int searchY = y + 26;
        int searchW = CONTENT_WIDTH - SEARCH_X;
        Image field = texture("assets/gregtech/textures/gui/background/text_field.png");
        if (field != null) {
            drawNinePatch(field, searchX, searchY, searchW, 11, 3);
        } else {
            G.setColor(FIELD_EDGE);
            G.fillRect(searchX - 1, searchY - 1, searchW + 2, 13);
            G.setColor(FIELD_BG);
            G.fillRect(searchX, searchY, searchW, 11);
        }
        text("搜索产物、流体或配方来源…", searchX + 3, searchY + 3, new Color(0xB0B0B0), font(5));
    }

    private static void drawList() {
        int x = CONTENT_X + PANEL_X;
        int y = CONTENT_Y + PANEL_Y + LIST_Y;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            drawRow(x, y + row * ROW_HEIGHT, row, rowTier(row));
        }
    }

    private static void drawRow(int x, int y, int row, int tier) {
        boolean highlighted = row == VISIBLE_ROWS - 1;
        G.setColor(highlighted ? TIER_HIGHLIGHT : (row % 2 == 0 ? ROW_DARK : ROW_DARK_ALT));
        G.fillRect(x, y, SCROLL_X - 2, ROW_HEIGHT - 1);

        int labelColor = row == 0 || row == 2 || row == 4 || row == 6 ? TEXT_GREEN.getRGB() : TEXT_MUTED.getRGB();
        if (row == 0 || row == 2 || row == 4 || row == 6) {
            drawTierButton(x, y + 1, TIER_NAMES[tier], new Color(labelColor));
        }

        for (int col = 0; col < COLUMNS; col++) {
            int index = row * COLUMNS + col;
            drawCell(x + CELL_START_X + col * CELL_STEP, y + 1, index);
        }
    }

    private static void drawCell(int x, int y, int index) {
        drawSlot(x, y, CELL_SIZE);
        boolean fluid = index % 11 == 0;
        boolean dimmed = index == 3 || index == 17 || index == 44 || index == 70 || index == 85;
        if (fluid) {
            drawFluidIcon(x + 1, y + 1, CELL_SIZE - 2, index);
            drawAmount(x + CELL_SIZE - 2, y + CELL_SIZE - 3, "1000", dimmed, 5);
        } else {
            drawItemIcon(index % 7, x + 1, y + 1, CELL_SIZE - 2, index);
            drawAmount(x + CELL_SIZE - 2, y + CELL_SIZE - 3, index % 5 == 0 ? "16" : "64", dimmed, 5);
        }
        if (dimmed) {
            G.setColor(new Color(0xD9221616));
            G.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
            G.setColor(new Color(0xFF8B1E1E));
            G.drawRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);
            Graphics2D g2 = (Graphics2D) G;
            g2.setColor(new Color(0xFFFF3030));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(x + 3, y + 3, x + CELL_SIZE - 4, y + CELL_SIZE - 4);
            g2.drawLine(x + CELL_SIZE - 4, y + 3, x + 3, y + CELL_SIZE - 4);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private static void drawTierButton(int x, int y, String label, Color color) {
        drawButtonBase(x, y, TIER_WIDTH, CELL_SIZE, false);
        centered(x, y, TIER_WIDTH, CELL_SIZE, label, color, font(7));
    }

    private static void drawScrollbar() {
        int x = CONTENT_X + PANEL_X + SCROLL_X;
        int y = CONTENT_Y + PANEL_Y + LIST_Y;
        int h = LIST_HEIGHT;
        G.setColor(new Color(0x8C8C8C));
        G.fillRect(x, y, 8, h);
        G.setColor(new Color(0x545454));
        G.drawRect(x, y, 8, h);
        G.setColor(new Color(0xC8C8C8));
        G.fillRect(x + 1, y + 1, 6, h - 2);
        G.setColor(new Color(0x777777));
        G.fillRect(x + 1, y + 1, 6, 45);
        G.setColor(new Color(0xE8E8E8));
        G.drawLine(x + 2, y + 2, x + 5, y + 2);
    }

    private static void drawBottom() {
        int x = CONTENT_X + PANEL_X;
        int y = CONTENT_Y + PANEL_Y + BOTTOM_Y;
        text("已启用 84 / 91", x, y + 3, TEXT_MUTED, font(7));
        drawButton(x + RESTORE_X, y, 18, 18, "恢", false, 6);
        drawButton(x + ENABLE_ALL_X, y, 68, 18, "全部启用", false, 6);
        drawButton(x + DISABLE_ALL_X, y, 72, 18, "全部停用", false, 6, new Color(0xFF5555));
        Image logo = texture("assets/gregtech/textures/gui/picture/gt_logo_17x17_transparent.png");
        if (logo != null) {
            G.drawImage(logo, x + GT_LOGO_X, y, 18, 18, null);
        } else {
            drawButtonBase(x + GT_LOGO_X, y, 18, 18, false);
            centered(x + GT_LOGO_X, y, 18, 18, "GT", TEXT_DARK, font(5));
        }
    }

    private static void drawPlayerInventory() {
        int x = (PANEL_WIDTH - 162) / 2 + PANEL_X;
        int y = PANEL_Y + 227;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(x + col * 18, y + row * 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(x + col * 18, y + 58, 18);
        }
    }

    private static void drawNinePatch(Image image, int x, int y, int w, int h, int border) {
        int iw = image.getWidth(null);
        int ih = image.getHeight(null);
        int b = Math.min(border, Math.min(iw / 2, ih / 2));
        // corners
        G.drawImage(image, x, y, x + b, y + b, 0, 0, b, b, null);
        G.drawImage(image, x + w - b, y, x + w, y + b, iw - b, 0, iw, b, null);
        G.drawImage(image, x, y + h - b, x + b, y + h, 0, ih - b, b, ih, null);
        G.drawImage(image, x + w - b, y + h - b, x + w, y + h, iw - b, ih - b, iw, ih, null);
        // edges
        G.drawImage(image, x + b, y, x + w - b, y + b, b, 0, iw - b, b, null);
        G.drawImage(image, x + b, y + h - b, x + w - b, y + h, b, ih - b, iw - b, ih, null);
        G.drawImage(image, x, y + b, x + b, y + h - b, 0, b, b, ih - b, null);
        G.drawImage(image, x + w - b, y + b, x + w, y + h - b, iw - b, b, iw, ih - b, null);
        // center
        G.drawImage(image, x + b, y + b, x + w - b, y + h - b, b, b, iw - b, ih - b, null);
    }

    private static void drawButton(int x, int y, int w, int h, String label, boolean active, int textSize) {
        drawButton(x, y, w, h, label, active, textSize, active ? Color.WHITE : TEXT_DARK);
    }

    private static void drawButton(int x, int y, int w, int h, String label, boolean active, int textSize,
        Color color) {
        drawButtonBase(x, y, w, h, active);
        centered(x, y, w, h, label, color, font(textSize));
    }

    private static void drawButtonBase(int x, int y, int w, int h, boolean active) {
        Image button = texture("assets/gregtech/textures/gui/button/standard.png");
        if (button != null) {
            G.drawImage(button, x, y, w, h, null);
            if (active) {
                G.setColor(new Color(0x38, 0x79, 0xB5, 0x50));
                G.fillRect(x, y, w, h);
            }
            return;
        }
        G.setColor(active ? new Color(0x8FAFC0) : new Color(0xBDBDBD));
        G.fillRect(x, y, w, h);
        G.setColor(new Color(0xF0F0F0));
        G.drawLine(x, y, x + w - 1, y);
        G.drawLine(x, y, x, y + h - 1);
        G.setColor(new Color(0x666666));
        G.drawLine(x, y + h - 1, x + w - 1, y + h - 1);
        G.drawLine(x + w - 1, y, x + w - 1, y + h - 1);
    }

    private static void drawSlot(int x, int y, int size) {
        Image slot = texture("assets/gregtech/textures/gui/slot/item_standard.png");
        if (slot != null) {
            G.drawImage(slot, x, y, size, size, null);
            return;
        }
        G.setColor(new Color(0xB0B0B0));
        G.fillRect(x, y, size, size);
        G.setColor(new Color(0x4E4E4E));
        G.drawRect(x, y, size, size);
        G.setColor(new Color(0xE8E8E8));
        G.drawLine(x + 1, y + 1, x + size - 2, y + 1);
        G.drawLine(x + 1, y + 1, x + 1, y + size - 2);
    }

    private static void drawItemIcon(int kind, int x, int y, int size, int index) {
        String key;
        Color tint;
        switch (kind) {
            case 0 -> {
                key = "assets/gregtech/textures/items/materialicons/CUSTOM/crudesteel/ingot.png";
                tint = new Color(0xC9D4DC);
            }
            case 1 -> {
                key = "assets/gregtech/textures/items/materialicons/CUSTOM/crudesteel/plate.png";
                tint = new Color(0xB6C3CC);
            }
            case 2 -> {
                key = "assets/gregtech/textures/items/gt.circuitcomponent/processed/organizedpicocircuit.png";
                tint = null;
            }
            case 3 -> {
                key = "assets/gregtech/textures/items/gt.circuitcomponent/processed/boardquantum.png";
                tint = null;
            }
            case 4 -> {
                key = "assets/gregtech/textures/items/materialicons/CUSTOM/crudesteel/dust.png";
                tint = new Color(0xE0D8B8);
            }
            case 5 -> {
                key = "assets/gregtech/textures/items/materialicons/CUSTOM/crudesteel/plate.png";
                tint = new Color(0xD8B078);
            }
            default -> {
                key = "assets/gregtech/textures/items/gt.circuitcomponent/processed/organizedquantumcircuit.png";
                tint = null;
            }
        }
        Image image = texture(key);
        if (image == null) {
            drawFallbackIcon(x, y, size, kind);
            return;
        }
        if (tint != null) {
            image = tinted(key, image, tint);
        }
        if (image.getWidth(null) > 16 || image.getHeight(null) > 16) {
            image = cropFirstFrame(image);
        }
        G.drawImage(image, x, y, size, size, null);
    }

    private static void drawFluidIcon(int x, int y, int size, int index) {
        Color fluid = index % 33 == 0 ? new Color(0xFF8A3D) : new Color(0x3C9FE0);
        G.setColor(new Color(0x2A2A2A));
        G.fillRect(x, y, size, size);
        G.setColor(new Color(0x9A9A9A));
        G.drawRect(x, y, size, size);
        G.setColor(fluid);
        G.fillRect(x + 1, y + 3, size - 2, size - 4);
        G.setColor(fluid.brighter());
        G.fillRect(x + 1, y + 3, size - 2, 2);
        G.setColor(new Color(255, 255, 255, 90));
        G.fillRect(x + 3, y + 6, 2, size - 9);
        G.setColor(fluid.darker());
        G.fillRect(x + 1, y + size - 2, size - 2, 1);
    }

    private static void drawFallbackIcon(int x, int y, int size, int kind) {
        G.setColor(kind == 2 || kind == 3 || kind == 6 ? new Color(0x2E6B45) : new Color(0xB8C4CC));
        G.fillRoundRect(x + 2, y + 4, size - 4, size - 7, 2, 2);
        G.setColor(new Color(255, 255, 255, 100));
        G.drawLine(x + 3, y + 5, x + size - 3, y + 5);
    }

    private static void drawMachineIcon(int x, int y, int size) {
        G.setColor(new Color(0x3E4A50));
        G.fillRect(x, y, size, size);
        G.setColor(new Color(0x8A969C));
        G.drawRect(x, y, size, size);
        G.setColor(new Color(0x18262C));
        G.fillRect(x + 2, y + 2, size - 4, size - 5);
        G.setColor(new Color(0x4FD0D8));
        G.fillRect(x + 4, y + 4, size - 8, 3);
        G.setColor(new Color(0xB8C4C8));
        for (int i = 0; i < 3; i++) {
            G.fillRect(x + 4, y + 9 + i * 2, size - 8, 1);
        }
    }

    private static void drawAmount(int right, int bottom, String value, boolean dimmed, int fontSize) {
        Font font = font(fontSize);
        G.setFont(font);
        FontMetrics metrics = G.getFontMetrics();
        int width = metrics.stringWidth(value);
        G.setColor(new Color(0, 0, 0, 190));
        G.drawString(value, right - width + 1, bottom + 1);
        G.setColor(dimmed ? new Color(0xA0A0A0) : Color.WHITE);
        G.drawString(value, right - width, bottom);
    }

    private static int rowTier(int row) {
        return switch (row) {
            case 0, 1 -> 1;
            case 2, 3 -> 2;
            case 4, 5 -> 3;
            default -> 4;
        };
    }

    private static void centered(int x, int y, int w, int h, String value, Color color, Font font) {
        G.setFont(font);
        FontMetrics metrics = G.getFontMetrics();
        int tx = x + (w - metrics.stringWidth(value)) / 2;
        int ty = y + (h - metrics.getHeight()) / 2 + metrics.getAscent();
        G.setColor(color);
        G.drawString(value, tx, ty);
    }

    private static void text(String value, int x, int y, Color color, Font font) {
        G.setFont(font);
        G.setColor(color);
        G.drawString(value, x, y + G.getFontMetrics()
            .getAscent());
    }

    private static void textRight(String value, int right, int y, Color color, Font font) {
        G.setFont(font);
        FontMetrics metrics = G.getFontMetrics();
        G.setColor(color);
        G.drawString(value, right - metrics.stringWidth(value), y + metrics.getAscent());
    }

    private static Font font(float size) {
        return BASE.deriveFont(Font.PLAIN, size);
    }

    private static Font loadFont() throws Exception {
        File simhei = new File("C:\\Windows\\Fonts\\simhei.ttf");
        if (simhei.isFile()) {
            return Font.createFont(Font.TRUETYPE_FONT, simhei);
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    }

    private static ZipFile openGtJar() {
        try {
            return new ZipFile(GT_JAR);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void closeGtJar() {
        if (GT_ZIP != null) {
            try {
                GT_ZIP.close();
            } catch (Exception ignored) {
                // best-effort close
            }
        }
    }

    private static Image texture(String path) {
        if (TEXTURES.containsKey(path)) {
            return TEXTURES.get(path);
        }
        Image image = null;
        if (GT_ZIP != null) {
            ZipEntry entry = GT_ZIP.getEntry(path);
            if (entry != null) {
                try (InputStream in = GT_ZIP.getInputStream(entry)) {
                    image = ImageIO.read(in);
                } catch (Exception ignored) {
                    image = null;
                }
            }
        }
        TEXTURES.put(path, image);
        return image;
    }

    private static Image tinted(String key, Image source, Color tint) {
        String cacheKey = key + ":" + tint.getRGB();
        Image cached = TINTED.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, 16, 16, null);
        graphics.setComposite(AlphaComposite.SrcAtop);
        graphics.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 105));
        graphics.fillRect(0, 0, 16, 16);
        graphics.dispose();
        TINTED.put(cacheKey, image);
        return image;
    }

    private static Image cropFirstFrame(Image source) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(source, 0, 0, 16, 16, 0, 0, 16, 16, null);
        graphics.dispose();
        return image;
    }
}
