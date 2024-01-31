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

package edu.illinois.gernat.btools.behavior.trophallaxis.io.read;

import java.awt.image.BufferedImage;

/**
 * Created by tobias on 29.09.16.
 */
public interface ImageSource {
    /**
     * @return the last successful loaded image.
     */
    BufferedImage getHiveImage();

    /**
     * Forces the class to load a image with a given filename
     *
     * @param imageFName the filename of the image
     * @return true if the loading was successful
     */
    boolean invoke(String imageFName);
}
