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

package edu.illinois.gernat.btools.behavior.trophallaxis.deploy;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * Created by tobias on 10.12.16.
 */
public class NeuralNetwork {

    private final int inputImageWidth;
    private final int inputImageHeight;

    private final byte[] graphDef;
    public NeuralNetwork(String modelName, int inputImageWidth, int inputImageHeight) {
        this.graphDef = readAllBytesOrExit(Paths.get(modelName));
        this.inputImageHeight = inputImageHeight;
        this.inputImageWidth = inputImageWidth;
    }

    /**
     * Predicts the probablity of images for containing a feature or not.
     *
     * @param images array of images to predict
     * @return a array with the probability for the feature.
     */
    public float[] predict(BufferedImage[] images) {
        // set the input data
        IntBuffer x = IntBuffer.allocate(images.length*inputImageWidth*inputImageHeight);
        for (BufferedImage image : images) {
            x.put(image.getData().getPixels(0, 0, inputImageWidth, inputImageHeight, new int[inputImageWidth * inputImageHeight]));
        }
        x.flip();
        Tensor<Integer> inputs = Tensor.create(new long[]{images.length, inputImageWidth, inputImageHeight},x);

        try (Graph g = new Graph()) {
            g.importGraphDef(this.graphDef);
            try (Session s = new Session(g)) {
            	@SuppressWarnings("unchecked")
                Tensor<Float> results = (Tensor<Float>) s.runner().feed("input",inputs).fetch("output").run().get(0);
                final long[] rshape = results.shape();
                if (results.numDimensions() != 1 || rshape[0] != images.length) {
                    throw new RuntimeException(String.format("Expected model produce a [N] shaped where N is the number of images, instead it produced one with shape %s", Arrays.toString(rshape)));
                }
                float[] res = new float[images.length];
                results.copyTo(res);
                inputs.close();
                results.close();
                return res;
            }
        }

    }

    private static byte[] readAllBytesOrExit(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: ");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}
