package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_classification;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core.ClassificationListener;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.ImageUtils;

// ImageClassification mithilfe von TensorFlow und dem mobilenet_v1
public class ImageClassificationTensorFlow {

    private static final String TAG = "ImageClassification TensorFlow";
    private static final boolean SAVE_DEBUG_IMAGE = false;
    private static final float PROBABILITY_MINIMUM = 0.3f;

    private static ImageClassifierFloatMobileNet classifier;
    private static int debugCounter;

    public ImageClassificationTensorFlow(Context context) {
        debugCounter = 0;
        try {
            classifier = new ImageClassifierFloatMobileNet(context);
        } catch (IOException e) {
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
            // Bitmap muss skaliert und rotiert werden, sodass es 224x224 groß ist und aufrecht steht
            Bitmap bitmap = ImageUtils.getResizedRotatedBitmap(
                    ImageUtils.YUV_420_888toBitmap(image[0]),
                    classifier.getImageSizeX(),
                    classifier.getImageSizeY(),
                    rotation + 90);
            if (SAVE_DEBUG_IMAGE) {
                debugCounter++;
                if (debugCounter == 10) FileReaderWriter.writeToFile(TAG, bitmap);
            }
            float probability = classifier.classifyFrame(bitmap);
            Log.d(TAG, "probability: " + probability);
            classificationListener.onClassification(probability > PROBABILITY_MINIMUM);
            return null;
        }

    }

}
