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

package edu.illinois.gernat.btools.behavior.trophallaxis.processing.image;

import java.awt.image.BufferedImage;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;

/**
 * Created by tobias on 27.09.16.
 *
 * Sets for every pixel in a image, whose value is above the threshold to this value.
 */
public class MyLookUpOp implements Operator {

    private final short[] table;

    public MyLookUpOp(short threshold) {
        table = new short[256];
        for (short i = 0; i < table.length; i++) {
            table[i] = i < threshold ? i : threshold;
        }
    }

    /**
     * Perfroms the winsorizing on a given image. All pixel are maped to the value in the table.
     *
     * @param raw input image
     * @return copy of the input image with mapped pixel values
     */
    public BufferedImage operate(BufferedImage raw) {
        LookupTable lookup = new ShortLookupTable(0, table);
        LookupOp grayValueScaling = new LookupOp(lookup, null);

        BufferedImage darker = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        darker = grayValueScaling.filter(raw, darker);
        return darker;
    }
}
