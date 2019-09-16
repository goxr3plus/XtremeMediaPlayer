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
package xtrememp.visualization;

import java.awt.Color;
import java.awt.Graphics2D;
import xtrememp.player.dsp.DssContext;
import xtrememp.visualization.toolbox.BinToBandMap;
import xtrememp.visualization.toolbox.BinToBandMap_Notes;
import org.jtransforms.fft.FloatFFT_1D;
import javax.sound.sampled.SourceDataLine;
import xtrememp.visualization.toolbox.Whitener;

/**
 * Renders a spectrogram displaying 9 octaves of musical notes.  This visualization
 * uses the BinToBandMap_Notes class to distribute the FFT frequency bins to notes
 * of the musical scale.  Our numbering of notes will reference their MIDI note
 * number (midiNum).  
 * MIDI numbers 21 to 116 cover 8 octaves at 12 notes per octave.
 * We will use noteNum = midiNum - 21.
 * So noteNums 0 to 96 (midiNums 21 to 117) cover 8 octaves plus 1 note.
 * 
 * There are three common parameter settings used with this visualization: 
 * In the first, we set:
 *      bandsPerNote    =   1  Hence bandNum = noteNum
 *      bandsPerOctave  =  12
 *      bandCount       = 108
 * In the second, we set:
 *      bandsPerNote    =   5
 *      bandsPerOctave  =  60
 *      bandCount       = 540
 * In the third, we set:
 *      bandsPerNote    =   9
 *      bandsPerOctave  = 108
 *      bandCount       = 972
 * Using the first set distributes the FFT frequency bins to notes of the musical
 * scale.  The resultant spectrogram looks similar to an old-fashioned "piano roll".
 * Using the second set distributes the FFT frequency bins to notes of
 * the musical scale - with 5 displayed bands per note.  It covers the same
 * musical range as using the first set but with 5 times the resolution.
 * Similarly the third set covers the same range but with 9 times the resolution.
 * 
 * @author Keith Bromley
 */
public final class PianoRoll extends AbstractSpectrumAnalyzer
{   public static final String NAME = "PianoRoll";

    public static final float DEFAULT_SPECTROGRAM_GAIN  = 0.0F;    // determined experimentally to give desired screen brightness
    public static final float DEFAULT_SPECTROGRAM_SLOPE = 0.00005F;// determined experimentally to give desired brightness of higher frequency bins 

    private final BinToBandMap binToBandMap;
    private int binCount;   // binCount = total number of spectral bins computed
    private int bandCount;  // bandCount = total number of bands to be displayed
    private int fftWindowLength;
    private float fftSampleRate;
    private float maxFreq;
    private final float gain;
    private final float slope;
    private float bandHeight;
    private final float[] brgb;
    private final float[] frgb;
    private float[][] audioChannels;
    private float[]   channelSamples;
    private float[]   binValues;
    private int[] topBinNumArray;
    private Whitener    whitener;
    private float[]     meanValues;
    private float[]     whiteBinValues;
    private float[]     peakBinValues;

    private static final int octaveCount    =  8;
    private static final int notesPerOctave = 12;
    private static final int bandsPerNote  =   9;   // Useful bandsPerNote values are 1, 5, 9.
    
    public static final BinToBandMap BIN_TO_BAND_MAP_NOTES  = new BinToBandMap_Notes(notesPerOctave, bandsPerNote);
    
    public PianoRoll() 
    {   this.binToBandMap = BIN_TO_BAND_MAP_NOTES;
        this.gain  = DEFAULT_SPECTROGRAM_GAIN;
        this.slope = DEFAULT_SPECTROGRAM_SLOPE;
        brgb = new float[3];
        frgb = new float[3];
    }  // end of PianoRoll constructor

    @Override
    public void init(int blockLength, SourceDataLine sourceDataLine)
    {   this.fftWindowLength = blockLength; // fftWindowLength = 8192
        this.fftSampleRate = sourceDataLine.getFormat().getFrameRate(); // fftSampleRate = 44100
        this.maxFreq =  fftSampleRate / 2.0f;  // maxFreq = 22050 Hz
        this.audioChannels = new float[2][blockLength];
        this.channelSamples = new float[blockLength];

        this.fft = new FloatFFT_1D(fftWindowLength);
        calculateWindowCoefficients(fftWindowLength);
        setBinCount( fftWindowLength / 2 ); // 8192 / 2 = 4096
        setBandCount(octaveCount*notesPerOctave*bandsPerNote + bandsPerNote);  // covers 8 octaves plus 1 note
        // 8*12*1 + 1 = 97 bands or 8*12*5 + 5 = 485 bands or 8*12*9 + 9 = 873
        computeBandTables(); 
    }

    /**
     * Sets the numbers of bins displayed by the Spectrogram.
     * This may be less that the number of bins computed by the FFT
     * @param count Cannot be more than half the "FFT sample size".
     */
    public synchronized void setBinCount(int count)
    {   binCount = count;
        this.whitener       = new Whitener(binCount);
        this.binValues      = new float[binCount];
        this.meanValues     = new float[binCount];
        this.whiteBinValues = new float[binCount];
        this.peakBinValues  = new float[binCount];
    }

    /**
     * Sets the numbers of bands displayed by the Spectrogram.
     * This may be less that the number of bins computed by the FFT
     * @param count Cannot be more than half the "FFT sample size".
     */
    public synchronized void setBandCount(int count)
    {   bandCount = count;
    }

    private void computeBandTables()
    {   topBinNumArray  = new int [bandCount];
        if (bandCount > 0 && fftWindowLength > 0 & fft != null)
        {   topBinNumArray =  binToBandMap.createTopBinNumArray(binCount, maxFreq, bandCount);
        }
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
    {   /*  Each iteration of this method performs the following operations:
            - Average the left and right audio channels to a single monophonic channel,
            - Multiply the block of monophonic audio data by the Hamming window function,
            - Calculate the FFT of this block of windowed monophonic audio samples,
            - Convert the FFT bins to the desired number and range of frequency bands,
            - Scale the resulting array of frequency bands vertically along the display,
            - Apply a variable gain factor along this array if desired, and
            - Draw this new band array in a sliding image of previous band arrays.
        */
                
        // FFT processing:
        audioChannels = dssContext.getAudioData();
        channelSamples = averageChannels(audioChannels);// channelSamples has length blockLength
        applyWindow(fftWindowLength, channelSamples);   // fftWindowLength = blockLength
        binValues = computeFFT(channelSamples);         // binValues has length fftWindowLength/2
        whitener.whiten ( binValues, meanValues, whiteBinValues);   // binValues typically range from 0.0 dB to 96.0 dB
        whitener.pickPeaks ( whiteBinValues, peakBinValues);
        
        bandHeight = (float) height / (float) bandCount;
        
        // Rendering:
        float   y = height;
        int     widthm1 = width - 1;// width minus 1
        int     binNum;             // binNum = bin number
        int     bandNum;            // bandNum = band number
        int     topBinNum;          // topBinNum = bin number of top bin in band
        int     bottomBinNum = 0;   // bottomBinNum = bin number of bottom bin in band
        float   binValue;           // binValue  = value of selected FFT bin
        float   bandValue;          // bandValue = value of selected band (the loudest bin in the band)
        
        // Group up available bands using band distribution table:
        for (bandNum = 0; bandNum < bandCount; bandNum++)
        {
            topBinNum = topBinNumArray[bandNum];
            float tempFloat = 0;
            
            // Find loudest bin in the band. (The band is from bins 'bottomBinNum' to 'topBinNum').
            for (binNum = bottomBinNum; binNum <= topBinNum; binNum++)
            {   binValue = whiteBinValues[binNum];  // binValue = Value of bin number binNum
                if (binValue > tempFloat)
                {   tempFloat = binValue;
                }
            }
            bottomBinNum = topBinNum;

            // Calculate gain using a static gain factor and slope.
            bandValue = tempFloat * ( gain + (slope * bandNum) );
            if (bandValue < 0.0F) { bandValue = 0.0F; } // Limit under-saturation.
            if (bandValue > 1.0F) { bandValue = 1.0F; } // Limit over-saturation.

            // Calculate spectrogram color shifting between foreground and background colors.
            float _bandValue = 1.0F - bandValue;
            backgroundColor.getColorComponents(brgb);
            foregroundColor.getColorComponents(frgb);
            Color color = new Color(frgb[0] * bandValue + brgb[0] * _bandValue,
                                    frgb[1] * bandValue + brgb[1] * _bandValue,
                                    frgb[2] * bandValue + brgb[2] * _bandValue);
            g2d.setColor(color);
            g2d.drawLine(widthm1, Math.round(y), widthm1, Math.round(y - bandHeight));

            // Optionally, draw the peaks spectrum:
            tempFloat = peakBinValues[binNum];  // peakBinValues are either 0.1 or 50.0
            if(tempFloat > 10.0F)
            {   g2d.setColor(Color.red);
                g2d.drawLine(widthm1, Math.round(y), widthm1, Math.round(y - bandHeight));
            }
        
            y -= bandHeight;
        }

        g2d.drawImage(buffImage, -1, 0, null);
        
    }  // end of render() method
    
}  // end of PianoRoll class
