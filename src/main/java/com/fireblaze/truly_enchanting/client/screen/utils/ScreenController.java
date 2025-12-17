package com.fireblaze.truly_enchanting.client.screen.utils;

/**
 * ScreenController - beschreibt eine UI-Region relativ zur Parent-GUI.
 * Erweiterungen:
 * - sichtbar/unsichtbar (visible)
 * - displayName (für Buttons / Beschriftungen)
 * - recalculatePosition(...) um Parent-Koordinaten dynamisch zu aktualisieren
 * - moveToSide(...) um Side zu ändern
 */
public class ScreenController {

    // Parent/Container (können zur Laufzeit neu gesetzt werden)
    private int parentLeft;
    private int parentTop;
    private int parentWidth;
    private int parentHeight;

    // Layout
    private int listWidth;
    private int rowHeight;

    // State
    private ScreenSide side;
    private boolean visible = true;
    private String displayName = null; // optionaler sichtbarer Name

    public ScreenController(
            int parentLeft,
            int parentTop,
            int parentWidth,
            int parentHeight,
            int rowHeight,
            int listWidth,
            ScreenSide side
    ) {
        this.parentLeft = parentLeft;
        this.parentTop = parentTop;
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        this.rowHeight = rowHeight;
        this.listWidth = listWidth;
        this.side = side;
        this.displayName = side.name(); // default-Name
    }

    // -----------------------
    // Basis-Accessors
    // -----------------------
    public ScreenSide getSide() {
        return side;
    }

    /**
     * Setzt die Side (LEFT/RIGHT/TOP). Verwende moveToSide(...) falls du
     * zusätzlich Positions-Neuberechnung wünschst.
     */
    public void setSide(ScreenSide side) {
        this.side = side;
    }

    public int getListX() {
        return switch (side) {
            case LEFT -> parentLeft - listWidth - 10;
            case RIGHT -> parentLeft + parentWidth + 10;
            case TOP -> parentLeft + (parentWidth - listWidth) / 2;
        };
    }

    public int getListY() {
        return switch (side) {
            case LEFT, RIGHT -> parentTop + 4;
            case TOP -> parentTop - 50;
        };
    }

    public int getListHeight() {
        return side == ScreenSide.TOP ? 40 : parentHeight;
    }

    public int getVisibleRows() {
        return parentHeight / rowHeight;
    }

    public int getListWidth() {
        return listWidth;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    // -----------------------
    // Neue, nötige Methoden
    // -----------------------

    /**
     * Sichtbarkeits-Flag: nur sichtbare Controller werden gerendert.
     */
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Liefert einen benutzerfreundlichen Namen, der z.B. auf Buttons angezeigt werden kann.
     */
    public String getDisplayName() {
        return displayName == null ? side.name() : displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Liefert die Höhe des Controllers (für Layout-Entscheidungen).
     * Default = listHeight
     */
    public int getHeight() {
        return getListHeight();
    }

    /**
     * Ermöglicht das Aktualisieren der Parent-Position/Größe, falls sich das
     * übergeordneten GUI verschiebt (z.B. beim Side-Switch).
     *
     * Nach dem Aufruf gelten neue parent-Koordinaten in getListX/Y etc.
     */
    public void recalculatePosition(int newParentLeft, int newParentTop, int newParentWidth, int newParentHeight) {
        this.parentLeft = newParentLeft;
        this.parentTop = newParentTop;
        this.parentWidth = newParentWidth;
        this.parentHeight = newParentHeight;
    }

    /**
     * Convenience-Überladung: nur Parent-Left/Top aktualisieren (typisch: leftPos/topPos neu).
     */
    public void recalculatePosition(int newParentLeft, int newParentTop) {
        this.parentLeft = newParentLeft;
        this.parentTop = newParentTop;
    }

    /**
     * Convenience: verschiebe den Controller auf eine andere Side.
     *
     * Wichtig: Nach einem Side-Wechsel solltest du recalculatePosition(...) aufrufen,
     * falls sich Parent-Koordinaten verändert haben.
     */
    public void moveToSide(ScreenSide newSide) {
        this.side = newSide;
    }

    /**
     * Optional: kleine Hilfsmethode, um zu prüfen, ob ein Punkt innerhalb des Controllers liegt.
     */
    public boolean containsPoint(int x, int y) {
        int lx = getListX();
        int ly = getListY();
        int lh = getListHeight();
        int lw = getListWidth();
        return x >= lx && x < lx + lw && y >= ly && y < ly + lh;
    }

    @Override
    public String toString() {
        return "ScreenController{" +
                "side=" + side +
                ", visible=" + visible +
                ", displayName='" + displayName + '\'' +
                ", listX=" + getListX() +
                ", listY=" + getListY() +
                ", listWidth=" + listWidth +
                ", listHeight=" + getListHeight() +
                '}';
    }
}
