package masterarbeit_thilo.hci.luh.de.visualbooksearch;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.appcompat.app.AppCompatActivity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.widget.SearchView;

import com.google.ar.core.ArCoreApk;

import org.opencv.android.OpenCVLoader;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.Debug;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.debug.FileReaderWriter;
import masterarbeit_thilo.hci.luh.de.visualbooksearch.room_database.BookViewModel;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "VisualBookSearch";

    private static boolean ARCoreSupported;

    private BookViewModel mBookViewModel;
    private BookAdapter mBookAdapter;
    private String speechQuery = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "OpenCVLoader.initDebug(): " + String.valueOf(OpenCVLoader.initDebug()));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Liste mit allen bzw. den gefilterten Büchern
        RecyclerView mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(), mLayoutManager.getOrientation());
        mRecyclerView.addItemDecoration(mDividerItemDecoration);

        mBookViewModel = ViewModelProviders.of(this).get(BookViewModel.class);
        mBookAdapter = new BookAdapter();
        mRecyclerView.setAdapter(mBookAdapter);
        mBookViewModel.getAllOnQueryText().observe(this, bookEntities -> {
            Log.d(TAG, "getAllOnQueryText: " + bookEntities.size());
            mBookAdapter.setData(bookEntities, speechQuery);
            speechQuery = null;
        });
        mBookViewModel.setOnQueryText("%"); // damit alle Bücher am Anfang gezeigt werden
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        FileReaderWriter.createDirectory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search_item).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);
                mBookViewModel.setOnQueryText(newText.concat("%"));
                return false;
            }
        });
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            speechQuery = intent.getStringExtra(SearchManager.QUERY);
            mBookViewModel.setOnQueryText("%");
        }
    }

    private void checkARCoreSupport() {
        Log.i(TAG, "checkARCoreSupport");
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Re-query at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(() -> {
                Log.i(TAG, "run()");
                checkARCoreSupport();
            }, 200);
        }
        ARCoreSupported = availability.isSupported();
        Log.i(TAG, "ARCoreSupported = " + ARCoreSupported);
    }

    public static boolean isARCoreSupported() {
        return ARCoreSupported;
    }

}
