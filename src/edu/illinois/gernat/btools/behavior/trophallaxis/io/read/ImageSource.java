package edu.illinois.gernat.btools.behavior.trophallaxis.io.read;

import java.awt.image.BufferedImage;

/**
 * Created by tobias on 29.09.16.
 */
public interface ImageSource {
    /**
     * @return the last successful loaded image.
     */
    BufferedImage getHiveImage();

    /**
     * Forces the class to load a image with a given filename
     *
     * @param imageFName the filename of the image
     * @return true if the loading was successful
     */
    boolean invoke(String imageFName);
}
