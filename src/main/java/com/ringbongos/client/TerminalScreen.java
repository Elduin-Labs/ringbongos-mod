package com.ringbongos.client;

import com.ringbongos.OSPayloads;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Ring Bong OS: a handheld-looking panel with a home screen of rounded app icons and one
 * screen per app. Everything is drawn by hand out of rounded rectangles — vanilla widgets
 * would break the illusion.
 */
public class TerminalScreen extends Screen {
    private static final int BODY_W = 190;
    private static final int BODY_H = 262;

    // Case.
    private static final int BEZEL = 0xFF101012;
    private static final int BEZEL_EDGE = 0xFF2C2C2E;
    // Screen.
    private static final int WALLPAPER = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int SEPARATOR = 0xFFE3E3E8;
    private static final int LABEL = 0xFF1C1C1E;
    private static final int SUBLABEL = 0xFF8E8E93;
    // Accents, borrowed from the usual system palette.
    private static final int BLUE = 0xFF0A84FF;
    private static final int ORANGE = 0xFFFF9F0A;
    private static final int GRAY = 0xFF8E8E93;
    private static final int GREEN = 0xFF30D158;
    private static final int ON_ACCENT = 0xFFFFFFFF;

    /** How often the OS asks the server for fresh numbers, in milliseconds. */
    private static final long REFRESH_MS = 1000L;

    private enum App {
        HOME("Ring Bong", GRAY),
        PING("Ping", BLUE),
        BONG("Bong", ORANGE),
        SYS("Status", GRAY),
        LOG("Log", GREEN);

        private final String title;
        private final int accent;

        App(String title, int accent) {
            this.title = title;
            this.accent = accent;
        }
    }

    private final BlockPos pos;
    private App app = App.HOME;
    private long lastRefresh = System.currentTimeMillis();
    private int left;
    private int top;

    public TerminalScreen(BlockPos pos) {
        super(Text.translatable("block.ringbongos.bong_terminal"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - BODY_W) / 2;
        top = (height - BODY_H) / 2;

        int screenLeft = left + 7;
        int screenWidth = BODY_W - 14;

        if (app == App.HOME) {
            // Two columns of app icons, the way a home screen lays them out.
            int gridLeft = screenLeft + 20;
            int gridTop = top + 62;
            App[] apps = {App.PING, App.BONG, App.SYS, App.LOG};
            for (int i = 0; i < apps.length; i++) {
                App target = apps[i];
                int x = gridLeft + (i % 2) * 78;
                int y = gridTop + (i / 2) * 74;
                addDrawableChild(new AppIcon(x, y, target, () -> switchTo(target)));
            }
            return;
        }

        addDrawableChild(new Pill(screenLeft + 6, top + 24, 44, 15, Text.literal("< Home"), BLUE, false,
                () -> switchTo(App.HOME)));

        if (app == App.BONG) {
            addDrawableChild(new Pill(screenLeft + 14, top + 150, screenWidth - 28, 22,
                    Text.literal("Ring"), ORANGE, true,
                    () -> ClientPlayNetworking.send(new OSPayloads.RingBong(pos))));
        }
    }

    private void switchTo(App next) {
        app = next;
        clearAndInit();
    }

    private void refresh() {
        lastRefresh = System.currentTimeMillis();
        ClientPlayNetworking.send(new OSPayloads.Refresh(pos));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (System.currentTimeMillis() - lastRefresh >= REFRESH_MS) {
            refresh();
        }
        super.render(context, mouseX, mouseY, delta);

        // Case and screen.
        roundRect(context, left - 2, top - 2, left + BODY_W + 2, top + BODY_H + 2, 16, BEZEL_EDGE);
        roundRect(context, left, top, left + BODY_W, top + BODY_H, 15, BEZEL);
        int screenLeft = left + 7;
        int screenRight = left + BODY_W - 7;
        int screenTop = top + 7;
        int screenBottom = top + BODY_H - 14;
        roundRect(context, screenLeft, screenTop, screenRight, screenBottom, 11, WALLPAPER);
        // Home indicator.
        roundRect(context, left + BODY_W / 2 - 22, top + BODY_H - 9, left + BODY_W / 2 + 22, top + BODY_H - 6,
                1, BEZEL_EDGE);

        OSPayloads.TerminalState snapshot = RingBongOSClient.state;

        // Status bar.
        String clock = snapshot == null ? "--:--" : clock(snapshot.timeOfDay());
        context.drawText(textRenderer, Text.literal(clock), screenLeft + 10, screenTop + 6, LABEL, false);
        String right = snapshot == null ? "" : (snapshot.powered() ? "R15" : "R0");
        context.drawText(textRenderer, Text.literal(right),
                screenRight - 10 - textRenderer.getWidth(right), screenTop + 6, SUBLABEL, false);

        if (snapshot == null) {
            centered(context, "Ring Bong OS", (screenLeft + screenRight) / 2, top + BODY_H / 2 - 4, LABEL);
            return;
        }

        int contentLeft = screenLeft + 10;
        int contentWidth = screenRight - screenLeft - 20;

        if (app == App.HOME) {
            centered(context, "Ring Bong OS", (screenLeft + screenRight) / 2, screenTop + 26, LABEL);
            centered(context, at(snapshot.pos()), (screenLeft + screenRight) / 2, screenTop + 38, SUBLABEL);
        } else {
            // A large title under the back button, the way a settings page reads.
            context.drawText(textRenderer, Text.literal(app.title), contentLeft, top + 44, LABEL, false);
            switch (app) {
                case PING -> renderPing(context, snapshot, contentLeft, contentWidth, top + 60);
                case BONG -> renderBong(context, snapshot, contentLeft, contentWidth, top + 60);
                case SYS -> renderSys(context, snapshot, contentLeft, contentWidth, top + 60);
                case LOG -> renderLog(context, snapshot, contentLeft, contentWidth, top + 60);
                default -> {
                }
            }
        }

        // Icons and pills are drawn by super.render() beneath the case, so put them back on top.
        for (var child : children()) {
            if (child instanceof AppIcon icon) {
                icon.render(context, mouseX, mouseY, delta);
            } else if (child instanceof Pill pill) {
                pill.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void renderPing(DrawContext context, OSPayloads.TerminalState snapshot, int x, int w, int y) {
        List<String> nearby = snapshot.nearby();
        if (nearby.isEmpty()) {
            card(context, x, y, w, List.of("Nobody within 64 blocks"), SUBLABEL);
            return;
        }
        card(context, x, y, w, nearby, LABEL);
    }

    private void renderBong(DrawContext context, OSPayloads.TerminalState snapshot, int x, int w, int y) {
        boolean ringing = snapshot.powered();
        roundRect(context, x, y, x + w, y + 66, 10, CARD);
        centered(context, ringing ? "Ringing" : "Idle", x + w / 2, y + 16, ringing ? ORANGE : LABEL);
        centered(context, ringing ? "redstone high" : "redstone low", x + w / 2, y + 30, SUBLABEL);
        centered(context, "Pulses redstone while", x + w / 2, y + 46, SUBLABEL);
        centered(context, "the chime plays.", x + w / 2, y + 56, SUBLABEL);
    }

    private void renderSys(DrawContext context, OSPayloads.TerminalState snapshot, int x, int w, int y) {
        rows(context, x, y, w, List.of(
                new String[]{"Location", at(snapshot.pos())},
                new String[]{"World", snapshot.dimension()},
                new String[]{"Day", String.valueOf(snapshot.timeOfDay() / 24000L)},
                new String[]{"Weather", snapshot.raining() ? "Raining" : "Clear"},
                new String[]{"Uptime", uptime(snapshot.uptimeTicks())},
                new String[]{"Redstone", snapshot.powered() ? "High" : "Low"}));
    }

    private void renderLog(DrawContext context, OSPayloads.TerminalState snapshot, int x, int w, int y) {
        List<String> log = snapshot.log();
        if (log.isEmpty()) {
            card(context, x, y, w, List.of("Nothing yet"), SUBLABEL);
            return;
        }
        card(context, x, y, w, log.subList(0, Math.min(log.size(), 9)), LABEL);
    }

    /** A white card holding one line of text per entry. */
    private void card(DrawContext context, int x, int y, int w, List<String> lines, int color) {
        int height = 10 + lines.size() * 13;
        roundRect(context, x, y, x + w, y + height, 10, CARD);
        int row = y + 6;
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                context.fill(x + 8, row - 2, x + w - 8, row - 1, SEPARATOR);
            }
            context.drawText(textRenderer, Text.literal(lines.get(i)), x + 9, row, color, false);
            row += 13;
        }
    }

    /** A white card of label/value rows, value right-aligned. */
    private void rows(DrawContext context, int x, int y, int w, List<String[]> entries) {
        int height = 10 + entries.size() * 13;
        roundRect(context, x, y, x + w, y + height, 10, CARD);
        int row = y + 6;
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                context.fill(x + 8, row - 2, x + w - 8, row - 1, SEPARATOR);
            }
            String[] entry = entries.get(i);
            context.drawText(textRenderer, Text.literal(entry[0]), x + 9, row, LABEL, false);
            context.drawText(textRenderer, Text.literal(entry[1]),
                    x + w - 9 - textRenderer.getWidth(entry[1]), row, SUBLABEL, false);
            row += 13;
        }
    }

    private void centered(DrawContext context, String text, int centerX, int y, int color) {
        context.drawText(textRenderer, Text.literal(text), centerX - textRenderer.getWidth(text) / 2, y,
                color, false);
    }

    private static String at(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /**
     * Fills a rectangle with its corners rounded off, which is the whole visual language of
     * this screen — icons, cards, buttons and the case itself are all made of these.
     */
    private static void roundRect(DrawContext context, int x0, int y0, int x1, int y1, int radius, int color) {
        int limit = Math.min(radius, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        for (int y = y0; y < y1; y++) {
            int fromEdge = -1;
            if (y < y0 + limit) {
                fromEdge = y - y0;
            } else if (y >= y1 - limit) {
                fromEdge = y1 - 1 - y;
            }
            int inset = 0;
            if (fromEdge >= 0) {
                int dy = limit - 1 - fromEdge;
                inset = limit - (int) Math.floor(Math.sqrt((double) limit * limit - (double) dy * dy));
            }
            context.fill(x0 + inset, y, x1 - inset, y + 1, color);
        }
    }

    /** Ticks since boot, as "1h 04m" / "04m 12s" / "12s". */
    private static String uptime(long ticks) {
        long seconds = ticks / 20L;
        long hours = seconds / 3600L;
        long minutes = seconds % 3600L / 60L;
        if (hours > 0L) {
            return hours + "h " + String.format("%02dm", minutes);
        }
        if (minutes > 0L) {
            return minutes + "m " + String.format("%02ds", seconds % 60L);
        }
        return seconds + "s";
    }

    private static String clock(long timeOfDay) {
        long ticks = timeOfDay % 24000L;
        long hours = (ticks / 1000L + 6L) % 24L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        RingBongOSClient.state = null;
    }

    /** A rounded app icon with its name underneath. */
    private static class AppIcon extends ClickableWidget {
        private static final int ICON = 40;

        private final App target;
        private final Runnable action;

        AppIcon(int x, int y, App target, Runnable action) {
            super(x, y, ICON, ICON + 12, Text.literal(target.title));
            this.target = target;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            boolean hovered = isHovered();
            int x = getX();
            int y = getY();
            // Pressed icons dim slightly, like a tap does.
            roundRect(context, x, y, x + ICON, y + ICON, 10, hovered ? blend(target.accent) : target.accent);
            // A single-letter glyph stands in for an app image.
            String glyph = target.title.substring(0, 1);
            context.drawText(textRenderer, Text.literal(glyph),
                    x + (ICON - textRenderer.getWidth(glyph)) / 2, y + ICON / 2 - 4, ON_ACCENT, false);
            context.drawText(textRenderer, getMessage(),
                    x + (ICON - textRenderer.getWidth(getMessage())) / 2, y + ICON + 3, LABEL, false);
        }

        @Override
        public void onClick(net.minecraft.client.gui.Click click, boolean doubled) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    /** A rounded button: filled for actions, plain text for navigation. */
    private static class Pill extends ClickableWidget {
        private final int accent;
        private final boolean filled;
        private final Runnable action;

        Pill(int x, int y, int width, int height, Text message, int accent, boolean filled, Runnable action) {
            super(x, y, width, height, message);
            this.accent = accent;
            this.filled = filled;
            this.action = action;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            boolean hovered = isHovered();
            int textColor;
            if (filled) {
                roundRect(context, getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        getHeight() / 2, hovered ? blend(accent) : accent);
                textColor = ON_ACCENT;
            } else {
                textColor = hovered ? blend(accent) : accent;
            }
            context.drawText(textRenderer, getMessage(),
                    getX() + (getWidth() - textRenderer.getWidth(getMessage())) / 2,
                    getY() + (getHeight() - 8) / 2, textColor, false);
        }

        @Override
        public void onClick(net.minecraft.client.gui.Click click, boolean doubled) {
            action.run();
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }

    /** Lightens a colour towards white, for hover and press states. */
    private static int blend(int color) {
        int r = (color >> 16 & 0xFF) + 40;
        int g = (color >> 8 & 0xFF) + 40;
        int b = (color & 0xFF) + 40;
        return (color & 0xFF000000) | Math.min(r, 255) << 16 | Math.min(g, 255) << 8 | Math.min(b, 255);
    }
}
