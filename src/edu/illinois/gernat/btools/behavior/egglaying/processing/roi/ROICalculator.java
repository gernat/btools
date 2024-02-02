package com.tjagla.processing.roi;

import com.tjagla.goemetry.ROI;
import com.tjagla.io.Bee;

/**
 * Created by tobias on 27.09.16.
 */
public interface ROICalculator {
    ROI calcROI(Bee bee);
}
