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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import javax.sound.sampled.SourceDataLine;
import xtrememp.player.dsp.DssContext;

/**
 * Visualization base class.
 *
 * @author Besmir Beqiri
 */
public abstract class AbstractVisualization implements Comparable<AbstractVisualization> {

    protected boolean colorSchemeDark;
    protected Color backgroundColor = Color.black;
    protected Color foregroundColor = Color.white;
    protected BufferedImage buffImage;
    protected Graphics2D buffGraphics;

    /**
     * Returns the display name of <code>this</code> visualization.
     *
     * @return the display name of <code>this</code> visualization.
     */
    public abstract String getDisplayName();

    public void init(int blockLength, SourceDataLine sourceDataLine) {

    }

    /**
     * Defines the rendering method.
     *
     * @param dssContext a DSS context that holds the audio data.
     * @param g2d a Graphics object used for painting.
     * @param width Width of the rendering area.
     * @param height Height of the rendering area.
     */
    public abstract void render(DssContext dssContext, Graphics2D g2d, int width, int height);

    /**
     * @return the true if the current color scheme is dark, else false.
     */
    public boolean isColorSchemeDark() {
        return colorSchemeDark;
    }

    /**
     * @param colorSchemeDark
     */
    public void setColorSchemeDark(boolean colorSchemeDark) {
        this.colorSchemeDark = colorSchemeDark;
    }
    
    /**
     * @return the background color.
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * @return the foreground color.
     */
    public Color getForegroundColor() {
        return foregroundColor;
    }

    /**
     * @param foregroundColor
     */
    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    /**
     * This method is used by visualizations processing a single channel of
     * audio data. It averages the left and right values from each channel to
     * provide the output.  
     * 
     * Returns a float array as the result of merging the audioChannels arrays.
     *
     * @param audioChannels  An array of floats containing the input audio data.
     * @return A float array object.
     */
    public float[] averageChannels(float[][] audioChannels) {
        int length = audioChannels[0].length;       // length = 8192
        int channelCount = audioChannels.length;    // channelCount = 1 or 2
        float[] outputSamples = new float[length];
        
        for (int sampleNum = 0; sampleNum < length; sampleNum++) {
            float sum = 0;  // the sum of the left and right audio samples
            for (int channelNum = 0; channelNum < channelCount; channelNum++) {
                sum += audioChannels[channelNum][sampleNum];
            }
            outputSamples[sampleNum] = sum / (float) channelCount;
        }
  
        return outputSamples;
    }

    public BufferedImage getBuffImage() {
        return buffImage;
    }

    public Graphics2D getBuffGraphics() {
        return buffGraphics;
    }

    public void checkBuffImage(GraphicsConfiguration gc, int width, int height) {
        if (buffImage == null || (buffImage.getWidth() != width || buffImage.getHeight() != height)) {
            // Free image resources.
            freeImage();

            // Create image.
            buffImage = gc.createCompatibleImage(width, height);

            buffGraphics = buffImage.createGraphics();
            buffGraphics.setColor(backgroundColor);
            buffGraphics.fillRect(0, 0, buffImage.getWidth(), buffImage.getHeight());
        }
    }

    public void freeImage() {
        if (buffGraphics != null) {
            buffGraphics.dispose();
            buffGraphics = null;
        }
        if (buffImage != null) {
            buffImage.flush();
            buffImage = null;
        }
    }

    @Override
    public int compareTo(AbstractVisualization vis) {
        return this.getDisplayName().compareTo(vis.getDisplayName());
    }
}
