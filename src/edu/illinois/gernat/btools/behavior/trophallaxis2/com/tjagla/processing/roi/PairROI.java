package edu.illinois.gernat.btools.behavior.trophallaxis2.com.tjagla.processing.roi;

import edu.illinois.gernat.btools.common.parameters.Tuple;

/**
 * Created by tobias on 27.09.16.
 */
public interface PairROI {
    CenterROI calcROI(Tuple p);
}
