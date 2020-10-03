package masterarbeit_thilo.hci.luh.de.visualbooksearch.segmentation_book_spines;

import android.graphics.Rect;
import android.util.Log;

import com.google.firebase.ml.vision.text.FirebaseVisionText;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;

import static masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug.DEBUGGING;

// Klasse für ein segmentiertes Buchrückenbild
public class BookSpine {

    private static final String TAG = "segmentation_book_spine BookSpine";
    private static final boolean SHOW_OCR_RESULT_IN_IMAGE = true;

    private static int ID = 1;

    private Mat img;
    private Point[] cornerPoints;
    // cornerPoints[0] = oben links
    // cornerPoints[1] = oben rechts
    // cornerPoints[2] = unten rechts
    // cornerPoints[3] = unten links
    private int id;
    private float[] colorFeatureVector;
    private double similarity_title, similarity_subtitle, similarity_author, similarity_publisher, similarity_color;
    private ArrayList<FirebaseVisionText.TextBlock> textBlocks = new ArrayList<>();

    public BookSpine(Mat img, Point[] cornerPoints) {
        this.img = img;
        this.cornerPoints = cornerPoints;
        id = ID++;
        if (DEBUGGING) {
            /*
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[0], Debug.RED);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[1], Debug.GREEN);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[2], Debug.BLUE);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[3], Debug.YELLOW);
            */
            /*
            Debug.drawLine(Debug.RESULT_SPINES, cornerPoints[0], cornerPoints[1], Debug.YELLOW, 8);
            Debug.drawLine(Debug.RESULT_SPINES, cornerPoints[1], cornerPoints[2], Debug.YELLOW, 8);
            Debug.drawLine(Debug.RESULT_SPINES, cornerPoints[2], cornerPoints[3], Debug.YELLOW, 8);
            Debug.drawLine(Debug.RESULT_SPINES, cornerPoints[3], cornerPoints[0], Debug.YELLOW, 8);
            */
            /*
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[0], Debug.RED);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[1], Debug.RED);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[2], Debug.RED);
            Debug.drawCircle(Debug.RESULT_SPINES, cornerPoints[3], Debug.RED);
            Debug.drawText(Debug.RESULT_SPINES, getCenter(), 0.8, "" + id, Debug.YELLOW);
            */
            //writeToFile();
        }
    }

    public static void resetId() {
        ID = 1;
    }

    public Mat getImg() {
        return img;
    }


    public void drawOCR(FirebaseVisionText text) {
        if (SHOW_OCR_RESULT_IN_IMAGE) {
            for (FirebaseVisionText.TextBlock block : text.getTextBlocks()) {
                String blockText = block.getText();
                Rect blockFrame = block.getBoundingBox();
                Imgproc.rectangle(img, new org.opencv.core.Point(blockFrame.left, blockFrame.top)
                        , new org.opencv.core.Point(blockFrame.right, blockFrame.bottom), new Scalar(255, 255, 0), 3);
                int[] baseLine = new int[1];
                Size textSize = Imgproc.getTextSize(blockText, Imgproc.FONT_HERSHEY_DUPLEX, 0.5, 1, baseLine);
                Imgproc.rectangle(img, new org.opencv.core.Point(blockFrame.left, blockFrame.top + 2)
                        , new org.opencv.core.Point(blockFrame.left + textSize.width, blockFrame.top - textSize.height)
                        , new Scalar(255, 255, 255), Core.FILLED);
                Imgproc.putText(img, blockText, new org.opencv.core.Point(blockFrame.left, blockFrame.top)
                        , Imgproc.FONT_HERSHEY_DUPLEX, 0.5, new Scalar(0, 0, 0), 1);
            }
        }
    }

    public void writeToFile() {
        if (id < 10) FileReaderWriter.writeToFile("book_spine_0" + id, img);
        else FileReaderWriter.writeToFile("book_spine_" + id, img);
    }

    public void setColorFeatureVector(float[] colorFeatureVector) {
        this.colorFeatureVector = colorFeatureVector;
        //Log.d(TAG, "Book_" + id + ": " + Arrays.toString(colorFeatureVector));
    }

    public float[] getColorFeatureVector() {
        return colorFeatureVector;
    }

    public int getId() {
        return id;
    }

    public void setSimilarityColor(double similarity_color) {
        this.similarity_color = similarity_color;
        Log.d(TAG, "Book_" + id + ": " + similarity_color);
    }

    public void setSimilarityTitle(double similarity_title) {
        if (similarity_title > this.similarity_title) this.similarity_title = similarity_title;
    }

    public void setSimilaritySubtitle(double similarity_subtitle) {
        if (similarity_subtitle > this.similarity_subtitle) this.similarity_subtitle = similarity_subtitle;
    }

    public void setSimilarityAuthor(double similarity_author) {
        if (similarity_author > this.similarity_author) this.similarity_author = similarity_author;
    }

    public void setSimilarityPublisher(double similarity_publisher) {
        if (similarity_publisher > this.similarity_publisher) this.similarity_publisher = similarity_publisher;
    }

    public double getSimilarityColor() {
        return similarity_color;
    }

    public double getSimilarityTitle() {
        return similarity_title;
    }

    public double getSimilaritySubtitle() {
        return similarity_subtitle;
    }

    public double getSimilarityAuthor() {
        return similarity_author;
    }

    public double getSimilarityPublisher() {
        return similarity_publisher;
    }

    public Point[] getCornerPoints() {
        return cornerPoints;
    }

    public float getWidth() {
        return (float) (Math.sqrt(Math.pow(cornerPoints[0].x - cornerPoints[1].x, 2) + Math.pow(cornerPoints[0].y - cornerPoints[1].y, 2))
                + Math.sqrt(Math.pow(cornerPoints[2].x - cornerPoints[3].x, 2) + Math.pow(cornerPoints[2].y - cornerPoints[3].y, 2))) / 2;
    }

    public float getHeight() {
        return (float) (Math.sqrt(Math.pow(cornerPoints[1].x - cornerPoints[2].x, 2) + Math.pow(cornerPoints[1].y - cornerPoints[2].y, 2))
                + Math.sqrt(Math.pow(cornerPoints[3].x - cornerPoints[0].x, 2) + Math.pow(cornerPoints[3].y - cornerPoints[0].y, 2))) / 2;
    }

    // Winkel von 0-180 (90 = Buch steht senkrecht im Regal)
    public double getRotation() {
        double xRight = cornerPoints[1].x - cornerPoints[2].x;
        double yRight = - (cornerPoints[1].y - cornerPoints[2].y); // minus, da y in screen umgekehrt
        double lengthRight = Math.sqrt(Math.pow(xRight, 2) + Math.pow(yRight, 2));
        double angleRight = Math.acos(xRight / lengthRight);

        double xLeft = cornerPoints[0].x - cornerPoints[3].x;
        double yLeft = - (cornerPoints[0].y - cornerPoints[3].y); // minus, da y in screen umgekehrt
        double lengthLeft = Math.sqrt(Math.pow(xLeft, 2) + Math.pow(yLeft, 2));
        double angleLeft = Math.acos(xLeft / lengthLeft);

        return (angleLeft + angleRight) / 2;
    }

    public Point getCenter() {
        float centerX = 0;
        float centerY = 0;
        for (Point p : cornerPoints) {
            centerX += p.x;
            centerY += p.y;
        }
        centerX /= cornerPoints.length;
        centerY /= cornerPoints.length;
        Point center = new Point(centerX, centerY);
        return center;
    }

    public void fitCornerPointsToScreenSize(Size imageSize, int screenWidth, int screenHeight, int rotation) {
        Log.d(TAG, "imageSize: " + imageSize);
        Log.d(TAG, "screenSize: " + screenWidth + "x" + screenHeight);
        Log.d(TAG, "rotation: " + rotation);
        for (int i = 0; i < cornerPoints.length; i++) {
            Log.d(TAG, "old_" + i + " :" + cornerPoints[i]);
            // yOffset, because the camera(3024/4032) has not the same aspect ratio as the screen(1080/2220)
            if (rotation == 0) {
                double yOffset = screenHeight - screenWidth * imageSize.height / imageSize.width;
                //Log.d(TAG, "yOffset: " + yOffset);
                cornerPoints[i] = new Point(
                        screenWidth * cornerPoints[i].x / imageSize.width,
                        (screenHeight - yOffset) * cornerPoints[i].y / imageSize.height + yOffset / 2);
            } else if (rotation == 90) {
                double yOffset = imageSize.height - imageSize.width * screenWidth / screenHeight;
                //Log.d(TAG, "yOffset: " + yOffset);
                cornerPoints[i] = new Point(
                        screenWidth - screenWidth * (cornerPoints[i].y - yOffset / 2) / (imageSize.height - yOffset),
                        screenHeight * cornerPoints[i].x / imageSize.width);
            } else if (rotation == 270) {
                double yOffset = imageSize.height - imageSize.width * screenWidth / screenHeight;
                //Log.d(TAG, "yOffset: " + yOffset);
                cornerPoints[i] = new Point(
                        screenWidth - screenWidth * (cornerPoints[i].y - yOffset / 2) / (imageSize.height - yOffset),
                        screenHeight * cornerPoints[i].x / imageSize.width);
            }
        }
        for (int i = 0; i < cornerPoints.length; i++) {
            Log.d(TAG, "new_" + i + " :" + cornerPoints[i]);
        }
    }

    public void resetValuesForCSV() {
        similarity_title = 0;
        similarity_subtitle = 0;
        similarity_author = 0;
        similarity_publisher = 0;
        similarity_color = 0;
    }
}
