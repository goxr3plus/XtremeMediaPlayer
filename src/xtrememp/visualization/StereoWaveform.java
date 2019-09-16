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
import xtrememp.player.dsp.DssContext;

import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DigitalSignalSynchronizer;

/**
 * This visualization shows the time-waveform of the input audio signal. The
 * upper half of the screen shows the waveform of the left channel of audio data
 * and the lower half shows the waveform of the right channel.
 *
 * @author Besmir Beqiri, Keith Bromley
 */
public final class StereoWaveform extends AbstractVisualization {

    public static final String NAME = "Stereo Waveform";
    private int sampleNum = 0;
    private int newSampleCount = 0;
    private final double period = 1.0 / DigitalSignalSynchronizer.DEFAULT_BLOCK_RATE;  // 0.023 sec
    private final float gain = 1.0F;
    private final int colorSize = 2000;
    private int colorIndex = 0;

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine) {   
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
        // Get audio channels data:
        float[][] audioChannels = dssContext.getAudioData();

        float leftPosLevel = 0.0f; // positive peak of left audio channel
        float leftNegLevel = 0.0f; // negative peak of left audio channel
        float rightPosLevel = 0.0f; // positive peak of right audio channel
        float rightNegLevel = 0.0f; // negative peak of left audio channel

        for (sampleNum = 0; sampleNum < newSampleCount; sampleNum++) {
            if (audioChannels[0][sampleNum] > leftPosLevel) {
                leftPosLevel = audioChannels[0][sampleNum];
            }
            if (audioChannels[0][sampleNum] < leftNegLevel) {
                leftNegLevel = audioChannels[0][sampleNum];
            }
            if (audioChannels[1][sampleNum] > rightPosLevel) {
                rightPosLevel = audioChannels[1][sampleNum];
            }
            if (audioChannels[1][sampleNum] < rightNegLevel) {
                rightNegLevel = audioChannels[1][sampleNum];
            }
        }

        // Normalize these levels:
        leftPosLevel = gain * leftPosLevel;
        leftNegLevel = gain * leftNegLevel;
        rightPosLevel = gain * rightPosLevel;
        rightNegLevel = gain * rightNegLevel;

        if (leftPosLevel > 1.0F) {
            leftPosLevel = 1.0F;
        }    // Limit left-channel  over-saturation.
        if (leftNegLevel < -1.0F) {
            leftNegLevel = -1.0F;
        }    // Limit left-channel  over-saturation.
        if (rightPosLevel > 1.0F) {
            rightPosLevel = 1.0F;
        }    // Limit right-channel over-saturation.
        if (rightNegLevel < -1.0F) {
            rightNegLevel = -1.0F;
        }    // Limit right-channel over-saturation.

        // Render the two waveforms:
        int widthm1 = width - 1;            // widthm1  = width minus 1
        int halfHeight = height >> 1;       // halfHeight = height * 1/2
        int quarterHeight = height >> 2;    // quarterHeight = height * 1/4
        int threeQuarterHeight = halfHeight + quarterHeight; // threeQuarterHeight = height * 3/4

        colorIndex = (colorIndex == colorSize - 1) ? 0 : colorIndex + 1;

        // Clear the previous last line:
        g2d.setColor(backgroundColor);
        g2d.drawLine(widthm1, 0, widthm1, height);

        // Draw the two center lines:
        g2d.setColor(foregroundColor);
        g2d.drawLine(0, quarterHeight, widthm1, quarterHeight);
        g2d.drawLine(0, threeQuarterHeight, widthm1, threeQuarterHeight);

        // Draw the left-channel (upper) waveform:
        g2d.setColor(Color.getHSBColor(colorIndex / (float) colorSize, 1.0f, 1.0f));
        int tmpLeftPos = Math.round(leftPosLevel * (float) quarterHeight);
        int tmpLeftNeg = Math.round(leftNegLevel * (float) quarterHeight);
        g2d.drawLine(widthm1, quarterHeight - tmpLeftPos, widthm1, quarterHeight - tmpLeftNeg);

        // Draw right-channel (lower) waveform:
        g2d.setColor(Color.getHSBColor(1.0F - colorIndex / (float) colorSize, 1.0f, 1.0f));
        int tmpRightPos = Math.round(rightPosLevel * (float) quarterHeight);
        int tmpRightNeg = Math.round(rightNegLevel * (float) quarterHeight);
        g2d.drawLine(widthm1, threeQuarterHeight - tmpRightPos, widthm1, threeQuarterHeight - tmpRightNeg);

        g2d.drawImage(buffImage, -1, 0, null);

    } // end of render() method

} // end of StereoWaveform class
