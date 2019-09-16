package xtrememp.visualization.toolbox;

/**
 * Whitener is a normalization method used to enhance the display of spectral
 * lines (usually the harmonics of musical notes) and to reduce the display of
 * noise (usually percussive effects which smear spectral energy over a large range.)
 * The technique used here is to (1) slide a moving window across the spectrum
 * seeking local peaks above some threshold; (2) replace those peaks with the
 * local mean value; (3) compute a moving average of the resultant reduced-peaks
 * spectrum, and (4) subtract this moving average from the initial spectrum.
 * 
 * This technique is called "Two-Pass Split-Window (TPSW) Filtering" and is
 * described in the paper "Evaluation of Threshold-Based Algorithms for Detection 
 * of Spectral Peaks in Audio" by Leonardo Nunes, Paulo Esquef, and Luiz Biscainho.
 * Download from: http://www.acoustics.hut.fi/~esquef/mypapers/nunes_aesbr2007.pdf
 * 
 * The Whitener class contains the private method localSum() which creates an
 * output array where each element is the sum of inputArray[k-M] to inputArray[k+M].
 * It also contains a public method whiten() which uses the localSum() method to
 * perform the TPSW filtering on the desired input array.
 *
 * @author Keith Bromley (adapted from a previous version written in C++ by
 * Bob Dukelow and translated into java by Elliot Ickovich)
 */


public class Whitener
{   private final int     filterHalfWidth;
    // filter half-width; number of bins to include in average each side of center
    private final int     gapHalfWidth; // gap half-width 
    // number of bins to (optionally) skip either side of center (i.e., skip 2*gapHalfWidth+1 bins)
    
    // Note that without the gap there are 2*filterHalfWidth+1 bins averaged to 
    // get the mean at each point (except near the edges). When the gap is used,
    // there are (2*filterHalfWidth+1) - (2*gapHalfWidth+1) bins in each average.

    private final int     binCount;       // number of frequency bins
    private final float[] noPeaksArray;   // intermediate array
    private final float[] sum;            // local sum vector
    private final float[] sumGap;         // local sum vector
    private final int[]   sumCount;       // count of values included in each local sum
    private final int[]   sumGapCount;    // count of values included in each local sum
    private       float   mean;
    private final float   noiseThreshold  = 2.0F;  // determines which peaks are replaced by the mean during the first pass
    private final float   signalThreshold = 30.0F; // determines which peaks are selected in the pickPeaks() method
    private       int     i,j             = 0;
    private       float   tempFloat       = 0.0F;
    
    public Whitener(int binCount)
    {   this.binCount  = binCount;
        filterHalfWidth    = 32;    // so the total filter width is 65 bins
        gapHalfWidth       =  2;    // so the total gap width is 5 bins
        // A Hamming FFT window will smear the main peak over about 5 FFT bins.

        noPeaksArray    = new float [binCount];
        sum             = new float [binCount];
        sumCount        = new int   [binCount];
        sumGap          = new float [binCount];
        sumGapCount     = new int   [binCount];
    }  // end of constructor Whitener()


    private void localSum(  
            float[] inputArray, // input array
            int     M,          // number of bins to include in sum each side of center
            float[] sumArray,   // output sum array
            int[]   countArray) // output array indicating number of input elements summed to form each element of the sumArray
    {   /*
        For input array inputArray[] create an output array sumArray[] where 
        each element of the sumArray[k] is the sum of inputArray[k-M] to 
        inputArray[k+M].  The number of elements in the sum is reduced near the
        edges (i.e., the out-of-range points are treated as zeros).
        We will assume that 2*M+1 is smaller than the binCount.
        */
        int first, last;

        // First do sumArray[0] to get things started
        sumArray[0] =  0.0F;
        last = M;
        for (j = 0; j <= last; j++)
        {   sumArray[0] += inputArray[j];
        }
        countArray[0] = last + 1;
		
        // Then just keep adding on to the sum for the next M sumArray elements
        for (i = 1; i <= last; i++)
        {   sumArray[i] = sumArray[i-1] + inputArray[i+M];
            countArray[i]   = countArray[i-1] + 1;
        }

        // Keep subtracting old and adding new values until window reaches other end.
        first = M + 1;
        last  = binCount - M - 1;
            
        for (i = first; i <= last; i++)
        {   sumArray[i]   = sumArray[i-1] - inputArray[i-1-M] + inputArray[i+M];
            countArray[i] = countArray[i-1];
        }
	
        // then just subtract old values on the left till the end
        first = binCount - M;
        for (i = first; i < binCount; i++)
        {   sumArray[i]   = sumArray[i-1] - inputArray[i-1-M];
            countArray[i] = countArray[i-1] - 1;
        }
    }  // end of private method localSum()

    
    /**
     * The whiten() method performs two-pass split-window filtering on the inputArray[].
     * The steps include: (1) Calculate a local first-pass mean for each bin using
     * a 65-bin window with a 5-bin gap. (2) Compare each bin value against its
     * respective local first-pass mean. Values above a threshold are replaced
     * by the local first-pass mean. (3) Calculate a local second-pass noise mean
     * estimate for each bin of this smoothed data again using a 65-bin window
     * this time with no gap. (4) Subtract this noise mean estimate from each
     * original bin value to whiten the noise.
     * @param inputArray
     * @param meanArray
     * @param outputArray
     * @author Keith Bromley (adapted from a previous version written in C++ by
     * Bob Dukelow and translated into java by Elliot Ickovich)
     */
    public void whiten( float[] inputArray, float[] meanArray, float[] outputArray )
    {   // In the first pass the input array is filtered through a split window.
        // We do this in three steps: first sum over the window, then sum over
        // the gap, and then subtract the gap contribution from the first.
        
        // local sum with no gap:
        // sliding window (width = 2*filterHalfWidth+1) summation of the inputArray
        localSum( inputArray, filterHalfWidth, sum, sumCount );
        
        // local sum of the gap:
        // sliding window (width = 2*gapHalfWidth+1) summation of the inputArray
        localSum( inputArray, gapHalfWidth, sumGap, sumGapCount );
        
        // Compute the noPeaksArray[] by replacing peaks of the inputArray[] with the local mean.
        for (i = 0; i < binCount; i++)
        {   mean = ( sum[i] - sumGap[i] ) / ( sumCount[i] - sumGapCount[i] );
            if (inputArray[i] > noiseThreshold * mean)
            {   noPeaksArray[i] = mean;
            } else
            {   noPeaksArray[i] = inputArray[i];
            }
        }
        // The noPeaksArray[] is the same as the inputArray[] but is free of prominent peaks.
        
        // In the second pass, we peform further smoothing on this noPeaksArray[]
        // by applying a conventional moving summation filter to determine sum[]
        // an array of local sums.
        localSum( noPeaksArray, filterHalfWidth, sum, sumCount );
                
        // Now subtract this local mean from the original inputArray
        for (i = 0; i < binCount; i++)
        {   mean = sum[i] / sumCount[i] ;
            meanArray[i] = mean;
            outputArray[i] = inputArray[i] - mean ;
            if (outputArray[i] < 0.0F) {outputArray[i] = 0.0F;}
        }
        // We have now "whitened" the noise floor and hopefully improved the
        // the visibility of the harmonic peaks above the noise floor.
        
    }  // end of method whiten()

    
    public void pickPeaks(float inputArray[], float outputArray[])
    {   for(int bin = 3; bin < inputArray.length-3; bin++)
        {   tempFloat = inputArray[bin];
            if( (tempFloat > signalThreshold)   &&
                (tempFloat > inputArray[bin-3]) &&
                (tempFloat > inputArray[bin-2]) &&
                (tempFloat > inputArray[bin-1]) &&
                (tempFloat > inputArray[bin+1]) &&
                (tempFloat > inputArray[bin+2]) &&
                (tempFloat > inputArray[bin+3]) )
            {   outputArray[bin] = 50.0F;
            }
            else
            {   outputArray[bin] =  0.1F;
            }
        }
    }  // end of method PickPeaks()

}  // end of class Whitener
