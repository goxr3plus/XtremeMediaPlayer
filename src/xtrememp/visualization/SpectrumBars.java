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
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.Point;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DssContext;

/**
 * Renders a spectrum analyzer.
 *
 * Based on the KJ-DSS project by Kris Fudalewski at
 * http://fudcom.com/main/libs/kjdss/.
 *
 * @author Besmir Beqiri
 */
public final class SpectrumBars extends AbstractSpectrumAnalyzer {

    public static final String NAME = "Spectrum Bars";
    
    public static final int DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT = 32;
    public static final int DEFAULT_SPECTRUM_ANALYSER_PEAK_DELAY = 25;
    public static final float DEFAULT_SPECTRUM_ANALYSER_DECAY = 0.02F;
    public static final float DEFAULT_SPECTRUM_ANALYSER_GAIN = 0.001F;
    public static final float DEFAULT_FLAT_BIN_GAIN = 2.0F;
    public static final float DEFAULT_LINEAR_BIN_GAIN = 2.0F;
    
    private enum bandDistribution { linear, log };
    private enum binGain { flat, linear };
    
    private bandDistribution bD;
    private binGain bG;
    
    private Band[] bandTable;
    private float[] binGainTable;
    private int bandCount;
    private int fftWindowLength;
    private float fftSampleRate;
    private final float decay;
    private final float gain;
    private final float flatBinGain;
    private final float linearBinGain;
    private final int[] peaks;
    private final int[] peaksDelay;
    private final int peakDelay;
    private final boolean peaksEnabled = true;
    private float bandWidth;
    private final boolean showFrequencies = true;
    private float[] oldBinMagArray;
    private LinearGradientPaint lgp;
    private final Map desktopHints;
    private final Font freqFont;

    public SpectrumBars() {
        this.bD = bandDistribution.log;
        this.bG = binGain.linear;
        this.decay = DEFAULT_SPECTRUM_ANALYSER_DECAY;
        this.gain = DEFAULT_SPECTRUM_ANALYSER_GAIN;
        this.flatBinGain = DEFAULT_FLAT_BIN_GAIN;
        this.linearBinGain = DEFAULT_LINEAR_BIN_GAIN;
        this.peaks = new int[DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT];
        this.peaksDelay = new int[DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT];
        this.peakDelay = DEFAULT_SPECTRUM_ANALYSER_PEAK_DELAY;

        Toolkit tk = Toolkit.getDefaultToolkit();
        this.desktopHints = (Map) (tk.getDesktopProperty("awt.font.desktophints"));
        this.freqFont = new Font("Arial", Font.PLAIN, 10);

        setBandCount(DEFAULT_SPECTRUM_ANALYSER_BAND_COUNT);
    }

    public void init(int blockLength, SourceDataLine sourceDataLine) {
        this.fftWindowLength = Math.min(blockLength, 2048); // If blockLength=8192 then fftWindowLength=2048
        this.fftSampleRate = sourceDataLine.getFormat().getFrameRate();
        this.fft = new FloatFFT_1D(fftWindowLength);
        this.oldBinMagArray = new float[bandCount];
        calculateWindowCoefficients(fftWindowLength);
        computeBandTables();
    }
    
    /**
    * Sets the numbers of bands rendered by the spectrum analyzer.
    *
    * @param count Cannot be more than half the "FFT window length".
    */
    public synchronized void setBandCount(int count) {
        bandCount = count;
    }
    
    /**
    * Calculates a table of frequencies represented by the spectral bin values
    * returned from the FFT processing. Each element states the end of the
    * frequency range of the corresponding FFT bin. For example:
    * Range of bin 0 =          0.0 Hz to freqTable[0] Hz
    * Range of bin 1 = freqTable[0] Hz to freqTable[1] Hz
    * Range of bin 2 = freqTable[1] Hz to freqTable[2] Hz
    *   ... and so on.
    *
    * @param spectrumLength
    * @param sampleRate The audio sample rate used to calculate the frequency
    * table (usually the sample rate of the input to the FFT calculate method).
    * Typically sampleRate = 44,100 samples per second.
    * @return An array of frequency limits for each band.
    */
    public static float[] calculateFrequencyTable(int spectrumLength, float sampleRate) {
        float maxFreq = sampleRate / 2.0f;
        float binWidth = maxFreq / spectrumLength;
        float[] freqTable = new float[(int) spectrumLength];

        // Build a table listing the frequency of each spectral bin.
        int bin = 0;
        for (float binFreq = binWidth; binFreq <= maxFreq; binFreq += binWidth) {
            freqTable[bin] = binFreq;
            bin++;
        }
        return freqTable;
    }

    private void computeBandTables() {
        if (bandCount > 0 && fftWindowLength > 0 & fft != null) {
            int binCount = fftWindowLength >> 1;
            
            // Create a band distribution table.
            if (bD == bandDistribution.linear) {bandTable = createLinearBandDistribution(bandCount, binCount, fftSampleRate);}
            if (bD == bandDistribution.log   ) {bandTable = createLogBandDistribution(   bandCount, binCount, fftSampleRate);}
            bandCount = bandTable.length;
            resolveBandDescriptions(bandTable); // Resolve the band descriptions.
            
            // Create a bin gain table.
            if (bG == binGain.flat  ) {binGainTable = createFlatBinGainTable(  binCount, fftSampleRate);}
            if (bG == binGain.linear) {binGainTable = createLinearBinGainTable(binCount, fftSampleRate);}
        }
    }

    public Band[] createLinearBandDistribution(int bandCount, int binCount, float sampleRate) {
        int binsPerBand = (int) ((double) binCount / (double) bandCount);

        float[] binFreqTable = calculateFrequencyTable(binCount, sampleRate); // creates a table listing the frequency of each bin
        
        float startFreq = 0.0f;
        bandTable = new Band[bandCount];
        int band = 0;

        for (double topBandBin = binsPerBand; topBandBin <= binCount && band < bandCount; topBandBin += binsPerBand) {
            bandTable[band] = new Band( (int) topBandBin, startFreq, binFreqTable[(int) topBandBin - binsPerBand]);
            startFreq = binFreqTable[(int) topBandBin - binsPerBand];
            band++;
        }
        return bandTable;
    }

    public Band[] createLogBandDistribution(int bandCount, int binCount, float sampleRate) 
    {   final int       sso = 2;        // logScaleOffset
        final double    lso = 20.0D;    // subSonicOffset

        // Check the output size from the FFT instance to build the band table.
        int hss = binCount - sso;

        double o = Math.log(lso);
        double r = (double) (bandCount - 1) / (Math.log(hss + lso) - o);

        float[] binFreqTable = calculateFrequencyTable(binCount, sampleRate); // creates a table listing the frequency of each bin
        float lfq = binFreqTable[sso];
        int lcb = 1;

        List<Band> bands = new ArrayList<>();
        // Subsonic bands group.
        bands.add(new Band(sso, 0, lfq));

        // Divide rest of bands using log.
        for (int b = 0; b < hss; b++) {
            // Calculate current band.
            double cb = ((Math.log((double) b + lso) - o) * r) + 1.0D;
            if (Math.round(cb) != lcb) {
                bands.add(new Band(b + sso, lfq, binFreqTable[b + sso]));
                lfq = binFreqTable[b + sso];
                lcb = (int) Math.round(cb);
            }
        }

        // Fill in the last entry if necessary.
        if (bands.size() < bandCount) {
            bands.add(new Band((hss - 1) + sso, lfq, binFreqTable[(hss - 1) + sso]));
        }

        return bands.toArray(new Band[bands.size()]);
    }

    private void resolveBandDescriptions(Band[] bandTable) {
        DecimalFormat df = new DecimalFormat("###.#");

        for (Band band : bandTable) {
            if (band.frequency >= 1000.0F) {
                band.description = df.format(band.frequency / 1000.0F) + "k";
            } else {
                band.description = df.format(band.frequency);
            }
        }
    }

   public float[] createFlatBinGainTable(int binCount, float sampleRate) {
        binGainTable = new float[binCount];
        for (int i = 0; i < binCount; i++) {
            binGainTable[i] = flatBinGain;
        }
        return binGainTable;
    }
    
    public float[] createLinearBinGainTable(int binCount, float sampleRate) {
        // Create a frequency table.
        float[] fqt = calculateFrequencyTable(binCount, sampleRate);
        binGainTable = new float[binCount];
        for (int i = 0; i < binCount; i++) {
            binGainTable[i] = (((fqt[i] / linearBinGain) + 512.0f) / 512.0f) * (linearBinGain * 1.5f);
        }
        return binGainTable;
    }
    
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public synchronized void render(DssContext dssContext, Graphics2D g2d, int width, int height) {
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);

        float c = 0;
        int binNum, bandNum, topBinNum;
        int bottomBinNum = 0;
        int tempInt;
        float tempFloat;
        float bandMag;

        int bm = 1;
        // Preparation used for rendering band frequencies.
        if (showFrequencies) {
            g2d.setRenderingHints(desktopHints);
            g2d.setFont(freqFont);
            bm = Math.round(32.0F / bandWidth);
            if (bm == 0) {
                bm = 1;
            }
        }
        // FFT processing.
        float[][] audioChannels = dssContext.getAudioData();
        float[] channelSamples = averageChannels(audioChannels);
        applyWindow(fftWindowLength, channelSamples);
        float[] binValues = computeFFT(channelSamples);
        bandWidth = (float) width / (float) bandCount;

        // Group up available bands using band distribution table.
        for (bandNum = 0; bandNum < bandCount; bandNum++) {
            //Get band distribution entry.
            topBinNum = bandTable[bandNum].distribution;
            tempFloat = 0;
            tempInt = 0;
            // Find loudest band in group. (Group is from 'li' to 'i').
            for (binNum = bottomBinNum; binNum < topBinNum; binNum++) {
                float binMag = binValues[binNum];
                if (binMag > tempFloat) {
                    tempFloat = binMag;
                    tempInt = binNum;
                }
            }
            bottomBinNum = topBinNum;
            // Calculate gain using log, then static gain.
            bandMag = (tempFloat * binGainTable[tempInt]) * gain;
            // Limit over-saturation.
            if (bandMag > 1.0F) {
                bandMag = 1.0F;
            }
            // Compute decay.
            if (bandMag >= (oldBinMagArray[bandNum] - decay)) {
                oldBinMagArray[bandNum] = bandMag;
            } else {
                oldBinMagArray[bandNum] -= decay;
                if (oldBinMagArray[bandNum] < 0) {
                    oldBinMagArray[bandNum] = 0;
                }
                bandMag = oldBinMagArray[bandNum];
            }

            if (lgp == null || lgp.getEndPoint().getY() != height) {
                Point start = new Point(0, 0);
                Point end = new Point(0, height);
                float[] dist = {0.0F, 0.25F, 0.75F, 1.0F};
                Color[] colors = {Color.red, Color.yellow, Color.green, Color.green.darker().darker()};
                lgp = new LinearGradientPaint(start, end, dist, colors, CycleMethod.REPEAT);
            }

            g2d.setPaint(lgp);
            renderSpectrumBar(g2d, Math.round(c), height - 18, Math.round(bandWidth) - 1,
                    Math.round(bandMag * (height - 20)), bandNum, bandTable[bandNum], showFrequencies && (bandNum % bm) == 0);
            c += bandWidth;
        }
    }

    private void renderSpectrumBar(Graphics2D g2d, int x, int y, int w, int h, int bd, Band band, boolean renderFrequency) {
        // Render spectrum bars.
        g2d.fillRect(x, y - h, w, h);
        // Render peaks.
        if ((peaksEnabled == true)) {
            g2d.setColor(foregroundColor);
            if (h > peaks[bd]) {
                peaks[bd] = h;
                peaksDelay[bd] = peakDelay;
            } else {
                peaksDelay[bd]--;
                if (peaksDelay[bd] < 0) {
                    peaks[bd]--;
                }
                if (peaks[bd] < 0) {
                    peaks[bd] = 0;
                }
            }
            g2d.fillRect(x, y - peaks[bd], w, 1);
        }
        // Render frequency string.
        if (renderFrequency) {
            g2d.setColor(foregroundColor);
            int sx = x + ((w - g2d.getFontMetrics().stringWidth(band.description)) >> 1);
            // g2d.drawLine(x + (w >> 1), y, x + (w >> 1), y - (g2d.getFontMetrics().getHeight() - g2d.getFontMetrics().getAscent()));
            g2d.drawString(band.description, sx, y + g2d.getFontMetrics().getHeight());
        }
    }
    
    private class Band {
        private int distribution;
        private float frequency;
        private float startFrequency;
        private float endFrequency;
        private String description;

        private Band(int distribution, float startFrequency, float endFrequency) {
            this.distribution = distribution;
            this.startFrequency = startFrequency;
            this.endFrequency = endFrequency;
            this.frequency = startFrequency + ((endFrequency - startFrequency) / 2.0f);
        }  // end of Band() constructor
        
    }  // end of private Band class
    
}  // end of SpectrumBars class