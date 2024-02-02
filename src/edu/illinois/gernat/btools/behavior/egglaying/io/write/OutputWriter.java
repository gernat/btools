package com.tjagla.io.write;

import com.tjagla.io.Bee;

import java.awt.image.BufferedImage;

/**
 * Created by tobias on 29.09.16.
 */
public interface OutputWriter {
    boolean write(String hiveImageFName, Bee bee, BufferedImage manipulated);

    void close();
}
