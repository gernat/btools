package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.read.ImageSource;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.write.TrophallaxisWriter;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.image.Operator;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi.CenterROI;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi.PairROI;
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
    public void process(Map<String, ArrayList<Tuple>> bees) {
        for (String imageFName : bees.keySet()) {

            if (!imageSource.invoke(imageFName)) continue;
            BufferedImage hive = imageSource.getHiveImage();

            for (Tuple p : bees.get(imageFName)) {
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
    public BufferedImage processSingle(BufferedImage hive, Tuple p) {
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

    protected abstract BufferedImage manipulateHive(BufferedImage hive, Tuple p);

    public void addManipulator(Operator m) {
        this.imageProcessors.add(m);
    }

    public void setRoiCalculator(PairROI roiCalculator) {
        this.roiCalculator = roiCalculator;
    }

    protected abstract BufferedImage clipROI(BufferedImage hive, CenterROI subImageDesc);


}
