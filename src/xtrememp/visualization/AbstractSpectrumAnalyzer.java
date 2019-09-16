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

/**
 * An abstract class that serves as a base class for all spectrum analyzers.
 *
 * @author Besmir Beqiri and Keith Bromley
 */
public abstract class AbstractSpectrumAnalyzer extends AbstractVisualization {

    protected FloatFFT_1D fft;
    private float[] windowCoefficients;
    private final double oneOverLog10 = 1.0 / Math.log(10);

    public void calculateWindowCoefficients(int windowLength) {
        // The following loop calculates the array coefficients for a Hamming window
        // which will multiply the frame of audio data before the FFT calculation.
        if (windowCoefficients == null || windowCoefficients.length != windowLength) {
            windowCoefficients = new float[windowLength];
            for (int k = 0; k < windowLength; k++) {
                windowCoefficients[k] = (float) (0.54 - 0.46 * Math.cos(2.0 * java.lang.Math.PI * k / windowLength));
            }
        }
    }

    public void applyWindow(int fftWindowLength, float[] audioChannels) {
        // This method multiplies the first fftWindowLength samples of audio
        // data by the Hamming window before performing the FFT calculation.
        float tempFloat;
        for (int i = 0; i < fftWindowLength; i++) {
            tempFloat = audioChannels[i] * windowCoefficients[i];
            audioChannels[i] = tempFloat;
        }
    }

    // This method has been replaced by the succeeding one.  Delete upon concurrence.
    public float[] calculateMagnitudes(float[] binArray) {
        float[] binMagArray = new float[binArray.length / 2];
        for (int k = 0; k < binMagArray.length; k++) {
            binMagArray[k] = (float) (Math.sqrt((binArray[2 * k] * binArray[2 * k]) + (binArray[2 * k + 1] * binArray[2 * k + 1])));
        }
        return binMagArray;
    }

    /**
     * This method merges the real and imaginary parts of the binArray[]
     * produced by the FFT, and allows us to change the scale of the resulting
     * spectral values between (1) amplitude, (2) power, and (3) dB. Ultimately,
     * this will be user-selectable through the GUI.
     *
     * @param scale selects whether to use amplitude, power, or dB.
     * @param binArray array of input spectral bin values from the FFT.
     * @return An array of the resulting spectral values with the selected scale
     * applied to.
     */
    public float[] applySelectedScale(int scale, float[] binArray) {
        float[] scaledBinArray = new float[binArray.length / 2];
        int length = scaledBinArray.length;
        float temp1, temp2;

        switch (scale) {
            case 1: // amplitude
                for (int k = 0; k < length; k++) {
                    temp1 = binArray[2 * k] * binArray[2 * k];
                    temp2 = binArray[2 * k + 1] * binArray[2 * k + 1];
                    scaledBinArray[k] = (float) (Math.sqrt(temp1 + temp2));
                }
                break;
            case 2: // power
                for (int k = 0; k < length; k++) {
                    temp1 = binArray[2 * k] * binArray[2 * k];
                    temp2 = binArray[2 * k + 1] * binArray[2 * k + 1];
                    scaledBinArray[k] = (float) (temp1 + temp2) / 100.0F;
                }
                break;
            case 3: // dB
                for (int k = 0; k < length; k++) {
                    temp1 = binArray[2 * k] * binArray[2 * k];
                    temp2 = binArray[2 * k + 1] * binArray[2 * k + 1];
                    scaledBinArray[k] = (float) (10.0 * (Math.log((1.0 + (double) ((temp1 + temp2) * oneOverLog10)))));
                }
                break;
        }
        return scaledBinArray;
    }

    public float[] computeFFT(float[] binArray) {
        /* Each visualization instantiates a new jTransforms FloatFFT_1D object
         called fft using a parameter "fftWindowLength" to designate the length
         of the FFT operation to be performed. We then call its realForward()
         method inputting an array called binArray which contains a one-dimensional
         array of real-valued input audio samples (of length fftWindowLength).
         This method does in-place operations on this array leaving it as an
         array of real and imaginary components of the output spectrum.  Our
         applySelectedScale() method then merges these real and imaginary
         components into a real-valued array of length fftWindowLength/2.
         */
//        float[] binArray = new float[audioChannels.length];
//        System.arraycopy(audioChannels, 0, binArray, 0, audioChannels.length);
        fft.realForward(binArray);
        return applySelectedScale(3, binArray);  // selects amplitude, power, or dB as the desired scale
    }

    /**
     * This method normalizes the spectral values in the input array to a range
     * from 0.0 to 1.0 using the max-over-line scheme. This normalizes the input
     * array such that the maximum value in each line of spectral values is 1.0.
     * A negative side-effect is that during quiet music passages, the noise
     * background is amplified and shows up distinctly in the visualization.
     *
     * @param binArray - the array of input values
     * @return - an array of output values
     */
    public float[] applyMaxOverLineNormalization(float[] binArray) {
        float[] normalizedBinArray = new float[binArray.length];
        int length = binArray.length;

        // First, let's calculate the maximum value within the binArray.
        float max = 0.0F;
        for (int k = 0; k < length; k++) {
            if (binArray[k] > max) {
                max = binArray[k];
            }
        }
        // Now normalize the data in the binArray using this maximum value.
        float norm = 1.0F / max;
        for (int k = 0; k < length; k++) {
            normalizedBinArray[k] = norm * binArray[k];
            if (normalizedBinArray[k] < 0.0F) {
                normalizedBinArray[k] = 0.0F;
            }  // limit under-saturation
            if (normalizedBinArray[k] > 1.0F) {
                normalizedBinArray[k] = 1.0F;
            }  // limit under-saturation
        }
        return normalizedBinArray;
    }
}
