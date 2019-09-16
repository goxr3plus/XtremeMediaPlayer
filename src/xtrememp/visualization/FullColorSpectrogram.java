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

import org.jtransforms.fft.FloatFFT_1D;
import java.awt.Color;
import java.awt.Graphics2D;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DssContext;

/**
 * Renders a spectrogram covering the full frequency range provided by the FFT.
 * The spectral magnitudes are represented using the HSB color space.
 * 
 * Based on the KJ-DSS project by Kris Fudalewski at
 * http://fudcom.com/main/libs/kjdss/.
 *
 * @author Keith Bromley
 */
public final class FullColorSpectrogram extends AbstractSpectrumAnalyzer {

    public static final String NAME = "Full Color Spectrogram";
    public static final float DEFAULT_SPECTROGRAM_GAIN  = 0.02F;   // determined experimentally to give desired screen brightness
    public static final float DEFAULT_SPECTROGRAM_SLOPE = 0.0001F; // determined experimentally to give desired brightness of higher frequency bins

    private int binCount;   // total number of bins to be displayed
    private int fftWindowLength;
    private final float gain;
    private final float slope;
    private float binHeight;
    private float[][] audioChannels;
    private float[]   channelSamples;
    private float[]   windowedChannelSamples;
    private float[]   binValues;
    private final float[] brgb;
    private final float[] frgb;
    private float red = 0.0F, green = 0.0F, blue  = 0.0F, alpha = 0.0F;
        
    public FullColorSpectrogram() {
        this.gain  = DEFAULT_SPECTROGRAM_GAIN;
        this.slope = DEFAULT_SPECTROGRAM_SLOPE;
        brgb = new float[3];
        frgb = new float[3];
    }

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine) {
        this.fftWindowLength = Math.min(blockLength, 2048); // If blockLength=8192 then fftWindowLength=2048
        this.fft = new FloatFFT_1D(fftWindowLength);
        calculateWindowCoefficients(fftWindowLength);
        binCount = fftWindowLength / 2; // 2048/2 = 1024 displayed frequency bins
        this.audioChannels = new float[2][blockLength];
        this.channelSamples = new float[blockLength];
        this.windowedChannelSamples = new float[fftWindowLength];
        this.binValues= new float[binCount];
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
        /*  This method performs the following operations:
         - average the left and right audio channels to a single monophonic channel,
         - multiply the frame of monophonic audio data by the Hamming window function,
         - calculate the FFT of this frame of windowed monophonic audio samples,
         - scale the resulting array of frequency bins vertically along the display, and
         - draw this new bin array in a sliding image of previous bin arrays.
         */

        // FFT processing:
        audioChannels = dssContext.getAudioData();
        channelSamples = averageChannels(audioChannels); // channelSamples has length blockLength
        System.arraycopy(channelSamples, 0, windowedChannelSamples, 0, fftWindowLength); // windowedChannelSamples has length fftWindowLength
        applyWindow(fftWindowLength, windowedChannelSamples);
        binValues = computeFFT(windowedChannelSamples); // binValues typically range from 0.0 dB to 96.0 dB
        
        binHeight = (float) height / (float) binCount;

        // Rendering.
        float y = height;
        int widthm1 = width - 1; // width minus 1
        int binNum;
        float mag; // magnitude of selected FFT bin 

        for (binNum = 0; binNum < binCount; binNum++) {
            
            // First draw a vertical line segment with the background color:
            g2d.setColor(backgroundColor);
            g2d.drawLine(widthm1, Math.round(y), widthm1, Math.round(y - binHeight));
            
            mag = (gain + (slope * binNum)) * binValues[binNum];
            if ( mag < 0.0F ) { mag = 0.0F; }   // Limit under-saturation.
            if ( mag > 1.0F ) { mag = 1.0F; }   // Limit over-saturation.
            /*
            Calculate the color using HSB ( h = hue, s = saturation, b = brightness )
            hue: 0 = red, 60 = yellow, 120 = green, 180 = cyan, 240 = blue, 300 = magenta, 360 = red
            
            If the user has selected a dark background, then we will allow mag
            values < 0.2 to go from background to blue, and mag values > 0.2 to
            range from blue to cyan to green to yellow to orange to red.

            If the user has selected a light background, then we will allow mag
            values < 0.2 to go from background to magenta, and mag vaules > 0.2 to
            range from magenta to blue to cyan to green to yellow to orange to red.
            */
            
            /*
            First, let's cover the case of mag < 0.2
            We want the color to go from background to either blue or magenta.
            We will use the RGB color map and take advantage of the alpha parameter
            to provide a smooth transition from the background.
            */
            if (mag < 0.2F)
            {   mag = 5.0F * mag;
                if (colorSchemeDark) // transition from background to blue
                {   red     = 0.0F; 
                    green   = 0.0F;
                    blue    = 1.0F;
                    alpha   = mag;
                }
                else                // transition from background to magenta
                {   red     = 1.0F; 
                    green   = 0.0F;
                    blue    = 1.0F;
                    alpha   = mag;
                }
                g2d.setColor(new Color( red, green, blue, alpha ));
            }

            // Now, let's cover the case of mag > 0.2
            else
            {   mag = (mag - 0.2F ) * 1.25F;
                if (colorSchemeDark)// transition from blue to cyan to green to yellow to orange to red
                {   // Let's let 240 (blue) represent mag = 0.2 and 0 (red) represent mag = 1.0
                    // Then we should use hue = ( 240 - mag*240 ) / 360
                    g2d.setColor( Color.getHSBColor( ( 240.0F - mag*240.0F ) / 360.0F, 1.0F, 1.0F ) );
                }
                else                // transition from magenta to blue to cyan to green to yellow to orange to red
                {   // Let's let 300 (magenta) represent mag = 0.2 and 0 (red) represent mag = 1.0
                    // Then we should use hue = ( 300 - mag*300 ) / 360
                    g2d.setColor( Color.getHSBColor( ( 300.0F - mag*300.0F ) / 360.0F, 1.0F, 1.0F ) );
                }
            }
            g2d.drawLine(widthm1, Math.round(y), widthm1, Math.round(y - binHeight));
            
            y -= binHeight;
        }

        g2d.drawImage(buffImage, -1, 0, null);
        
    }  // end of render() method
    
}  // end of FullColorSpectrogram class
