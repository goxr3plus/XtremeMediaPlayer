/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2011 Besmir Beqiri
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package xtrememp.ui.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 *
 * @author Besmir Beqiri
 */
public class SortOrderIcon implements Icon {

    private static final int DEFAULT_WIDTH = 10;
    private static final int DEFAULT_HEIGHT = 10;
    private Color color;
    private int width;
    private int height;
    private Polygon poly;

    public SortOrderIcon(Color color) {
        this(color, true, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SortOrderIcon(Color color, boolean ascending) {
        this(color, ascending, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public SortOrderIcon(Color color, boolean ascending, int width, int height) {
        this.color = color;
        this.width = width;
        this.height = height;
        initPolygon(ascending);
    }

    private void initPolygon(boolean ascending) {
        poly = new Polygon();
        int halfWidth = width / 2;
        if (ascending) {
            poly.addPoint(0, height);
            poly.addPoint(halfWidth, 0);
            poly.addPoint(width, height);
        } else {
            poly.addPoint(0, 0);
            poly.addPoint(halfWidth, height);
            poly.addPoint(width, 0);
        }
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(color);
        g2d.translate(x, y);
        g2d.fillPolygon(poly);
        g2d.translate(-x, -y);

        g2d.dispose();
    }
}
