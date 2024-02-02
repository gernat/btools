package edu.illinois.gernat.btools.behavior.egglaying.io.write;

import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.behavior.egglaying.io.Bee;

/**
 * Created by tobias on 29.09.16.
 */
public interface OutputWriter {
    boolean write(String hiveImageFName, Bee bee, BufferedImage manipulated);

    void close();
}
