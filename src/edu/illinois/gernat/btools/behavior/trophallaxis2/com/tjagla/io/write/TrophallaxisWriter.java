package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.write;

import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.common.parameters.Tuple;


/**
 * Created by tobias on 29.09.16.
 */
public interface TrophallaxisWriter {
    boolean write(String hiveImageFName, Tuple p, BufferedImage manipulated);

    void close();
}
