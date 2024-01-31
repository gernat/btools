/*
 * Copyright (C) 2016, 2024 University of Illinois Board of Trustees.
 *
 * This file is part of bTools.
 *
 * bTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * bTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bTools. If not, see http://www.gnu.org/licenses/.
 */

package edu.illinois.gernat.btools.behavior.trophallaxis.processing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

import edu.illinois.gernat.btools.behavior.trophallaxis.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.trophallaxis.io.read.ImageSource;
import edu.illinois.gernat.btools.behavior.trophallaxis.io.write.TrophallaxisWriter;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.image.Operator;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.roi.CenterROI;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.roi.PairROI;
import edu.illinois.gernat.btools.common.parameters.Tuple;

/**
 * Created by tobias on 27.09.16.
 */
public abstract class PairProcessor {

    static final Color BACKGROUND_COLOR = new Color(127, 127, 127); // default color for image parts outside of the hive images

    private final ImageSource imageSource;
    private final TrophallaxisWriter writer;
    private PairROI roiCalculator;
    private final ArrayList<Operator> imageProcessors = new ArrayList<Operator>();

    public PairProcessor(ImageSource imageSource, TrophallaxisWriter writer) {
        this.imageSource = imageSource;
        this.writer = writer;
    }

    /**
     * Takes a map with image pahts (hive images) and accompany bCode descriptions (bees) and
     * produces for every bCode a small image and pushes it to the output writer.
     *
     * @param bees map from hive image paths to bCode descriptions (bees)
     */
    public void process(Map<String, ArrayList<Tuple<LabeledBee, LabeledBee>>> bees) {
        for (String imageFName : bees.keySet()) {

            if (!imageSource.invoke(imageFName)) continue;
            BufferedImage hive = imageSource.getHiveImage();

            for (Tuple<LabeledBee, LabeledBee> p : bees.get(imageFName)) {
                BufferedImage manipulated = processSingle(hive, p);
                writer.write(imageFName, p, manipulated);
            }

        }
        writer.close();
    }

    /**
     * Clips a single roi from a hive image and does the manipulations on the small image in order
     * to the added manipulation operations.
     *
     * @param hive great image
     * @param p    description of the current pair of bees
     * @return clipped and manipulated small image
     */
    public BufferedImage processSingle(BufferedImage hive, Tuple<LabeledBee, LabeledBee> p) {
        CenterROI subImageDesc = null;
        if (roiCalculator != null) {
            subImageDesc = roiCalculator.calcROI(p);
        }

        hive = manipulateHive(hive, p);

        BufferedImage manipulated = clipROI(hive, subImageDesc);
        if (manipulated == null) {
            return null;
        }
        for (Operator m : imageProcessors) {
            manipulated = m.operate(manipulated);
        }
        return manipulated;
    }

    protected abstract BufferedImage manipulateHive(BufferedImage hive, Tuple<LabeledBee, LabeledBee> p);

    public void addManipulator(Operator m) {
        this.imageProcessors.add(m);
    }

    public void setRoiCalculator(PairROI roiCalculator) {
        this.roiCalculator = roiCalculator;
    }

    protected abstract BufferedImage clipROI(BufferedImage hive, CenterROI subImageDesc);

}
