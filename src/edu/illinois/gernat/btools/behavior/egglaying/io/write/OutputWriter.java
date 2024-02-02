package edu.illinois.gernat.btools.behavior.egglaying.io.write;

import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.behavior.egglaying.io.LabeledBee;

/**
 * Created by tobias on 29.09.16.
 */
public interface OutputWriter {
    boolean write(String hiveImageFName, LabeledBee bee, BufferedImage manipulated);

    void close();
}
