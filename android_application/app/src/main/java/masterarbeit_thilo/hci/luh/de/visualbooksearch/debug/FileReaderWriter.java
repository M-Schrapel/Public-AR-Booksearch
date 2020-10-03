package masterarbeit_thilo.hci.luh.de.visualbooksearch.debug;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookEntity;

// Zum Laden und Speichern von Debug Bildern
public class FileReaderWriter {

    private static final String TAG = "FileReaderWriter";

    // Ordner an dem sich die Testdateien befinden müssen und wo die Debugausgaben gespeichert werden
    private static final String DIR_NAME = "visual_book_search";

    private static File directory, subDirectory;

    // Einmaliges Erstellen des Debugordners
    public static void createDirectory() {
        directory = new File(Environment.getExternalStorageDirectory() + "/" + DIR_NAME);
        directory.mkdir();
    }

    // Erstellen eines Unterordners für den jeweiligen Suchdurchlauf
    public static void createSubDirectory() {
        String timeStamp = new SimpleDateFormat("dd_MM_yyyy_HHmmss").format(new Date());
        subDirectory = new File(directory + "/" + timeStamp);
        subDirectory.mkdir();
    }

    // Laden eines alten Bildes für die Suche
    public static void loadFile(String fileName, Mat src) {
        Bitmap bmp = BitmapFactory.decodeFile(directory + "/" + fileName + ".jpg");
        Utils.bitmapToMat(bmp, src);
    }

    // OpenCV Mat wird zu einer Bitmap konvertiert und in eine Datei geschrieben
    public static void writeToFile(String fileName, Mat mat) {
        long startTime = SystemClock.uptimeMillis();
        File file = new File(subDirectory + "/" + fileName + ".jpg");
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bmp);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "writeToFile: " + fileName + " = " + (endTime - startTime) + "ms");
    }

    // Bitmap wird in eine Datei geschrieben
    public static void writeToFile(String fileName, Bitmap bmp) {
        long startTime = SystemClock.uptimeMillis();
        File file = new File(directory + "/" + fileName + ".jpg");
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "writeToFile: " + fileName + " = " + (endTime - startTime) + "ms");
    }

    public static void writeToFile(String fileName, byte[] bytes) {
        File file = new File(subDirectory + "/" + fileName + ".jpg");
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                    //Log.i(TAG, "writeFile: " + name_ext + " finished");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Zur Konvertierung einer OpenCV Mat zu einer Bitmap
    public static Bitmap getBitmapFromMat(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);
        return bmp;
    }

    public static String getFeatureImportancesFileName(BookEntity book) {
        return subDirectory + "/" + book.title + ".csv";
    }

}
