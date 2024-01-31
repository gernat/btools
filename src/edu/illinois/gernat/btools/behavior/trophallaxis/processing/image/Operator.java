package edu.illinois.gernat.btools.behavior.trophallaxis.processing.image;

import java.awt.image.BufferedImage;

/**
 * Created by tobias on 27.09.16.
 */
public interface Operator {
    BufferedImage operate(BufferedImage raw);
}
