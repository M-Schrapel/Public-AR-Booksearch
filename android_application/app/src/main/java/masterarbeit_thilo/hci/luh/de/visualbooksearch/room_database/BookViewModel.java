package masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database;

import android.app.Application;
import android.util.Log;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.image_processing.FeatureExtraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookViewModel extends AndroidViewModel {

    private static final String TAG = "room_database BookViewModel";

    public static final int INDEX_TITLE = 0;
    public static final int INDEX_SUBTITLE = 1;
    public static final int INDEX_AUTHOR = 2;
    public static final int INDEX_PUBLISHER = 3;
    public static final int INDEX_GENRE = 4;
    public static final int INDEX_WIDTH = 5;
    public static final int INDEX_COLOR = 6;
    public static final int INDEX_BOOKCASE = 7;

    private static Application application;
    private static Gson gson = new Gson();

    private BookRepository repository;
    private LiveData<List<BookEntity>> allBooks;
    private LiveData<List<BookEntity>> allOnQueryText;
    private MutableLiveData<String> queryText = new MutableLiveData<>();

    public BookViewModel(Application application) {
        super(application);
        BookViewModel.application = application;
        repository = new BookRepository(application);
        allBooks = repository.getAll();
        allOnQueryText = Transformations.switchMap(queryText, title ->
                repository.findOnQueryText(title));
    }

    public LiveData<List<BookEntity>> getAll() {
        return allBooks;
    }

    public LiveData<List<BookEntity>> getAllOnQueryText() {
        return allOnQueryText;
    }

    public void setOnQueryText(String query) {
        queryText.setValue(query);
    }

    public void insert(BookEntity bookEntity) {
        repository.insert(bookEntity);
    }

    public void delete(BookEntity bookEntity) {
        repository.delete(bookEntity);
    }

    public void clearAllTables() {
        repository.clearAllTables();
    }

    public static ArrayList<BookEntity> populateData() {
        ArrayList<BookEntity> books = new ArrayList<>();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(application.getAssets().open("book_spine_data.csv")));
            CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                int width = nextRecord[INDEX_WIDTH].isEmpty() ? 0 : Integer.parseInt(nextRecord[INDEX_WIDTH]);
                books.add(new BookEntity(
                        nextRecord[INDEX_TITLE],
                        nextRecord[INDEX_SUBTITLE],
                        nextRecord[INDEX_AUTHOR],
                        nextRecord[INDEX_PUBLISHER],
                        nextRecord[INDEX_GENRE],
                        gson.fromJson(nextRecord[INDEX_COLOR], float[].class)));
            }
            csvReader.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return books;
    }

}
