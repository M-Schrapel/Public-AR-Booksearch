package masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BookDao {

    @Query("SELECT * FROM book_entities ORDER BY title")
    public LiveData<List<BookEntity>> getAll();

    @Query("SELECT * FROM book_entities where title LIKE :query ORDER BY title")
    public LiveData<List<BookEntity>> findOnQueryText(String query);

    @Insert
    public void insert(BookEntity book);

    @Insert
    public void insertAll(List<BookEntity> books);

    @Delete
    public void delete(BookEntity book);

}
