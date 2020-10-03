package masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database;

import android.app.Application;
import androidx.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class BookRepository {

    private static BookDatabase database;
    private static BookDao mBookDao;

    public BookRepository(Application application) {
        database = BookDatabase.getInstance(application);
        mBookDao = database.bookDao();
    }

    public LiveData<List<BookEntity>> getAll() {
        return mBookDao.getAll();
    }

    public LiveData<List<BookEntity>> findOnQueryText(String query) {
        return mBookDao.findOnQueryText(query);
    }

    public void insert(BookEntity bookEntity) {
        insertTask(bookEntity);
    }

    public void delete(BookEntity bookEntity) {
        deletetTask(bookEntity);
    }

    public void clearAllTables() {
        clearAllTablesTask();
    }

    private static void insertTask(final BookEntity bookEntity) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mBookDao.insert(bookEntity);
                return null;
            }
        }.execute();
    }

    private static void deletetTask(final BookEntity bookEntity) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mBookDao.delete(bookEntity);
                return null;
            }
        }.execute();
    }

    private static void clearAllTablesTask() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                database.clearAllTables();
                return null;
            }
        }.execute();
    }

}
