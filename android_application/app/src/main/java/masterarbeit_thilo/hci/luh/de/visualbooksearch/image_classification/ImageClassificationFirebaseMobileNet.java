package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_classification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModelSource;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core.ClassificationListener;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.ImageUtils;

// ImageClassification mithilfe von Firebase und dem mobilenet_v1,
// welche allerdings schlechtere Ergebnisse lieferte und deshalb nicht genutzt wurde
public class ImageClassificationFirebaseMobileNet {

    private static final String TAG = "ImageClassification FirebaseMobileNet";
    private static final boolean SAVE_DEBUG_IMAGE = false;
    private static final String MODEL_NAME = "local_mobilenet_v1";
    private static final String MODEL_FILE = "mobilenet_v1_1.0_224.tflite";
    private static final String LABEL_FILE = "labels_mobilenet_quant_v1_224.txt";
    private static final int INPUT_SIZE = 224;
    private static final int OUTPUT_SIZE = 1001;
    private static final String SEARCHED_LABEL = "bookcase";
    private static final float PROBABILITY_MINIMUM = 0.6f;

    private static FirebaseModelInterpreter firebaseInterpreter;
    private static FirebaseModelInputOutputOptions inputOutputOptions;
    private static int SEARCHED_LABEL_INDEX = 0;
    private static int debugCounter;

    public ImageClassificationFirebaseMobileNet(Context context) {
        debugCounter = 0;
        FirebaseLocalModelSource localSource =
                new FirebaseLocalModelSource.Builder(MODEL_NAME)
                        .setAssetFilePath(MODEL_FILE)
                        .build();
        FirebaseModelManager.getInstance().registerLocalModelSource(localSource);
        FirebaseModelOptions options = new FirebaseModelOptions.Builder()
                .setLocalModelName(MODEL_NAME)
                .build();
        try {
            firebaseInterpreter = FirebaseModelInterpreter.getInstance(options);
            inputOutputOptions = new FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, INPUT_SIZE, INPUT_SIZE, 3})
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, OUTPUT_SIZE})
                    .build();
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(LABEL_FILE)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(SEARCHED_LABEL)) break;
                SEARCHED_LABEL_INDEX++;
            }
        } catch (FirebaseMLException | IOException e) {
            e.printStackTrace();
        }
    }

    public ImageClassificationTask startClassification(Image image, int rotation, ClassificationListener classificationListener) {
        ImageClassificationTask classificationTask = new ImageClassificationTask(classificationListener, rotation);
        classificationTask.execute(image);
        return classificationTask;
    }

    private static class ImageClassificationTask extends AsyncTask<Image, Void, Void> {

        private ClassificationListener classificationListener;
        private int rotation;

        ImageClassificationTask(ClassificationListener classificationListener, int rotation) {
            this.classificationListener = classificationListener;
            this.rotation = rotation;
        }

        @Override
        protected Void doInBackground(Image... image) {
            Bitmap bitmap = ImageUtils.getResizedRotatedBitmap(
                    ImageUtils.YUV_420_888toBitmap(image[0]),
                    INPUT_SIZE,
                    INPUT_SIZE,
                    90 - rotation);
            if (SAVE_DEBUG_IMAGE) {
                debugCounter++;
                if (debugCounter == 10) FileReaderWriter.writeToFile(TAG, bitmap);
            }
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
            int batchNum = 0;
            float[][][][] input = new float[1][INPUT_SIZE][INPUT_SIZE][3];
            for (int x = 0; x < INPUT_SIZE; x++) {
                for (int y = 0; y < INPUT_SIZE; y++) {
                    int pixel = bitmap.getPixel(x, y);
                    // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                    // model. For example, some models might require values to be normalized
                    // to the range [0.0, 1.0] instead.
                    input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f;
                    input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f;
                    input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f;
                }
            }
            try {
                FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                        .add(input)
                        .build();
                firebaseInterpreter.run(inputs, inputOutputOptions)
                        .addOnSuccessListener(result -> {
                            float[][] output = result.getOutput(0);
                            float[] probabilities = output[0];
                            //Log.d(TAG, "probability: " + probabilities[SEARCHED_LABEL_INDEX]);
                            boolean found = probabilities[SEARCHED_LABEL_INDEX] > PROBABILITY_MINIMUM;
                            classificationListener.onClassification(found);
                        })
                        .addOnFailureListener(e -> classificationListener.onClassification(false));
            } catch (FirebaseMLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
