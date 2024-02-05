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

package edu.illinois.gernat.btools.behavior.egglaying.processing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.egglaying.io.write.OutputWriter;
import edu.illinois.gernat.btools.behavior.egglaying.processing.roi.ROICalculator;
import edu.illinois.gernat.btools.behavior.trophallaxis.io.read.ImageSource;
import edu.illinois.gernat.btools.behavior.trophallaxis.processing.image.Operator;

/**
 * Created by tobias on 27.09.16.
 */
public abstract class Processor {

    static final Color BACKGROUND_COLOR = new Color(127, 127, 127); // default color for image parts outside of the hive images

    private final ImageSource imageSource;
    private final OutputWriter writer;
    private ROICalculator roiCalculator;
    private final ArrayList<Operator> imageProcessors = new ArrayList<Operator>();

    public Processor(ImageSource imageSource, OutputWriter writer) {
        this.imageSource = imageSource;
        this.writer = writer;
    }

    /**
     * Takes a map with image pahts (hive images) and accompany bCode descriptions (bees) and
     * produces for every bCode a small image and pushes it to the output writer.
     *
     * @param bees map from hive image paths to bCode descriptions (bees)
     */
    public void process(Map<String, ArrayList<LabeledBee>> bees) {
        for (String imageFName : bees.keySet()) {

            if (!imageSource.invoke(imageFName)) continue;
            BufferedImage hive = imageSource.getHiveImage();

            for (LabeledBee bee : bees.get(imageFName)) {
                BufferedImage manipulated = processSingle(hive, bee);
                writer.write(imageFName, bee, manipulated);
            }

        }
        writer.close();
    }

    /**
     * Clips a single roi from a hive image and does the manipulations on the small image in order
     * to the added manipulation operations.
     *
     * @param hive great image
     * @param bee  description of the current bCode
     * @return clipped and manipulated small image
     */
    public BufferedImage processSingle(BufferedImage hive, LabeledBee bee) {
        ROI subImageDesc = null;
        if (roiCalculator != null) {
            subImageDesc = roiCalculator.calcROI(bee);
        }

        BufferedImage manipulated = clipROI(hive, subImageDesc);
        for (Operator m : imageProcessors) {
            manipulated = m.operate(manipulated);
        }
        return manipulated;
    }

    public void addManipulator(Operator m) {
        this.imageProcessors.add(m);
    }

    public void setRoiCalculator(ROICalculator roiCalculator) {
        this.roiCalculator = roiCalculator;
    }

    protected abstract BufferedImage clipROI(BufferedImage hive, ROI subImageDesc);


}
