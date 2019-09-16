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
import java.awt.RenderingHints;
import static java.lang.Math.max;
import static java.lang.Math.min;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DigitalSignalSynchronizer;
import xtrememp.player.dsp.DssContext;

/**
 * Early high-end high-fidelity audio systems often had a display called a
 * “stereograph”. It was a cathode-ray tube in which the left analog audio
 * signal controlled the horizontal movement of the electron beam hitting the
 * screen, and the right analog signal controlled the vertical movement. If the
 * recording being played was monophonic then the left and right channels were
 * identical and the display would show a single line at a slope of 45 degrees.
 * That is, as the left signal increased and moved the electron beam to the
 * right, the right signal also increased by exactly the same amount and moved
 * the electron beam upward. If the left and right channels each contained only
 * a single sine wave of differing frequency and phase, then the display would
 * show Lissajous figures. In general, the movement of the display provides
 * information on the frequency and phase of the audio and their stereo
 * relationships.
 */
public final class Stereograph extends AbstractVisualization {

    public static final String NAME = "Stereograph";
    private int newSampleCount = 0;
    private int x = 0, y = 0;
    private int xOld = 0, yOld = 0;
    private final double period = 1.0 / DigitalSignalSynchronizer.DEFAULT_BLOCK_RATE;  // 0.023 sec

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
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);  // Fills the panel with the background color.

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a line to the x,y coordinates of each new audio sample from the
        // x,y coordinates of the previous audio sample
        g2d.setColor(foregroundColor);
        float[][] audioChannels = dssContext.getAudioData();
        for (int i = 0; i < newSampleCount; i++) {
            int w2 = width / 2;
            int h2 = height / 2;
            x = w2 + (int) (w2 * audioChannels[0][i]);
            y = h2 - (int) (h2 * audioChannels[1][i]);
            
            x = min(max(0, x), width);
            y = min(max(0, y), height);
            
            g2d.drawLine(xOld, yOld, x, y);
            xOld = x;
            yOld = y;
        }
    }  // end of render() method

}  // end of Stereograph class
