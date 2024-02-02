package edu.illinois.gernat.btools.behavior.egglaying.processing;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.behavior.egglaying.goemetry.Point2d;
import edu.illinois.gernat.btools.behavior.egglaying.goemetry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.io.write.OutputWriter;
import edu.illinois.gernat.btools.behavior.trophallaxis.io.read.ImageSource;

/**
 * Created by tobias on 27.09.16.
 */
public class MyProcessor extends Processor {


    public MyProcessor(ImageSource imageSource, OutputWriter writer) {
        super(imageSource, writer);
    }

    /**
     * Clips and rotates a roi from a hive image.
     *
     * @param hive         big image with many bees.
     * @param subImageDesc describing the roi by the upper left corner and the rotation angle.
     * @return clipped and rotated subimage.
     */
    @Override
    protected BufferedImage clipROI(BufferedImage hive, ROI subImageDesc) {
        // create image for the result
        BufferedImage abdomen = new BufferedImage((int) subImageDesc.width, (int) subImageDesc.height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D background = abdomen.createGraphics();
        background.setColor(BACKGROUND_COLOR);
        background.fillRect(0, 0, abdomen.getWidth(), abdomen.getHeight());
        background.dispose();

        // rotation and clipping
        AffineTransform adjustments = new AffineTransform();
        adjustments.rotate(subImageDesc.rotAngle);
        Point2d imageCoords = subImageDesc.upperLeft.getSwitchedCoordinates();
        adjustments.translate(-imageCoords.x, -imageCoords.y);

        abdomen = new AffineTransformOp(adjustments, AffineTransformOp.TYPE_BILINEAR).filter(hive, abdomen);

        return abdomen;
    }

}
