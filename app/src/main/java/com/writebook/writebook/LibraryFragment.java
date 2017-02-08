package com.writebook.writebook;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.writebook.writebook.data.WritebookContract;
import com.writebook.writebook.sync.WritebookSyncAdapter;

/**
 * Created by Joel on 11/06/2015.
 */
public class LibraryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private final String LOG_TAG = LibraryFragment.class.getSimpleName();

    private LibraryAdapter mLibraryAdapter;
    private ListView mListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BroadcastReceiver mBroadcastReceiver;

    private boolean mUseDetailLayout;
    private int mPosition = ListView.INVALID_POSITION;
    private static final String SELECTED_KEY = "selected_position";

    private String mIntent_inmediatly_sync_finish;
    private String mIntent_inmediatly_sync_fail;

    private static final int LIBRARY_LOADER = 0;

    private static final String[] LIBRARY_COLUMNS = {
            WritebookContract.LibraryEntry.TABLE_NAME + "." +
            WritebookContract.LibraryEntry._ID,
            WritebookContract.LibraryEntry.COLUMN_TITLE,
            WritebookContract.LibraryEntry.COLUMN_AUTHOR,
            WritebookContract.LibraryEntry.COLUMN_CATEGORY,
            WritebookContract.LibraryEntry.COLUMN_TIME_READ,
            WritebookContract.LibraryEntry.COLUMN_DATE,
            WritebookContract.LibraryEntry.COLUMN_VOTES,
            WritebookContract.LibraryEntry.COLUMN_ID_STORY
    };

    public static final int COL_LIBRARY_ID = 0;
    public static final int COL_LIBRARY_TITLE = 1;
    public static final int COL_LIBRARY_AUTHOR = 2;
    public static final int COL_LIBRARY_CATEGORY = 3;
    public static final int COL_LIBRARY_TIME_READ = 4;
    public static final int COL_LIBRARY_DATE = 5;
    public static final int COL_LIBRARY_VOTES = 6;
    public static final int COL_LIBRARY_STORY_ID = 7;


    public LibraryFragment() {
    }

    public interface Callback {
        public void onItemSelected(Uri dateUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mIntent_inmediatly_sync_finish = getString(R.string.inmediatly_sync_finish);
        mIntent_inmediatly_sync_fail = getString(R.string.inmediatly_sync_fail);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(mIntent_inmediatly_sync_finish)) {
                    if (mSwipeRefreshLayout != null)
                        mSwipeRefreshLayout.setRefreshing(false);
                } else if (intent.getAction().equals(mIntent_inmediatly_sync_fail)) {
                    if (mSwipeRefreshLayout != null)
                        mSwipeRefreshLayout.setRefreshing(false);

                    Toast toast = Toast.makeText(context, R.string.http_connection_timeout,
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(mIntent_inmediatly_sync_finish);
        intentFilter.addAction(mIntent_inmediatly_sync_fail);
        LocalBroadcastManager.getInstance(getActivity()).
                registerReceiver(mBroadcastReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_library, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LIBRARY_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void updateLibrary() {
        WritebookSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLibraryAdapter = new LibraryAdapter(getActivity(), null, 0);
        View rootView = inflater.inflate(R.layout.fragment_library, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview_library);
        mListView.setAdapter(mLibraryAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String storyID = cursor.getString(COL_LIBRARY_STORY_ID);
                    Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyID);

                    ((Callback) getActivity()).onItemSelected(uri);
                }
                mPosition = position;
            }
        });
        mListView.setEmptyView(rootView.findViewById(R.id.empty_list_library));

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mLibraryAdapter.setUseDetailLayout(mUseDetailLayout);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipe_refresh_library);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.green_A400, R.color.blue_400,
                R.color.orange_400, R.color.red_400);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateLibrary();
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    void onLanguageChanged( ) {
        updateLibrary();
        getLoaderManager().restartLoader(LIBRARY_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = WritebookContract.LibraryEntry.COLUMN_ID_STORY + " DESC";
        Uri libraryUri = WritebookContract.LibraryEntry.buildLibraryTitlesUri();

        String storyLanguage = Utility.getStoryLanguage(getActivity());
        String sStoryWithLanguage = WritebookContract.LibraryEntry.TABLE_NAME +
                        "." + WritebookContract.LibraryEntry.COLUMN_LANGUAGE + " = ? " ;

        if (storyLanguage.equals("any")) {
            return new CursorLoader(getActivity(),
                    libraryUri,
                    LIBRARY_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        } else {
            return new CursorLoader(getActivity(),
                    libraryUri,
                    LIBRARY_COLUMNS,
                    sStoryWithLanguage,
                    new String[]{storyLanguage},
                    sortOrder
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLibraryAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLibraryAdapter.swapCursor(null);
    }

    public void setUseDetailLayout(boolean useDetailLayout) {
        mUseDetailLayout = useDetailLayout;
        if (mLibraryAdapter != null) {
            mLibraryAdapter.setUseDetailLayout(useDetailLayout);
        }
    }
}
