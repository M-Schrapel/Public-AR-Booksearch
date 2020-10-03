package masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines.BookSpine;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;

// Diese Klasse ist für die Extraktion der Merkmale (Farbe, Text) zuständig
public class FeatureExtraction {

    private static final String TAG = "image_processing FeatureExtraction";

    public enum HistColor {BLACK, WHITE_GRAY, RED, YELLOW, GREEN, CYAN, BLUE, MAGENTA};

    private ArrayList<Double> colorTimes = new ArrayList<>();
    private ArrayList<Double> textTimes = new ArrayList<>();
    private FirebaseVisionTextRecognizer detector;
    private FeatureMatching featureMatching;
    private List<Task<FirebaseVisionText>> tasks = new ArrayList<>();

    public FeatureExtraction(BookEntity book) {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        featureMatching = new FeatureMatching(book);
    }

    public BookSpine extractFeatures(ArrayList<BookSpine> bookSpines) {
        long start = System.currentTimeMillis();
        for (final BookSpine bookSpine : bookSpines) {
            extractColor(bookSpine);
            if (!featureMatching.checkColor(bookSpine)) { // wenn die Farbe zu stark abweicht, dann mit dem nächsten weitermachen
                if (DEBUGGING) {
                    Point[] cornerPoints = bookSpine.getCornerPoints();
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[0], cornerPoints[1], Debug.RED, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[1], cornerPoints[2], Debug.RED, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[2], cornerPoints[3], Debug.RED, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[3], cornerPoints[0], Debug.RED, 3);
                }
                continue;
            } else {
                if (DEBUGGING) {
                    Point[] cornerPoints = bookSpine.getCornerPoints();
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[0], cornerPoints[1], Debug.GREEN, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[1], cornerPoints[2], Debug.GREEN, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[2], cornerPoints[3], Debug.GREEN, 3);
                    Debug.drawLine(Debug.FILTER_8_DISCARD_COLOR, cornerPoints[3], cornerPoints[0], Debug.GREEN, 3);
                }
            }
            extractText(bookSpine);
            //if (DEBUGGING) bookSpine.writeToFile();
        }
        double totalTime = 0;
        for (double t : colorTimes) {
            totalTime += t;
        }
        Log.d(TAG, "colorTimes.size(): " + colorTimes.size() + ", totalTime: " + totalTime);
        Task<Void> waitAll = Tasks.whenAll(tasks).addOnCompleteListener(task -> {});
        try {
            Tasks.await(waitAll);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Tasks.await(waitAll)");
        //Log.d(TAG, "textTimes.size(): " + textTimes.size() + ", totalTime: " + textTimes.get(textTimes.size() - 1));

        try {
            detector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        for (BookSpine b : bookSpines) {
            Log.d(TAG, "-----");
            Log.d(TAG, "Book spine " + b.getId() + ": ");
            Log.d(TAG, "Color: " + b.getSimilarityColor() + " " + Arrays.toString(b.getColorFeatureVector()));
            Log.d(TAG, "Title: " + b.getSimilarityTitle());
            Log.d(TAG, "Subtitle: " + b.getSimilaritySubtitle());
            Log.d(TAG, "Author: " + b.getSimilarityAuthor());
            Log.d(TAG, "Publisher: " + b.getSimilarityPublisher());
        }
        */

        Log.d(TAG, "-----");
        double time = (System.currentTimeMillis() - start) / 1000.0;
        Log.i(TAG, "extractFeatures(): " + time + " s");
        return featureMatching.findBestMatch(bookSpines);
    }

    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }


    // Der Farbvektor (4.5.1) wird berechnet
    private void extractColor(BookSpine bookSpine) {
        // @TODO hier lineare Interpolation und in create_data_set.py auch ändern
        long start = System.currentTimeMillis();
        Mat img = bookSpine.getImg();
        float[] hist = new float[HistColor.values().length];

        /*
        Log.i(TAG, "total: " + img.total());
        Log.i(TAG, "row*col: " + img.rows() * img.cols());
        Log.i(TAG, "size: " + img.size());
        Log.i(TAG, "channels: " + img.channels());
        */

        int size = img.rows() * img.cols();
        byte[] img_data = new byte[size * img.channels()];
        img.get(0, 0, img_data);
        int i = 0;
        while (i < size * img.channels()) {
            float r = (img_data[i++] & 0xFF) / 255f;
            float g = (img_data[i++] & 0xFF) / 255f;
            float b = (img_data[i++] & 0xFF) / 255f;
            float max = Math.max(Math.max(r, g), b);
            float min = Math.min(Math.min(r, g), b);

            float value = max;
            //Log.i(TAG, "v_" + i + ": " + value);

            float saturation = (max == 0) ? 0 : (max - min) / max;
            //Log.i(TAG, "s_" + i + ": " + saturation);
            if (value < 0.15) {hist[HistColor.BLACK.ordinal()]++; continue;}
            if (saturation < 0.3) {hist[HistColor.WHITE_GRAY.ordinal()]++; continue;}

            float hue;
            if (max == min) {
                hue = 0;
            } else if (max == r) {
                hue = 60 * (g - b) / (max - min);
            } else if (max == g) {
                hue = 60 * (2 + (b - r) / (max - min));
            } else {
                hue = 60 * (4 + (r - g) / (max - min));
            }
            if (hue < 0) hue += 360;
            //Log.i(TAG, "h_" + i + ": " + hue);
            if (hue <= 40 || hue > 320) {hist[HistColor.RED.ordinal()]++; continue;}
            if (hue <= 80) {hist[HistColor.YELLOW.ordinal()]++; continue;}
            if (hue <= 160) {hist[HistColor.GREEN.ordinal()]++; continue;}
            if (hue <= 200) {hist[HistColor.CYAN.ordinal()]++; continue;}
            if (hue <= 280) {hist[HistColor.BLUE.ordinal()]++;}
            else {hist[HistColor.MAGENTA.ordinal()]++;}

            //Log.i(TAG, "------");
        }
        for (i = 0; i < hist.length; i++) {
            //Log.i(TAG, "hist[" + HistColor.values()[i].toString() + "] = " + hist[i]);
            hist[i] /= size;
            //Log.i(TAG, "hist[" + HistColor.values()[i].toString() + "] = " + hist[i]);
        }
        double time = (System.currentTimeMillis() - start) / 1000.0;
        //Log.i(TAG, "extractColor(): " + time + " s");
        //Log.i(TAG, "-----");
        colorTimes.add(time);
        bookSpine.setColorFeatureVector(hist);
    }

    // Der Text wird extrahiert (4x pro Buchrücken)
    private void extractText(BookSpine bookSpine) {
        final long start = System.currentTimeMillis();
        Mat img = bookSpine.getImg();
        for (int i = 0; i < 4; i++) {
            final int final_i = i;
            if (i != 0) Core.rotate(img, img, Core.ROTATE_90_CLOCKWISE);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(FileReaderWriter.getBitmapFromMat(img));
            //final long start = System.currentTimeMillis();
            tasks.add(detector.processImage(image)
                    .addOnSuccessListener(text -> {
                        //textBlocks.addAll(text.getTextBlocks());
                        //if (id == 2) Log.i(TAG, final_i + text.getText());
                        //bookSpine.addTextBlocks(text);
                        featureMatching.checkText(bookSpine, text);
                        if (final_i == 3) {
                            double time = (System.currentTimeMillis() - start) / 1000.0;
                            textTimes.add(time);
                            Log.i(TAG, "BookSpine_" + bookSpine.getId() + ", OCR duration: " + time + " s");
                        }
                    })
                    .addOnFailureListener(exception -> {
                        //if (id == 2) Log.i(TAG, final_i + "onFailure");
                        if (final_i == 3) {
                            double time = (System.currentTimeMillis() - start) / 1000.0;
                            textTimes.add(time);
                            Log.i(TAG, "BookSpine_" + bookSpine.getId() + ", OCR duration: " + time + " s");
                        }
                    }));
        }
    }

}
