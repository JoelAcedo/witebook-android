package com.writebook.writebook;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.writebook.writebook.auth.LoginActivity;
import com.writebook.writebook.data.WritebookContract;
import com.writebook.writebook.sync.WritebookSyncAdapter;


public class MainActivity extends AppCompatActivity implements LibraryFragment.Callback {
    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String DETAILFRAGMENT_TAG = "DFTAG";

    private boolean mTwoPane;
    private String mLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLanguage = Utility.getStoryLanguage(this);

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.story_detail_container) != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                String storyId = Utility.getLastStoryIdSavedWithSelectedLanguage(this);

                if (storyId.equals(Utility.DEFAULT_LAST_STORY_ID)) {
                    getFragmentManager().beginTransaction()
                            .add(R.id.story_detail_container, new StoryDetailFragment(), DETAILFRAGMENT_TAG)
                            .commit();
                } else {
                    Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyId);
                    createStoryDetailFragment(uri);
                }
            }
        } else {
            mTwoPane = false;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LibraryFragment libraryFragment = ((LibraryFragment) getFragmentManager()
                            .findFragmentById(R.id.fragment_library));
        libraryFragment.setUseDetailLayout(!mTwoPane);

        WritebookSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String language = Utility.getStoryLanguage(this);

        if (language != null && !language.equals(mLanguage)) {
            LibraryFragment lf = (LibraryFragment)getFragmentManager().findFragmentById(R.id.fragment_library);
            if ( null != lf ) {
                lf.onLanguageChanged();
            }

            if (mTwoPane) {
                StoryDetailFragment sdf = (StoryDetailFragment) getFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);

                if (null != sdf) {
                    String storyId = Utility.getLastStoryIdSavedWithSelectedLanguage(this);

                    if (!storyId.equals(Utility.DEFAULT_LAST_STORY_ID)) {
                        Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyId);
                        createStoryDetailFragment(uri);
                    }
                }
            }
            mLanguage = language;
        }

        if (!mTwoPane) {
            StoryDetailFragment sdf = (StoryDetailFragment) getFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != sdf) {
                getFragmentManager().beginTransaction().remove(sdf).commit();
                sdf.onDestroy();

                sdf = (StoryDetailFragment) getFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            }

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingIntent);
            return true;
        } else if (id == R.id.action_about_upload) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.dialog_upload_stories_info) + " \n\n"
                    + getString(R.string.dialog_upload_stories_page) + "\n")
                    .setPositiveButton(R.string.dialog_upload_stories_go, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(getString(R.string.dialog_upload_stories_url)));

                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton(R.string.dialog_upload_stories_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

            Dialog dialog = builder.create();
            dialog.show();
        } else if (id == R.id.action_login) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            createStoryDetailFragment(contentUri);

        } else {
            Intent intent = new Intent(this, StoryDetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }

    private void createStoryDetailFragment(Uri contentUri) {
        Bundle args = new Bundle();
        args.putParcelable(StoryDetailFragment.DETAIL_URI, contentUri);

        StoryDetailFragment storyDetailFragment = new StoryDetailFragment();
        storyDetailFragment.setArguments(args);

        getFragmentManager().beginTransaction()
                .replace(R.id.story_detail_container, storyDetailFragment,
                        DETAILFRAGMENT_TAG)
                .commit();
    }
}
