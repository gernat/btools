package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.image;

import java.awt.image.BufferedImage;

/**
 * Created by tobias on 27.09.16.
 */
public interface Operator {
    BufferedImage operate(BufferedImage raw);
}
