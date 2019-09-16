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
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DigitalSignalSynchronizer;
import xtrememp.player.dsp.DssContext;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

/**
 * Shows three hanging water balloons bouncing with the music.
 *
 * @author Keith Bromley, Romain Depoivre
 */
public final class WaterBalloons extends AbstractVisualization {

    public static final String NAME = "Water Balloons";
    private static final double frequency = 2.0; // determined experimentally to get a pleasing effect
    private static final double period = 1.0 / DigitalSignalSynchronizer.DEFAULT_BLOCK_RATE;  // 0.023 sec
    private static final double twoPi = 2.0 * Math.PI;
    private static final int colorSize = 2000;
    private static float prevLeftSum;
    private static float prevRightSum;
    private static int newSampleCount = 0;
    private static int t = 0;
    private static double time = 0.0;

    final static class Balloon {

        //We have no tuple with Java, let's use Point(2D) everywhere...
        private final Point2D.Float refPos = new Point2D.Float(0, 0);
        private final Point2D.Float szRatio = new Point2D.Float(0, 0);
        private final Point2D.Float mvFact = new Point2D.Float(0, 0);
        private final Point position = new Point(0, 0);
        private final int colorIncrement;
        private int colorIndex = 0;
        private int a = 0, b = 0;
        private double xa = 0, ya = 0, x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        private final int[] xPoly = {0, 0, 0};
        private final int[] yPoly = {0, 0, 0};

        public Balloon(float xRefPosition, float yRefPosition,
                float xSizeRatio, float ySizeRatio,
                float xMoveFactor, float yMoveFactor,
                int colorIncrement) {
            this.refPos.x = xRefPosition;
            this.refPos.y = yRefPosition;
            this.mvFact.x = xMoveFactor;
            this.mvFact.y = yMoveFactor;
            this.szRatio.x = xSizeRatio;
            this.szRatio.y = ySizeRatio;
            this.colorIncrement = colorIncrement;
        }

        public void move(float leftSum, float rightSum, double time,
                int width, int height) {
            position.move(//new_position <-- reference_postion + offset
                    (int) (refPos.x * width
                    + 32.0 * rightSum * sin(twoPi * frequency * mvFact.x * time)),
                    (int) (refPos.y * height
                    + 64.0 * leftSum * sin(twoPi * frequency * mvFact.y * time)));
        }

        public void updateColor() {
            colorIndex = (colorIndex == colorSize - 1) ? 0 : colorIndex + colorIncrement;
        }

        public void paint(Graphics2D g2d, int width, int height) {
            g2d.setColor(Color.getHSBColor(((float) colorIndex / colorSize), 1.0f, 1.0f));
            a = (int) (height * szRatio.x);
            b = min((int) (height * szRatio.y), height - position.y);
            g2d.fillOval(position.x - a, position.y - b, 2 * a, 2 * b);

            xa = refPos.x * width - position.x;
            ya = 0 - position.y;
            x1 = (pow(a, 2) * (pow(b, 2) * xa + ya * sqrt(pow(b, 2) * pow(xa, 2) + pow(a, 2) * (-pow(b, 2) + pow(ya, 2)))))
                    / (pow(b, 2) * pow(xa, 2) + pow(a, 2) * pow(ya, 2));
            x2 = (pow(a, 2) * (pow(b, 2) * xa - ya * sqrt(pow(b, 2) * pow(xa, 2) + pow(a, 2) * (-pow(b, 2) + pow(ya, 2)))))
                    / (pow(b, 2) * pow(xa, 2) + pow(a, 2) * pow(ya, 2));
            y1 = (pow(b, 2) * (pow(a, 2) * ya - xa * sqrt(pow(b, 2) * pow(xa, 2) + pow(a, 2) * (-pow(b, 2) + pow(ya, 2)))))
                    / (pow(b, 2) * pow(xa, 2) + pow(a, 2) * pow(ya, 2));
            y2 = (pow(b, 2) * (pow(a, 2) * ya + xa * sqrt(pow(b, 2) * pow(xa, 2) + pow(a, 2) * (-pow(b, 2) + pow(ya, 2)))))
                    / (pow(b, 2) * pow(xa, 2) + pow(a, 2) * pow(ya, 2));

            xPoly[0] = (int) xa + position.x;
            xPoly[1] = (int) x1 + position.x;
            xPoly[2] = (int) x2 + position.x;
            yPoly[0] = 0;
            yPoly[1] = (int) y1 + position.y;
            yPoly[2] = (int) y2 + position.y;
            g2d.fillPolygon(xPoly, yPoly, 3);
        }
    }

    private static final Balloon[] balloons = new Balloon[3];

    static {
        WaterBalloons.balloons[0] = new Balloon(03f / 16, 11f / 16, 1f / 4, 1f / 3, 1.3f, 1.0f, 1);
        WaterBalloons.balloons[1] = new Balloon(01f / 02, 01f / 02, 1f / 6, 1f / 4, 1.7f, 1.4f, 2);
        WaterBalloons.balloons[2] = new Balloon(13f / 16, 05f / 16, 1f / 8, 1f / 6, 2.9f, 1.8f, 3);
    }

    public WaterBalloons() {
    }

    @Override
    public void init(int sampleSize, SourceDataLine sourceDataLine) {
        // newSampleCount = sampleRate * period = 44,100 * 0.023 = 1,024 samples
        // Note that java.sound defines frames as what XtremeMP calls samples.
        newSampleCount = (int) (sourceDataLine.getFormat().getFrameRate() * period);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public void setBackgroundColor(Color backgroundColor) {
        super.setBackgroundColor(backgroundColor);
        freeImage();
    }

    @Override
    public void setForegroundColor(Color foregroundColor) {
        super.setForegroundColor(foregroundColor);
        freeImage();
    }

    @Override
    public synchronized void render(DssContext dssContext, Graphics2D g2d, int width, int height) {
        float leftSum = 0.0f;
        float rightSum = 0.0f;

        float[][] audioChannels = dssContext.getAudioData();

        for (int i = 0; i < newSampleCount; i++) {
            leftSum += Math.abs(audioChannels[0][i]);
            rightSum += Math.abs(audioChannels[1][i]);
        }

        // Apply convenient scaling and limiting:
        leftSum = ((leftSum * 5.0f) / (float) newSampleCount);
        rightSum = ((rightSum * 5.0f) / (float) newSampleCount);

        leftSum = min(1.0f, leftSum);
        rightSum = min(1.0f, rightSum);

        // Allow only a small change from their previous value.
        float allowedChangeFactor = 0.1F;
        leftSum = prevLeftSum + allowedChangeFactor * (leftSum - prevLeftSum);
        rightSum = prevRightSum + allowedChangeFactor * (rightSum - prevRightSum);

        prevLeftSum = leftSum;
        prevRightSum = rightSum;

        time = t * period;
        t++;

        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Balloon b : balloons) {
            b.move(leftSum, rightSum, time, width, height);
            b.updateColor();
            b.paint(g2d, width, height);
        }
    }  // end of render() method

}  // end of WaterBalloons class
