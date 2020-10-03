package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_classification;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.ar_core.ClassificationListener;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.ImageUtils;

// ImageClassification mithilfe von Firebase,
// welche allerdings schlechtere Ergebnisse lieferte und deshalb nicht genutzt wurde
public class ImageClassificationFirebase {

    private static final String TAG = "ImageClassification Firebase";
    private static final boolean SAVE_DEBUG_IMAGE = false;
    private static final String SEARCHED_LABEL = "Shelf";
    private static final float CONFIDENCE_MINIMUM = 0.6f;
    private static int debugCounter = 0;

    public static ImageClassificationTask startClassification(Image image, int rotation, ClassificationListener classificationListener) {
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
            Bitmap bitmap = ImageUtils.getRotatedBitmap(ImageUtils.YUV_420_888toBitmap(image[0]), 90 - rotation);
            if (SAVE_DEBUG_IMAGE) {
                debugCounter++;
                if (debugCounter == 10) FileReaderWriter.writeToFile(TAG, bitmap);
            }
            FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVision.getInstance().getOnDeviceImageLabeler().processImage(firebaseVisionImage)
                    .addOnSuccessListener(labels -> {
                        boolean found = false;
                        for (FirebaseVisionImageLabel label : labels) {
                            String text = label.getText();
                            float confidence = label.getConfidence();
                            if (text.equals(SEARCHED_LABEL)) {
                                //Log.d(TAG, "confidence: " + confidence);
                                if (confidence > CONFIDENCE_MINIMUM) {
                                    found = true;
                                }
                                break;
                            }
                        }
                        classificationListener.onClassification(found);
                    })
                    .addOnFailureListener(e -> classificationListener.onClassification(false));
            return null;
        }
    }

}
