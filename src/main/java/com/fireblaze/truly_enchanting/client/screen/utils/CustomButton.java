package com.fireblaze.truly_enchanting.client.screen.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class CustomButton {

    public int x1, y1;
    public int x2, y2;

    public ScreenSide side; // neu: know which side this button belongs to (nullable)

    private final String labelOff;
    private final String labelOn;

    private final int unpressedColor;
    private final int pressedColor;
    private final int hoverBorderColor;

    private boolean hovered = false;
    private boolean armed = false;
    private boolean toggled = false;
    public boolean visible = true;

    private final Font font;

    public interface ClickHandler {
        void onClick(boolean newState);
    }

    private final ClickHandler clickHandler;

    // alter Konstruktor (ohne side) bleibt kompatibel:
    public CustomButton(int x1, int y1, int x2, int y2,
                        String labelOff, String labelOn,
                        int unpressedColor, int pressedColor, int hoverBorderColor,
                        Font font, ClickHandler clickHandler) {
        this(x1, y1, x2, y2, labelOff, labelOn, unpressedColor, pressedColor, hoverBorderColor, font, clickHandler, null);
    }

    // neuer Konstruktor mit Side
    public CustomButton(int x1, int y1, int x2, int y2,
                        String labelOff, String labelOn,
                        int unpressedColor, int pressedColor, int hoverBorderColor,
                        Font font, ClickHandler clickHandler,
                        ScreenSide side) {

        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);

        this.labelOff = labelOff;
        this.labelOn = labelOn;

        this.unpressedColor = unpressedColor;
        this.pressedColor = pressedColor;
        this.hoverBorderColor = hoverBorderColor;

        this.font = font;
        this.clickHandler = clickHandler;
        this.side = side;
    }

    public void setPosition(int newX, int newY) {
        int w = getWidth();
        int h = getHeight();
        this.x1 = newX;
        this.y1 = newY;
        this.x2 = newX + w;
        this.y2 = newY + h;
    }

    public void render(GuiGraphics gui, int mouseX, int mouseY) {
        hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;

        int bg = toggled ? pressedColor : unpressedColor;
        gui.fill(x1, y1, x2, y2, bg);

        if (hovered) {
            gui.fill(x1, y1, x2, y1 + 1, hoverBorderColor);
            gui.fill(x1, y2 - 1, x2, y2, hoverBorderColor);
            gui.fill(x1, y1, x1 + 1, y2, hoverBorderColor);
            gui.fill(x2 - 1, y1, x2, y2, hoverBorderColor);
        }

        String label = toggled ? labelOn : labelOff;
        int textX = x1 + (getWidth() - font.width(label)) / 2;
        int textY = y1 + (getHeight() - font.lineHeight) / 2;
        gui.drawString(font, label, textX, textY, 0xFFFFFF, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            armed = true;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && armed) {
            armed = false;
            if (isMouseOver(mouseX, mouseY)) {
                toggled = !toggled;
                if (clickHandler != null) clickHandler.onClick(toggled);
                return true;
            }
        }
        return false;
    }

    private boolean isMouseOver(double mx, double my) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    public int getWidth() { return x2 - x1; }
    private int getHeight() { return y2 - y1; }

    public void setToggled(boolean state) { this.toggled = state; }
}
