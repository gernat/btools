package edu.illinois.gernat.btools.behavior.trophallaxis.processing.roi;

import edu.illinois.gernat.btools.behavior.trophallaxis.io.LabeledBee;
import edu.illinois.gernat.btools.common.parameters.Tuple;

/**
 * Created by tobias on 27.09.16.
 */
public interface PairROI {
    CenterROI calcROI(Tuple<LabeledBee, LabeledBee> p);
}
