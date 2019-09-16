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
import java.awt.RenderingHints;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DssContext;
import xtrememp.visualization.toolbox.Whitener;

/**
 * This is a version of the QuarterSpectrum visualization specifically designed
 * to allow direct comparison between the usual spectrum and a whitened spectrum.
 *
 * @author Besmir Beqiri, Keith Bromley
 */
public final class WhiteQuarterSpectrum extends AbstractSpectrumAnalyzer {

    public static final String NAME = "White Quarter Spectrum";

    public static final float DEFAULT_SPECTRUM_GAIN  = 5.000F;
    public static final float DEFAULT_SPECTRUM_SLOPE = 0.005F;
    
    private int counter = 0;
    private int x = 0, y = 0;
    private int xOld = 0, yOld = 0;
    private float binWidth;
    private float binHeight;
    private int binCount;
    private float gain;
    private float slope;
    private int fftWindowLength;
    private Whitener whitener;
    
    private float[][] audioChannels;
    private float[]   channelSamples;
    private float[]   binValues;
    private float[]   displayedBinValues;
    private float[]   meanValues;
    private float[]   whiteBinValues;
    private float[]   peakBinValues;

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine) {
        this.fftWindowLength = blockLength;  // If blockLength = 8192 then fftWindowLength = 8192
        this.fft = new FloatFFT_1D(fftWindowLength);
        calculateWindowCoefficients(fftWindowLength);
        binCount = fftWindowLength / 8; // 8192/8 = 1024 displayed frequency bins
        this.whitener = new Whitener(binCount);
        this.audioChannels = new float[2][blockLength];
        this.channelSamples = new float[blockLength];
        this.binValues= new float[ fftWindowLength / 2 ]; // used to capture output of computeFFT()
        this.displayedBinValues= new float[binCount];     // We only display 1024 of the 4096 binValues computed
        this.meanValues = new float[binCount];
        this.whiteBinValues = new float[binCount];
        this.peakBinValues = new float[binCount];
        this.gain  = DEFAULT_SPECTRUM_GAIN;
        this.slope = DEFAULT_SPECTRUM_SLOPE;
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
    public synchronized void render(DssContext dssContext, Graphics2D g2d, int width, int height)
    {   g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);  // Clears the panel to the background color.

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(foregroundColor);

        // FFT processing.
        audioChannels = dssContext.getAudioData();
        channelSamples = averageChannels(audioChannels);// channelSamples has length blockLength
        applyWindow(fftWindowLength, channelSamples);   // fftWindowLength = blockLength = 8192
        binValues = computeFFT(channelSamples);         // binValues has length fftWindowLength/2 = 4096
        System.arraycopy(binValues, 0, displayedBinValues, 0, binCount); // displayedBinValues has length binCount = 1024
        whitener.whiten ( displayedBinValues, meanValues, whiteBinValues);// binValues typically range from 0.0 dB to 96.0 dB
        whitener.pickPeaks ( whiteBinValues, peakBinValues); // peakBinValues are either 50.0 dB or 1.0 db

        binWidth = (float) width / (float) binCount;
        binHeight = (float) height / 96.0F; // binValues typically range from 0.0 dB to 96.0 dB
        
        // First, draw the whitened spectrum:
        g2d.setColor(Color.orange);
        xOld = 0;
        yOld = 0;
        // Draw a line between the x,y coordinates of each new spectral bin value
        // and those of the previous bin.   x = bin number; y = spectral magnitude
        for (int bin = 0; bin < binCount; bin++)
        {   x = (int) (bin * binWidth);
            y = height - (int) ( (gain + slope*bin) * whiteBinValues[bin] );
            
            if(x < 0)       {x = 0;}
            if(x > width)   {x = width;}
            if(y < 0)       {y = 0;}
            if(y > height)  {y = height;}

            g2d.drawLine(xOld, yOld, x, y);
            xOld = x;
            yOld = y;
        }
        
        // Second, draw the peaks spectrum:
        g2d.setColor(Color.white);
        xOld = 0;
        yOld = 0;
        // Draw a line between the x,y coordinates of each new spectral bin value
        // and those of the previous bin.   x = bin number; y = spectral magnitude
        for (int bin = 0; bin < binCount; bin++)
        {   x = (int) (bin * binWidth);
            y = (int) (binHeight / 2 *  peakBinValues[bin]);  // We want the peaks to show only the top quarter of the screen
            
            if(x < 0)       {x = 0;}
            if(x > width)   {x = width;}
            if(y < 0)       {y = 0;}
            if(y > height)  {y = height;}

            g2d.drawLine(xOld, yOld, x, y);
            xOld = x;
            yOld = y;
        }

    }  // end of render() method

}  // end of WhiteQuarterSpectrum class
