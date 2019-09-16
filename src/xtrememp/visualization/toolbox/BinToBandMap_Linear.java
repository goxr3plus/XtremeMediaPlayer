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
package xtrememp.visualization.toolbox;

/**
 * Bin-to-band mapping refers to combining bin data into groups thereby 
 * reducing the number of displayed bands.
 * 
 * The BinToBandMap_Linear class require only three input parameters:
 *      binCount    - the number of input spectral bins,
 *      maxFreq     - the frequency (in Hz) of the top of the top spectral bin,
 *      bandCount   - the desired number of bands for the map's output.
 * 
 * The BinToBandMap_Linear.createTopBinNumArray method builds two arrays:
 *    topBinNum [bandNum] = the number of the top bin in a given band, and
 *    topBinFreq[bandNum] = the frequency at the top of the top bin in a given band.
 * The latter map is used only internally to this class.
 * 
 * Remember that in the FFT.calculateFrequencyTable method, each table element
 * states the end of the frequency range of the corresponding FFT bin.
 * For example:
 *      Range of bin 0 =          0.0 Hz to freqTable[0] Hz
 *      Range of bin 1 = freqTable[0] Hz to freqTable[1] Hz
 *      Range of bin 2 = freqTable[1] Hz to freqTable[2] Hz
 *      ... and so on.
 * 
 * The BinToBandMap_Linear class stipulates that:
 * - The bottom bin in the bottom band is bin 0.
 * - The frequency of the bottom of the bottom bin in the bottom band is 0 Hz.
 * - Both the bins and the bands cover the range from 0 Hz to maxFreq Hz.
 *
 * @see BinToBandMap
 * @see BinToBandMap_Linear
 * @see BinToBandMap_Notes
 *
 * @author Keith Bromley
 */
public class BinToBandMap_Linear implements BinToBandMap
{   private int     bandNum = 0;
    private float[] binFreqTable;
    private int[]   topBinNumArray;
    private float[] topBinFreqArray;
    
    public BinToBandMap_Linear()
    { 
    }

    @Override
    public int[] createTopBinNumArray (int binCount, float maxFreq, int bandCount)
    {   
        float binWidth = maxFreq / (float) binCount; 
        // For example, 5.3833 Hz/bin = 5512.5 Hz / 1024 bins

        // Create a frequency table containing the upper frequency of all bins.
        binFreqTable    = new float[binCount];
        for (int binNum = 0; binNum < binCount; binNum++)
        {   binFreqTable[binNum] = ( binNum + 1 ) * binWidth ;
        }
        // for(int binNum = 0; binNum < binCount; binNum++)
        // {   System.out.printf( "\n binNum = %3d topBinFreq = %7.2f", binNum, binFreqTable[binNum] );
        // }
        
        float bandWidth = maxFreq / (float) bandCount;  
        // For example, 5.3833 Hz/band = 5512.5 Hz / 1024 bands
        
        topBinNumArray  = new int  [bandCount];
        topBinFreqArray = new float[bandCount];
        
        // In general, the frequency boundaries between bins do NOT line up
        // exactly with the frequency boundaries between bands.  We will define
        // the bandTopBin as the bin whose top frequency is just less than (or
        // equal to) the top frequency of the band.

        for (bandNum = 0; bandNum < bandCount; bandNum++)
        {   float topBandFreq = ( bandNum + 1 ) * bandWidth;
        
            for (int binNum = 0; binNum < binCount; binNum++)
            {   float topBinFreq = binFreqTable[binNum];
            
                if(topBinFreq > topBandFreq)
                {   topBinNumArray[bandNum] = binNum - 1; // bandTopBinNum 
                    topBinFreqArray[bandNum] = binFreqTable[ binNum - 1 ]; // bandTopBinFreq
                    break;
                }
            }
        }

        /*
        System.out.printf("\n BinToMap_Linear");
        System.out.printf("\n binCount = %3d bandCount = %3d", binCount, bandCount );
        System.out.printf("\n binWidth = %7.2f bandWidth = %7.2f  maxFreq = %7.2f", binWidth, bandWidth, maxFreq );

        for(bandNum = 0; bandNum < bandCount; bandNum++)
        {   System.out.printf( "\n bandNum = %3d topBandBin = %3d  topBinFreq = %7.2f", bandNum, topBinNumArray[bandNum], topBinFreqArray[bandNum] );
        }

        Example print output:
        binCount = 1024 bandCount =  32
        binWidth =    5.38 bandWidth =  172.27  maxFreq = 5512.50
        
        bandNum =   0 topBandBin =  31  topBinFreq =  172.27
        bandNum =   1 topBandBin =  63  topBinFreq =  344.53
        bandNum =   2 topBandBin =  95  topBinFreq =  516.80
        bandNum =   3 topBandBin = 127  topBinFreq =  689.06
        bandNum =   4 topBandBin = 159  topBinFreq =  861.33
        

        binCount = 4096 bandCount = 972
        binWidth =    5.38 bandWidth =   22.69  maxFreq = 22050.00
        bandNum =   0 topBandBin =   3  topBinFreq =   21.53
        bandNum =   1 topBandBin =   7  topBinFreq =   43.07
        bandNum =   2 topBandBin =  11  topBinFreq =   64.60
        bandNum =   3 topBandBin =  15  topBinFreq =   86.13
        bandNum =   4 topBandBin =  20  topBinFreq =  113.05

        bandNum = 967 topBandBin = 4078  topBinFreq = 21958.48
        bandNum = 968 topBandBin = 4082  topBinFreq = 21980.02
        bandNum = 969 topBandBin = 4086  topBinFreq = 22001.55
        bandNum = 970 topBandBin = 4090  topBinFreq = 22023.08
        bandNum = 971
        */
        return topBinNumArray;
        
    } // end of createTopBinNumArray() method
    
} // end of BinToBandMap_Linear class
