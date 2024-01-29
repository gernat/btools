package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.LabeledBee;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.read.ImageSource;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.io.write.TrophallaxisWriter;
import edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi.CenterROI;
import edu.illinois.gernat.btools.common.geometry.Coordinate;
import edu.illinois.gernat.btools.common.geometry.Vector;
import edu.illinois.gernat.btools.common.parameters.Tuple;

/**
 * Created by tobias on 23.01.17.
 */
public class TrophallaxisProcessor extends PairProcessor {
    public static final int LABEl_COLOR = 0x383838;
    public static final float EXPAND = 0.2f; // only use because the calculated corners of the QR-Label are too small (mostly inside of the label)

    public TrophallaxisProcessor(ImageSource imageSource, TrophallaxisWriter writer) {
        super(imageSource, writer);
    }

    protected BufferedImage manipulateHive(BufferedImage hive, Tuple<LabeledBee, LabeledBee> p) {
        LabeledBee beeA = p.a;
        LabeledBee beeB = p.b;

        float[] points1 = beeA.getCorners();
        float[] points2 = beeB.getCorners();

        Polygon p1 = new Polygon();
        Polygon p2 = new Polygon();
        for (int i = 0; i < points1.length; i += 2) {
            Coordinate aLabel = new Coordinate(points1[i], -points1[i + 1]);
            aLabel.translate(new Vector(new Coordinate(beeA.getX(), -beeA.getY()), aLabel), EXPAND);
            p1.addPoint((int) aLabel.x, (int) aLabel.y);

            Coordinate bLabel = new Coordinate(points2[i], -points2[i + 1]);
            bLabel.translate(new Vector(new Coordinate(beeB.getX(), -beeB.getY()), bLabel), EXPAND);
            p2.addPoint((int) bLabel.x, (int) bLabel.y);
        }

        Graphics2D graphics2D = (Graphics2D) hive.getGraphics();
        graphics2D.setColor(new Color(LABEl_COLOR));
        graphics2D.fillPolygon(p1);
        graphics2D.fillPolygon(p2);
        graphics2D.dispose();
        return hive;
    }

    protected BufferedImage clipROI(BufferedImage hive, CenterROI subImageDesc) {
        // create image for the result
        BufferedImage pair = new BufferedImage((int) subImageDesc.width, (int) subImageDesc.height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D background = pair.createGraphics();
        background.setColor(BACKGROUND_COLOR);
        background.fillRect(0, 0, pair.getWidth(), pair.getHeight());
        background.dispose();

        // rotation and clipping
        AffineTransform adjustments = new AffineTransform();
        Coordinate imageCoords = new Coordinate(subImageDesc.center.x, -subImageDesc.center.y);
        adjustments.rotate(subImageDesc.rotAngle, subImageDesc.width / 2.0, subImageDesc.height / 2.0);
        adjustments.translate(-(imageCoords.x - subImageDesc.width / 2.0), -(imageCoords.y - subImageDesc.height / 2.0));

        AffineTransformOp op = new AffineTransformOp(adjustments, AffineTransformOp.TYPE_BILINEAR);
        pair = op.filter(hive, pair);

        if (pair.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            BufferedImage dimg = new BufferedImage(pair.getWidth(), pair.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = dimg.createGraphics();
            g2d.drawImage(pair, 0, 0, null);
            g2d.dispose();
            pair = dimg;
        }

        return pair;
    }
}
