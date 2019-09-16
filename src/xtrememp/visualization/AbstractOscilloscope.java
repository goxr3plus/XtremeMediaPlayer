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
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DigitalSignalSynchronizer;
import xtrememp.player.dsp.DssContext;

/**
 * Base abstract oscilloscope class.
 *
 * @author Besmir Beqiri, Keith Bromley
 */
public abstract class AbstractOscilloscope extends AbstractVisualization {
    
    protected final double period = 1.0 / DigitalSignalSynchronizer.DEFAULT_BLOCK_RATE;  // 0.023 sec
    protected static int newSampleCount = 0;

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine) {
        newSampleCount = (int) (sourceDataLine.getFormat().getFrameRate() * period);
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
        g2d.fillRect(0, 0, width, height);  // Clears the panel to the background color.

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        float[][] audioChannels = dssContext.getAudioData();
        render(g2d, width, height, audioChannels[0], audioChannels[1]);
    }

    protected abstract void render(Graphics2D g2d, int width, int height,
            float[] leftChannel, float[] rightChannel);
}
