package edu.illinois.gernat.btools.behavior.egglaying.processing.roi;

import edu.illinois.gernat.btools.behavior.egglaying.geometry.ROI;
import edu.illinois.gernat.btools.behavior.egglaying.io.Bee;

/**
 * Created by tobias on 27.09.16.
 */
public interface ROICalculator {
    ROI calcROI(Bee bee);
}
