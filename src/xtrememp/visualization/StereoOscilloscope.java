/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2014 Besmir Beqiri
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package xtrememp.visualization;

import java.awt.Color;
import java.awt.Graphics2D;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Stereo oscilloscope.
 *
 * @author Besmir Beqiri, Keith Bromley
 */
public class StereoOscilloscope extends AbstractOscilloscope {

    public static final String NAME = "Stereo Oscilloscope";
    private float bandWidth;
    private int x = 0, yLeft = 0, yRight = 0;
    private int xOld = 0, yLeftOld = 0, yRightOld = 0;

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    protected void render(Graphics2D g2d, int width, int height,
            float[] leftChannel, float[] rightChannel) {
        bandWidth = (float) width / (float) newSampleCount;
        int halfHeight = height / 2;
        int quarterHeight = height / 4;
        xOld = 0;
        yLeftOld = 0;

        // Sum the sample values from the left and right audio channels.
        // Draw a line between the x,y coordinates of each new audio sample and
        // those of the previous sample.   x = sample number; y = sample value
        for (int i = 0; i < newSampleCount; i++) {
            x = (int) (i * bandWidth);
            yLeft = halfHeight + (int) (quarterHeight * leftChannel[i]);

            x = min(max(0, x), width);
            yLeft = min(max(0, yLeft), height);

            g2d.setColor(Color.cyan);
            g2d.drawLine(xOld, yLeftOld, x, yLeft);
            yLeftOld = yLeft;

            yRight = halfHeight + (int) (quarterHeight * rightChannel[i]);
            yRight = min(max(0, yRight), height);

            g2d.setColor(Color.magenta);
            g2d.drawLine(xOld, yRightOld, x, yRight);
            xOld = x;
            yRightOld = yRight;
        }
    }
}
