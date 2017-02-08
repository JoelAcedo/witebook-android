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
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.writebook.writebook.data.WritebookContract;
import com.writebook.writebook.services.WritebookVoteService;
import com.writebook.writebook.sync.WritebookSyncAdapter;

public class StoryDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = StoryDetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final String WRITEBOOK_SHARE_HASHTAG = " #Writebook";

    private ShareActionProvider mShareActionProvider;
    private String mWritebook;
    private ProgressBar mProgressBar;
    private Uri mUri;
    private ViewHolder mViewHolder;

    private BroadcastReceiver mBroadcastReceiver;
    private String mIntent_inmediatly_sync_finish;
    private String mIntent_inmediatly_sync_fail;


    private static final int DETAIL_LOADER = 0;

    private static final String[] DETAIL_COLUMNS = {
            WritebookContract.LibraryEntry.TABLE_NAME + "." +
            WritebookContract.LibraryEntry._ID,
            WritebookContract.LibraryEntry.COLUMN_ID_STORY,
            WritebookContract.LibraryEntry.COLUMN_TITLE,
            WritebookContract.LibraryEntry.COLUMN_AUTHOR,
            WritebookContract.LibraryEntry.COLUMN_CATEGORY,
            WritebookContract.LibraryEntry.COLUMN_TIME_READ,
            WritebookContract.LibraryEntry.COLUMN_DATE,
            WritebookContract.LibraryEntry.COLUMN_VOTES,
            WritebookContract.LibraryEntry.COLUMN_STORY_BODY
    };

    public static final int COL_LIBRARY_ID = 0;
    public static final int COL_LIBRARY_STORY_ID = 1;
    public static final int COL_LIBRARY_TITLE = 2;
    public static final int COL_LIBRARY_AUTHOR = 3;
    public static final int COL_LIBRARY_CATEGORY = 4;
    public static final int COL_LIBRARY_TIME_READ = 5;
    public static final int COL_LIBRARY_DATE = 6;
    public static final int COL_LIBRARY_VOTES = 7;
    public static final int COL_LIBRARY_STORY_BODY = 8;

    public static class ViewHolder {
        public final TextView titleView;
        public final TextView authorView;
        public final TextView timeReadView;
        public final TextView votesView;
        public final TextView categoryView;
        public final TextView storyBodyView;

        public ViewHolder(View view) {
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            authorView = (TextView) view.findViewById(R.id.list_item_author_textview);
            timeReadView = (TextView) view.findViewById(R.id.list_item_time_textview);
            votesView = (TextView) view.findViewById(R.id.list_item_votes_textview);
            categoryView = (TextView) view.findViewById(R.id.list_item_category_textview);
            storyBodyView = (TextView) view.findViewById(R.id.fragment_story_detail_body_textview);
        }
    }

    public StoryDetailFragment() {}

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
                    if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE)
                        mProgressBar.setVisibility(View.GONE);
                } else if (intent.getAction().equals(mIntent_inmediatly_sync_fail)) {
                    if (mProgressBar != null && mProgressBar.getVisibility() == View.VISIBLE)
                        mProgressBar.setVisibility(View.GONE);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(StoryDetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_story_detail, container, false);
        mViewHolder = new ViewHolder(rootView);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar_StoryDetailFragment);

        if (arguments != null)
            getStoryBodyFromServer();

        return rootView;
    }

    @Override
    public void onStart() {
        if (mViewHolder != null) {
            if (!Utility.getStoryTextSize(getActivity())
                    .equals(mViewHolder.storyBodyView.getTextSize())) {
                mViewHolder.storyBodyView.setTextSize(Utility.getStoryTextSize(getActivity()));
            }
        }

        super.onStart();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(mBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_story_fragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = //(ShareActionProvider) menuItem.getActionProvider();
                (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        // If onLoadFinished happens before this, we can go ahead and set the share intent now.
        if (mWritebook != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mWritebook + WRITEBOOK_SHARE_HASHTAG + " " +
            getString(R.string.writebook_message));

        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    private void getStoryBodyFromServer() {
        if (mUri != null) {
            String storyID = WritebookContract.LibraryEntry.getStoryID(mUri);
            mProgressBar.setVisibility(View.VISIBLE);
            WritebookSyncAdapter.syncImmediatelyWithBodyStory(getActivity(), storyID);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null && data.moveToFirst()) {

            String titleStr = data.getString(COL_LIBRARY_TITLE);
            mViewHolder.titleView.setText(titleStr);

            String authorStr = data.getString(COL_LIBRARY_AUTHOR);
            mViewHolder.authorView.setText(authorStr);

            String timeReadStr = data.getString(COL_LIBRARY_TIME_READ);
            mViewHolder.timeReadView.setText(timeReadStr + " min");

            String categoryJson = data.getString(COL_LIBRARY_CATEGORY);
            String categoryStr = Utility.getCategoryFromJsonCat(getActivity(), categoryJson);
            mViewHolder.categoryView.setText(categoryStr);

            final String storyId = data.getString(COL_LIBRARY_STORY_ID);
            ImageView imageVotesView = (ImageView) getActivity().findViewById(R.id.list_like_icon);

            if (imageVotesView != null) {
                if (!Utility.storyVoted(getActivity(), storyId)) {
                    imageVotesView.setImageResource(R.drawable.ic_heart_outline);
                    imageVotesView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utility.setStoryVoted(getActivity(), storyId, true);
                            WritebookVoteService.startActionVote(getActivity(), storyId);
                        }
                    });

                } else {
                    imageVotesView.setImageResource(R.drawable.ic_heart);
                    imageVotesView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utility.setStoryVoted(getActivity(), storyId, false);
                            WritebookVoteService.startActionUnvote(getActivity(), storyId);
                        }
                    });
                }
            }


            Integer votesInt = data.getInt(COL_LIBRARY_VOTES);
            mViewHolder.votesView.setText(votesInt.toString());

            mViewHolder.storyBodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                    Utility.getStoryTextSize(getActivity()));

            String bodyStr = data.getString(COL_LIBRARY_STORY_BODY);
            mViewHolder.storyBodyView.setText(Html.fromHtml(bodyStr));

            ColorDrawable colorDrawable = Utility.getColorCategory(getActivity(), categoryJson);
            ColorDrawable barColorDrawable = Utility.getColorCategory(getActivity(), categoryJson);

            /*this.getView().findViewById(R.id.list_item_category_color_layout)
                    .setBackgroundDrawable(colorDrawable);
            this.getActivity().getActionBar()
                    .setBackgroundDrawable(barColorDrawable);*/

            mWritebook = titleStr + " " + getString(R.string.writebook_by) + " " + authorStr;

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}