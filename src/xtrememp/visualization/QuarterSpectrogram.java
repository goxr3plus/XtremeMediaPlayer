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
 * Renders a quarter-spectrogram. The visualization displays only the lowest
 * quarter of the spectral bins computed by the FFT. This allows high-resolution
 * viewing of the frequency bins from 0 Hz to 5,512.5 Hz - where most of the
 * musically-interesting audio energy is.
 *
 * Based on the KJ-DSS project by Kris Fudalewski at
 * http://fudcom.com/main/libs/kjdss/.
 *
 * @author Keith Bromley
 */
public final class QuarterSpectrogram extends AbstractSpectrumAnalyzer {

    public static final String NAME = "Quarter Spectrogram";
    public static final float DEFAULT_SPECTROGRAM_GAIN  = 0.01F;   // determined experimentally to give desired screen brightness
    public static final float DEFAULT_SPECTROGRAM_SLOPE = 0.00001F;// determined experimentally to give desired brightness of higher frequency bins

    private int         binCount;   // total number of bins to be displayed
    private int         fftWindowLength;
    private final float gain;
    private final float slope;
    private boolean     maxOverLine = false;
    private float       binHeight;
    private final float[] brgb;
    private final float[] frgb;
    private float[][]   audioChannels;
    private float[]     channelSamples;
    private float[]     binValues;
    private float[]     displayedBinValues;

    public QuarterSpectrogram() {
        this.gain = DEFAULT_SPECTROGRAM_GAIN;
        this.slope = DEFAULT_SPECTROGRAM_SLOPE;
        brgb = new float[3];
        frgb = new float[3];
    }

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine) {
        this.fftWindowLength = blockLength;  // If blockLength = 8192 then fftWindowLength = 8192
        this.fft = new FloatFFT_1D(fftWindowLength);
        calculateWindowCoefficients(fftWindowLength);
        binCount = fftWindowLength / 8; // 8192/8 = 1024 displayed frequency bins
        this.audioChannels  = new float[2][blockLength];
        this.channelSamples = new float[blockLength];
        this.binValues      = new float[ fftWindowLength / 2 ]; // used to capture output of computeFFT()
        this.displayedBinValues= new float[binCount]; // We only display 1024 of the 4096 binValues computed
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
        channelSamples = averageChannels(audioChannels);// channelSamples has length blockLength
        applyWindow(fftWindowLength, channelSamples);   // fftWindowLength = blockLength = 8192
        binValues = computeFFT(channelSamples);         // binValues has length fftWindowLength/2 = 4096
        System.arraycopy(binValues, 0, displayedBinValues, 0, binCount); // displayedBinValues has length binCount = 1024
        if(maxOverLine == true) {displayedBinValues = applyMaxOverLineNormalization(displayedBinValues);}

        binHeight = (float) height / (float) binCount;

        // Rendering.
        float y = height;
        int widthm1 = width - 1; // width minus 1
        int binNum;
        float binMag; // magnitude of selected FFT bin 

        for (binNum = 0; binNum < binCount; binNum++) {
            binMag = (gain + (slope * binNum)) * binValues[binNum];
            if ( binMag < 0.0F ) { binMag = 0.0F; }   // Limit under-saturation.
            if ( binMag > 1.0F ) { binMag = 1.0F; }   // Limit over-saturation.

            // Calculate spectrogram color shifting between foreground and background colors.
            float _binMag = 1.0F - binMag;
            backgroundColor.getColorComponents(brgb);
            foregroundColor.getColorComponents(frgb);
            Color color = new Color(frgb[0] * binMag + brgb[0] * _binMag,
                    frgb[1] * binMag + brgb[1] * _binMag,
                    frgb[2] * binMag + brgb[2] * _binMag);
            g2d.setColor(color);
            g2d.drawLine(widthm1, Math.round(y), widthm1, Math.round(y - binHeight));

            y -= binHeight;
        }

        g2d.drawImage(buffImage, -1, 0, null);
        
    }  // end of render() method
    
}  // end of QuarterSpectrogram class
