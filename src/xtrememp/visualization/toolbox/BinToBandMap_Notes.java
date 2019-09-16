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
 * Bin-to-band mapping refers to combining spectral bin data into groups thereby 
 * reducing the number of displayed bands.
 * 
 * Remember that in the FFT.calculateFrequencyTable method, each table element
 * states the end of the frequency range of the corresponding FFT bin.
 * For example:
 *      Range of bin 0 =          0.0 Hz to freqTable[0] Hz
 *      Range of bin 1 = freqTable[0] Hz to freqTable[1] Hz
 *      Range of bin 2 = freqTable[1] Hz to freqTable[2] Hz
 *      ... and so on.
 * 
 * The BinToBandMap_Notes class distributes the FFT frequency bins to notes of
 * the musical scale.  Our numbering of notes will reference their MIDI note
 * number (midiNum).
 * MIDI numbers 21 to 116 cover 8 octaves at 12 notes per octave.
 * We will use noteNum = midiNum - 21.
 * So noteNums 0 to 96 (midiNums 21 to 117) cover 8 octaves plus 1 note.
 * 
 *      noteNum   0 (midiNote  21) is A1  =    55 Hz (exactly)
 *      noteNum   3 (midiNote  24) is C2  =    65 Hz
 *      noteNum  36 (midiNote  57) is A4  =   440 Hz (exactly)
 *      noteNum  87 (midiNote 108) is C9  =  8372 Hz
 *      noteNum  96 (midiNote 117) is A9  = 14080 Hz (exactly) 
 * 
 * There are three common parameter settings used with this class:
 * In the first, we set:
 *      bandsPerNote    =   1     Hence bandNum = noteNum
 *      bandsPerOctave  =  12
 *      bandCount       =   8*12 + 1
 * In the second, we set:
 *      bandsPerNote    =   5
 *      bandsPerOctave  =  60
 *      bandCount       =   8*60 + 5
 * In the third, we set:
 *      bandsPerNote    =   9
 *      bandsPerOctave  = 108
 *      bandCount       =   8*108 + 9
 * 
 * Using the second set distributes the FFT frequency bins to notes of
 * the musical scale - with 5 displayed bands per note.  It covers the same
 * musical range as using the first set but with 5 times the resolution.
 * Similarly the third set covers the same range but with 9 times the resolution.
 *
 * @see BinToBandMap
 * @see BinToBandMap_Linear
 * @see BinToBandMap_Notes
 *
 * @author Keith Bromley
 */
public class BinToBandMap_Notes implements BinToBandMap
{   private int     notesPerOctave  = 0;
    private int     binNum          = 0;
    private int     bandNum         = 0;
    private int     noteNum         = 0;
    private int     midiNum         = 0;
    private int     bandsPerNote    = 0;
    private int     bandsPerOctave  = 0;
    private float[] binFreqTable;
    private float[] bandFreqTable;
    private int[]   topBinNumArray;
    private float[] topBinFreqArray;
    private double  base            = 0.0;
    private double  base2           = 0.0;
    private double  exponent        = 0.0;

    /**
     * Create a BinToBandMap_Notes instance supplying a notesPerOctave value and
     * a bandsPerNote value. 
     * @param notesPerOctave    - number of notes in an octave (usually 12)
     * @param bandsPerNote      - number of bands representing a note (1, 5, or 9)
     */
    public BinToBandMap_Notes(int notesPerOctave, int bandsPerNote)
    {   this.notesPerOctave = notesPerOctave;
        this.bandsPerNote   = bandsPerNote;
    }
    
    @Override
    public int[] createTopBinNumArray (int binCount, float maxFreq, int bandCount)
    {    float binWidth = maxFreq / (float) binCount; 
        // For example, 5.3833 Hz/bin = 22050 Hz / 4096 bins

        // Create a frequency table containing the upper frequency of all bins.
        binFreqTable    = new float[binCount];
        for (binNum = 0; binNum < binCount; binNum++)
        {   binFreqTable[binNum] = ( binNum + 1 ) * binWidth ;
        }
        
        /*
        Calculate the frequencies of all notes of interest.
        If bandsPerNote = 1 then:
 *      bandNum   0  noteNum   0 (midiNote  21) is A1  =    55 Hz (exactly)
 *      bandNum   3  noteNum   3 (midiNote  24) is C2  =    65 Hz
 *      bandNum  36  noteNum  36 (midiNote  57) is A4  =   440 Hz (exactly)
 *      bandNum  87  noteNum  87 (midiNote 108) is C9  =  8372 Hz
 *      bandNum  96  noteNum  96 (midiNote 117) is A9  = 14080 Hz (exactly) 

        If bandsPerNote = 5 then:
 *      bandNum   2  noteNum   0 (midiNote  21) is A1  =    55 Hz (exactly)
 *      bandNum  17  noteNum   3 (midiNote  24) is C2  =    65 Hz
 *      bandNum 182  noteNum  36 (midiNote  57) is A4  =   440 Hz (exactly)
 *      bandNum 437  noteNum  87 (midiNote 108) is C9  =  8372 Hz
 *      bandNum 482  noteNum  96 (midiNote 117) is A9  = 14080 Hz (exactly) 
        
        If bandsPerNote = 9 then:
 *      bandNum   4  noteNum   0 (midiNote  21) is A1  =    55 Hz (exactly)
 *      bandNum  31  noteNum   3 (midiNote  24) is C2  =    65 Hz
 *      bandNum 328  noteNum  36 (midiNote  57) is A4  =   440 Hz (exactly)
 *      bandNum 787  noteNum  87 (midiNote 108) is C9  =  8372 Hz
 *      bandNum 868  noteNum  96 (midiNote 117) is A9  = 14080 Hz (exactly) 
        */
        bandFreqTable    = new float[bandCount];
        bandsPerOctave  =  bandsPerNote * notesPerOctave;
        base    = Math.pow (2.0 , 1.0 /      bandsPerOctave  );
        base2   = Math.pow (2.0 , 1.0 / (2 * bandsPerOctave) );

        for(bandNum = 0; bandNum < bandCount; bandNum++)
        {   exponent    = bandNum - ( (bandsPerNote-1) / 2 );
            bandFreqTable[bandNum]= 55.0F * (float) Math.pow ( base, exponent );
        }
    
        /*
        for(bandNum = 0; bandNum < bandCount; bandNum++)
        {   noteNum = Math.floorDiv(bandNum , bandsPerNote);
            midiNum = noteNum + 21;
            System.out.printf("\n bandNum = %3d  noteNum = %3d  midiNum = %3d  noteFreq = %7.2f  upperFreq = %7.2f", bandNum, noteNum, midiNum, bandFreqTable[bandNum], bandFreqTable[bandNum]*base2);
        }

        Example Print Out:
        
        If bandsPerNote = 1 then:
        bandNum =   0  noteNum =   0  midiNum =  21  noteFreq =   55.00  upperFreq =   56.61
        bandNum =   1  noteNum =   1  midiNum =  22  noteFreq =   58.27  upperFreq =   59.98
        bandNum =   2  noteNum =   2  midiNum =  23  noteFreq =   61.74  upperFreq =   63.54
        bandNum =   3  noteNum =   3  midiNum =  24  noteFreq =   65.41  upperFreq =   67.32
        bandNum =   4  noteNum =   4  midiNum =  25  noteFreq =   69.30  upperFreq =   71.33
        
        bandNum =  34  noteNum =  34  midiNum =  55  noteFreq =  392.00  upperFreq =  403.48
        bandNum =  35  noteNum =  35  midiNum =  56  noteFreq =  415.30  upperFreq =  427.47
        bandNum =  36  noteNum =  36  midiNum =  57  noteFreq =  440.00  upperFreq =  452.89
        bandNum =  37  noteNum =  37  midiNum =  58  noteFreq =  466.16  upperFreq =  479.82
        bandNum =  38  noteNum =  38  midiNum =  59  noteFreq =  493.88  upperFreq =  508.36
        
        bandNum =  91  noteNum =  91  midiNum = 112  noteFreq = 10548.08  upperFreq = 10857.16
        bandNum =  92  noteNum =  92  midiNum = 113  noteFreq = 11175.30  upperFreq = 11502.77
        bandNum =  93  noteNum =  93  midiNum = 114  noteFreq = 11839.82  upperFreq = 12186.75
        bandNum =  94  noteNum =  94  midiNum = 115  noteFreq = 12543.85  upperFreq = 12911.42
        bandNum =  95  noteNum =  95  midiNum = 116  noteFreq = 13289.75  upperFreq = 13679.17

        If bandsPerNote = 5 then:
        bandNum =   0  noteNum =   0  midiNum =  21  noteFreq =   53.74  upperFreq =   54.06
        bandNum =   1  noteNum =   0  midiNum =  21  noteFreq =   54.37  upperFreq =   54.68
        bandNum =   2  noteNum =   0  midiNum =  21  noteFreq =   55.00  upperFreq =   55.32
        bandNum =   3  noteNum =   0  midiNum =  21  noteFreq =   55.64  upperFreq =   55.96
        bandNum =   4  noteNum =   0  midiNum =  21  noteFreq =   56.29  upperFreq =   56.61
        
        bandNum = 180  noteNum =  36  midiNum =  57  noteFreq =  429.95  upperFreq =  432.44
        bandNum = 181  noteNum =  36  midiNum =  57  noteFreq =  434.95  upperFreq =  437.47
        bandNum = 182  noteNum =  36  midiNum =  57  noteFreq =  440.00  upperFreq =  442.55
        bandNum = 183  noteNum =  36  midiNum =  57  noteFreq =  445.11  upperFreq =  447.69
        bandNum = 184  noteNum =  36  midiNum =  57  noteFreq =  450.28  upperFreq =  452.89
        
        bandNum = 475  noteNum =  95  midiNum = 116  noteFreq = 12986.21  upperFreq = 13061.44
        bandNum = 476  noteNum =  95  midiNum = 116  noteFreq = 13137.10  upperFreq = 13213.21
        bandNum = 477  noteNum =  95  midiNum = 116  noteFreq = 13289.75  upperFreq = 13366.74
        bandNum = 478  noteNum =  95  midiNum = 116  noteFreq = 13444.17  upperFreq = 13522.05
        bandNum = 479  noteNum =  95  midiNum = 116  noteFreq = 13600.38  upperFreq = 13679.17
        
        If bandsPerNote = 9 then:
        bandNum =   0  noteNum =   0  midiNum =  21  noteFreq =   53.61  upperFreq =   53.78
        bandNum =   1  noteNum =   0  midiNum =  21  noteFreq =   53.95  upperFreq =   54.12
        bandNum =   2  noteNum =   0  midiNum =  21  noteFreq =   54.30  upperFreq =   54.47
        bandNum =   3  noteNum =   0  midiNum =  21  noteFreq =   54.65  upperFreq =   54.82
        bandNum =   4  noteNum =   0  midiNum =  21  noteFreq =   55.00  upperFreq =   55.18
        bandNum =   5  noteNum =   0  midiNum =  21  noteFreq =   55.35  upperFreq =   55.53
        bandNum =   6  noteNum =   0  midiNum =  21  noteFreq =   55.71  upperFreq =   55.89
        bandNum =   7  noteNum =   0  midiNum =  21  noteFreq =   56.07  upperFreq =   56.25
        bandNum =   8  noteNum =   0  midiNum =  21  noteFreq =   56.43  upperFreq =   56.61
        
        bandNum = 324  noteNum =  36  midiNum =  57  noteFreq =  428.85  upperFreq =  430.23
        bandNum = 325  noteNum =  36  midiNum =  57  noteFreq =  431.61  upperFreq =  433.00
        bandNum = 326  noteNum =  36  midiNum =  57  noteFreq =  434.39  upperFreq =  435.78
        bandNum = 327  noteNum =  36  midiNum =  57  noteFreq =  437.19  upperFreq =  438.59
        bandNum = 328  noteNum =  36  midiNum =  57  noteFreq =  440.00  upperFreq =  441.41
        bandNum = 329  noteNum =  36  midiNum =  57  noteFreq =  442.83  upperFreq =  444.26
        bandNum = 330  noteNum =  36  midiNum =  57  noteFreq =  445.68  upperFreq =  447.12
        bandNum = 331  noteNum =  36  midiNum =  57  noteFreq =  448.55  upperFreq =  450.00
        bandNum = 332  noteNum =  36  midiNum =  57  noteFreq =  451.44  upperFreq =  452.89
        
        bandNum = 855  noteNum =  95  midiNum = 116  noteFreq = 12952.92  upperFreq = 12994.55
        bandNum = 856  noteNum =  95  midiNum = 116  noteFreq = 13036.32  upperFreq = 13078.22
        bandNum = 857  noteNum =  95  midiNum = 116  noteFreq = 13120.25  upperFreq = 13162.42
        bandNum = 858  noteNum =  95  midiNum = 116  noteFreq = 13204.73  upperFreq = 13247.17
        bandNum = 859  noteNum =  95  midiNum = 116  noteFreq = 13289.75  upperFreq = 13332.47
        bandNum = 860  noteNum =  95  midiNum = 116  noteFreq = 13375.32  upperFreq = 13418.31
        bandNum = 861  noteNum =  95  midiNum = 116  noteFreq = 13461.44  upperFreq = 13504.70
        bandNum = 862  noteNum =  95  midiNum = 116  noteFreq = 13548.11  upperFreq = 13591.66
        bandNum = 863  noteNum =  95  midiNum = 116  noteFreq = 13635.34  upperFreq = 13679.17
        */
        
        topBinNumArray  = new int  [bandCount];
        topBinFreqArray = new float[bandCount];
        
        // In general, the frequency boundaries between bins do NOT line up
        // exactly with the frequency boundaries between bands.  We will define
        // the bandTopBin as the bin whose top frequency is just less than (or
        // equal to) the top frequency of the band.

        // The topBandFreq is the top frequency of the band.  For musical notes,
        // it is the center frequency * TwentyFourthRootOfTwo.
        
        for (bandNum = 0; bandNum < bandCount; bandNum++)
        {   float topBandFreq = bandFreqTable[bandNum] * (float) base2;
        
            for (binNum = 0; binNum < binCount; binNum++)
            {   float topBinFreq = binFreqTable[binNum];
            
                if(topBinFreq > topBandFreq)
                {   topBinNumArray[bandNum] = binNum - 1; // bandTopBinNum 
                    topBinFreqArray[bandNum] = binFreqTable[ binNum - 1 ]; // bandTopBinFreq
                    break;
                }
            }
        }

        /*
        for(bandNum = 0; bandNum < bandCount; bandNum++)
        {   // System.out.printf( "\n noteNum = %3d  noteFreq = %7.2f  topBandBin = %3d  topBinFreq = %7.2f", noteNum, noteFreqTable[noteNum], topBinNumArray[noteNum], topBinFreqArray[noteNum] );
            System.out.printf( "\n bandNum = %3d  bandFreq = %7.2f  topBandBin = %3d  topBinFreq = %7.2f", bandNum, bandFreqTable[bandNum], topBinNumArray[bandNum], topBinFreqArray[bandNum] );
        }
        
        Example print output:
        
        If bandsPerNote = 1 then:
        bandNum =   0  bandFreq =   55.00  topBandBin =   9  topBinFreq =   53.83   <- noteNum = 0   midiNum = 21
        bandNum =   1  bandFreq =   58.27  topBandBin =  10  topBinFreq =   59.22
        bandNum =   2  bandFreq =   61.74  topBandBin =  10  topBinFreq =   59.22   
        bandNum =   3  bandFreq =   65.41  topBandBin =  11  topBinFreq =   64.60
        bandNum =   4  bandFreq =   69.30  topBandBin =  12  topBinFreq =   69.98
        
        bandNum =  34  bandFreq =  392.00  topBandBin =  73  topBinFreq =  398.36
        bandNum =  35  bandFreq =  415.30  topBandBin =  78  topBinFreq =  425.28
        bandNum =  36  bandFreq =  440.00  topBandBin =  83  topBinFreq =  452.20   <- noteNum = 36   midiNum = 57
        bandNum =  37  bandFreq =  466.16  topBandBin =  88  topBinFreq =  479.11
        bandNum =  38  bandFreq =  493.88  topBandBin =  93  topBinFreq =  506.03
        
        bandNum =  91  bandFreq = 10548.08  topBandBin = 2015  topBinFreq = 10852.73
        bandNum =  92  bandFreq = 11175.30  topBandBin = 2135  topBinFreq = 11498.73
        bandNum =  93  bandFreq = 11839.82  topBandBin = 2262  topBinFreq = 12182.41 
        bandNum =  94  bandFreq = 12543.85  topBandBin = 2397  topBinFreq = 12909.16
        bandNum =  95  bandFreq = 13289.75  topBandBin = 2540  topBinFreq = 13678.97    <- noteNum = 95   midiNum = 116

        If bandsPerNote = 5 then:
        bandNum =   0  bandFreq =   53.74  topBandBin =   9  topBinFreq =   53.83
        bandNum =   1  bandFreq =   54.37  topBandBin =   9  topBinFreq =   53.83
        bandNum =   2  bandFreq =   55.00  topBandBin =   9  topBinFreq =   53.83   <- noteNum = 0   midiNum = 21
        bandNum =   3  bandFreq =   55.64  topBandBin =   9  topBinFreq =   53.83
        bandNum =   4  bandFreq =   56.29  topBandBin =   9  topBinFreq =   53.83
        
        bandNum = 180  bandFreq =  429.95  topBandBin =  79  topBinFreq =  430.66
        bandNum = 181  bandFreq =  434.95  topBandBin =  80  topBinFreq =  436.05
        bandNum = 182  bandFreq =  440.00  topBandBin =  81  topBinFreq =  441.43   <- noteNum = 36   midiNum = 57
        bandNum = 183  bandFreq =  445.11  topBandBin =  82  topBinFreq =  446.81
        bandNum = 184  bandFreq =  450.28  topBandBin =  83  topBinFreq =  452.20
        
        bandNum = 475  bandFreq = 12986.21  topBandBin = 2425  topBinFreq = 13059.89
        bandNum = 476  bandFreq = 13137.10  topBandBin = 2453  topBinFreq = 13210.62
        bandNum = 477  bandFreq = 13289.75  topBandBin = 2482  topBinFreq = 13366.74    <- noteNum = 95   midiNum = 116
        bandNum = 478  bandFreq = 13444.17  topBandBin = 2510  topBinFreq = 13517.47
        bandNum = 479  bandFreq = 13600.38  topBandBin = 2540  topBinFreq = 13678.97
        
        If bandsPerNote = 9 then:
        bandNum =   0  bandFreq =   53.61  topBandBin =   8  topBinFreq =   48.45
        bandNum =   1  bandFreq =   53.95  topBandBin =   9  topBinFreq =   53.83
        bandNum =   2  bandFreq =   54.30  topBandBin =   9  topBinFreq =   53.83
        bandNum =   3  bandFreq =   54.65  topBandBin =   9  topBinFreq =   53.83
        bandNum =   4  bandFreq =   55.00  topBandBin =   9  topBinFreq =   53.83   <- noteNum = 0   midiNum = 21
        bandNum =   5  bandFreq =   55.35  topBandBin =   9  topBinFreq =   53.83
        bandNum =   6  bandFreq =   55.71  topBandBin =   9  topBinFreq =   53.83
        bandNum =   7  bandFreq =   56.07  topBandBin =   9  topBinFreq =   53.83
        bandNum =   8  bandFreq =   56.43  topBandBin =   9  topBinFreq =   53.83
        
        bandNum = 324  bandFreq =  428.85  topBandBin =  78  topBinFreq =  425.28
        bandNum = 325  bandFreq =  431.61  topBandBin =  79  topBinFreq =  430.66
        bandNum = 326  bandFreq =  434.39  topBandBin =  79  topBinFreq =  430.66
        bandNum = 327  bandFreq =  437.19  topBandBin =  80  topBinFreq =  436.05
        bandNum = 328  bandFreq =  440.00  topBandBin =  80  topBinFreq =  436.05   <- noteNum = 36   midiNum = 57
        bandNum = 329  bandFreq =  442.83  topBandBin =  81  topBinFreq =  441.43
        bandNum = 330  bandFreq =  445.68  topBandBin =  82  topBinFreq =  446.81
        bandNum = 331  bandFreq =  448.55  topBandBin =  82  topBinFreq =  446.81
        bandNum = 332  bandFreq =  451.44  topBandBin =  83  topBinFreq =  452.20
        
        bandNum = 855  bandFreq = 12952.92  topBandBin = 2412  topBinFreq = 12989.90
        bandNum = 856  bandFreq = 13036.32  topBandBin = 2428  topBinFreq = 13076.04
        bandNum = 857  bandFreq = 13120.25  topBandBin = 2444  topBinFreq = 13162.17
        bandNum = 858  bandFreq = 13204.73  topBandBin = 2459  topBinFreq = 13242.92
        bandNum = 859  bandFreq = 13289.75  topBandBin = 2475  topBinFreq = 13329.05 <- noteNum = 95   midiNum = 116
        bandNum = 860  bandFreq = 13375.32  topBandBin = 2491  topBinFreq = 13415.19
        bandNum = 861  bandFreq = 13461.44  topBandBin = 2507  topBinFreq = 13501.32
        bandNum = 862  bandFreq = 13548.11  topBandBin = 2523  topBinFreq = 13587.45
        bandNum = 863  bandFreq = 13635.34  topBandBin = 2540  topBinFreq = 13678.97
        */
        
        return topBinNumArray;
        
    } // end of createTopBinNumArray() method
    
} // end of BinToBandMap_Notes class
