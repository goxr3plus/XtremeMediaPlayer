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
package xtrememp.visualization.toolbox;

/**
 * Interface for mapping bin to bands.
 * Bin-to-band mapping refers to combining bin data into groups thereby 
 * reducing the number of displayed bands.  For example, a traditional 10-band 
 * spectrum analyzer contains only 10 displayed frequency bands sampled from 
 * potentially thousands of frequency bins. In order to distribute the bins into
 * only 10 bands, several different mappings or distributions can be be used 
 * such as linear or logarithmic mappings.
 *
 * @see BinToBandMap
 * @see BinToBandMap_Linear
 * @see BinToBandMap_Notes
 *
 * @author Keith Bromley
 */
public interface BinToBandMap {

    /**
     * @param binCount  - the number of input spectral bins,
     * @param maxFreq   - the frequency (in Hz) of the top of the top spectral bin,
     * @param bandCount - the desired number of bands for the map's output.
     *
     * @return An array containing the number of the top bin in each band.
     */
    int[] createTopBinNumArray (int binCount, float maxFreq, int bandCount);
}
