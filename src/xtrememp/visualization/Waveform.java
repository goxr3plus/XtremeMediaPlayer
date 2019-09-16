/**
 * Xtreme Media Player a cross-platform media player.
 * Copyright (C) 2005-2014 Besmir Beqiri
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
package xtrememp.visualization;

import java.awt.Color;
import java.awt.Graphics2D;
import xtrememp.player.dsp.DssContext;

import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DigitalSignalSynchronizer;

/**
 * This visualization shows a representation of the time-waveform of the input
 * audio signal.  The upper half of the waveform shows the right channel of audio
 * and the lower half shows the left channel.  To clarify this, we show them in
 * two different colors.  This is a rather unique representation.  If the left
 * and right channels have distinctly different amplitudes then this waveform
 * will look very asymmetrical.  This is normal.  This representation makes much
 * more efficient usage of screen area than does the more conventional representation
 * in which the upper and lower halves are nearly symmetrical.
 * 
 * @author Besmir Beqiri
 */
public final class Waveform extends AbstractVisualization
{   public static final String NAME = "Waveform";
    private int sampleNum       = 0;
    private int newSampleCount  = 0;
    private final double period = 1.0 / DigitalSignalSynchronizer.DEFAULT_BLOCK_RATE;  // 0.023 sec
    private float gain          = 2.0F;
    private final int colorSize = 2000;
    private int colorIndex      = 0;
        
    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine)
    {   // newSampleCount = sampleRate * period = 44,100 * 0.023 = 1,024 samples
        // Note that java.sound defines frames as what XtremeMP calls samples.
        newSampleCount = (int) (sourceDataLine.getFormat().getFrameRate() * period);
    }

    @Override
    public String getDisplayName()
    {   return NAME;
    }

    @Override
    public void setBackgroundColor(Color backgroundColor)
    {   super.setBackgroundColor(backgroundColor);
        freeImage();
    }

    @Override
    public void setForegroundColor(Color foregroundColor)
    {   super.setForegroundColor(foregroundColor);
        freeImage();
    }

    @Override
    public synchronized void render(DssContext dssContext, Graphics2D g2d, int width, int height)
    {
        // Get audio channels data:
        float[][] audioChannels = dssContext.getAudioData();
        float leftLevel  = 0.0f;
        float rightLevel = 0.0f;
        
        for (sampleNum = 0; sampleNum < newSampleCount; sampleNum++)
        {   leftLevel  -= Math.abs(audioChannels[0][sampleNum]);
            rightLevel += Math.abs(audioChannels[1][sampleNum]);
        }
        
        // Normalizing:
        leftLevel  = gain * leftLevel  / (float) newSampleCount;
        rightLevel = gain * rightLevel / (float) newSampleCount;
        if (leftLevel  > 1.0F) { leftLevel  = 1.0F;}    // Limit left-channel  over-saturation.
        if (rightLevel > 1.0F) { rightLevel = 1.0F;}   // Limit right-channel over-saturation.
            
        // Rendering:
        int widthm1 = width - 1;        // widthm1  = width minus 1
        int halfHeight = height >> 1;   // halfHeight = height * 1/2
        
        colorIndex = (colorIndex == colorSize - 1) ? 0 : colorIndex + 1;
        
        // Clear the previous last line:
        g2d.setColor(backgroundColor);
        g2d.drawLine(widthm1, 0, widthm1, height);
        
        // Draw the center line:
        g2d.setColor(foregroundColor);
        g2d.drawLine(0, halfHeight, widthm1, halfHeight);
        
        // Draw the last line:
        g2d.setColor(Color.getHSBColor(colorIndex / (float) colorSize, 1.0f, 1.0f));
        int tmp1 = Math.round(leftLevel * (float) halfHeight) + halfHeight;
        g2d.drawLine(widthm1, halfHeight, widthm1, tmp1);
        
        g2d.setColor(Color.getHSBColor(1.0F - colorIndex / (float) colorSize, 1.0f, 1.0f));
        int tmp2 = Math.round(rightLevel * (float) halfHeight) + halfHeight;
        g2d.drawLine(widthm1, halfHeight, widthm1, tmp2);
        
        g2d.drawImage(buffImage, -1, 0, null);
        
    }  // end of render() method
    
}  // end of UpDownWaveform class