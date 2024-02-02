package com.tjagla.deploy;

import org.bytedeco.javacpp.tensorflow;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.bytedeco.javacpp.tensorflow.GraphDef;
import static org.bytedeco.javacpp.tensorflow.ReadBinaryProto;
import static org.bytedeco.javacpp.tensorflow.Session;
import static org.bytedeco.javacpp.tensorflow.SessionOptions;
import static org.bytedeco.javacpp.tensorflow.Status;
import static org.bytedeco.javacpp.tensorflow.StringArray;
import static org.bytedeco.javacpp.tensorflow.StringTensorPairVector;
import static org.bytedeco.javacpp.tensorflow.StringVector;
import static org.bytedeco.javacpp.tensorflow.Tensor;
import static org.bytedeco.javacpp.tensorflow.TensorShape;
import static org.bytedeco.javacpp.tensorflow.TensorVector;

/**
 * Created by tobias on 10.12.16.
 */
public class NeuralNetwork {
    private static final String[] PARAM_TARGET = new String[]{"save/Const:0"};
    private static final StringVector TARGET_NODE_NAMES = new StringVector("save/restore_all");
    private static final String[] INPUT_NODE = new String[]{"input"};
    private static final StringVector OUTPUT_NODE = new StringVector("output");

    private final int inputImageWidth;
    private final int inputImageHeight;

    private final Session session;

    public NeuralNetwork(String modelName, String checkpointName, int inputImageWidth, int inputImageHeight) {
//        SessionOptions sO = new SessionOptions();
//        tensorflow.ConfigProto cp = new tensorflow.ConfigProto();
//        tensorflow.GPUOptions gpuO = new tensorflow.GPUOptions();
//        gpuO.set_per_process_gpu_memory_fraction(0.7);
//        cp.set_allocated_gpu_options(gpuO);
//        sO.config(cp);
//        this.session = new Session(sO);

        this.session = new Session(new SessionOptions());

        this.inputImageWidth = inputImageWidth;
        this.inputImageHeight = inputImageHeight;

        // restore graph definition
        GraphDef def = new tensorflow.GraphDef();
        ReadBinaryProto(tensorflow.Env.Default(), modelName, def);
        Status s = session.Create(def);
        if (!s.ok()) {
            throw new RuntimeException(s.error_message().getString());
        }

        // restore parameters
        Tensor fn = new Tensor(tensorflow.DT_STRING, new TensorShape(1));
        StringArray a = fn.createStringArray();
        a.position(0).put(checkpointName);
        s = session.Run(new StringTensorPairVector(PARAM_TARGET, new Tensor[]{fn}), new StringVector(), TARGET_NODE_NAMES, new TensorVector());
        if (!s.ok()) {
            throw new RuntimeException(s.error_message().getString());
        }
    }

    /**
     * Predicts the probablity of images for containing a feature or not.
     *
     * @param images array of images to predict
     * @return a array with the probability for the feature.
     */
    public float[] predict(BufferedImage[] images) {
        // set the input data
        Tensor inputs = new Tensor(tensorflow.DT_INT32, new TensorShape(images.length, inputImageWidth, inputImageHeight));
        IntBuffer x = inputs.createBuffer();
        for (BufferedImage image : images) {
            x.put(image.getData().getPixels(0, 0, inputImageWidth, inputImageHeight, new int[inputImageWidth * inputImageHeight]));
        }

        // variable for the output
        TensorVector outputs = new TensorVector();
        outputs.resize(0);
        Status s = session.Run(new StringTensorPairVector(INPUT_NODE, new Tensor[]{inputs}),
                OUTPUT_NODE, new StringVector(), outputs); // prediction
        if (!s.ok()) {
            throw new RuntimeException(s.error_message().getString());
        }
        // this is how you get back the predicted value from outputs
        FloatBuffer output = outputs.get(0).createBuffer();
        float[] results = new float[output.limit()];
        output.get(results);

        return results;
    }
}
