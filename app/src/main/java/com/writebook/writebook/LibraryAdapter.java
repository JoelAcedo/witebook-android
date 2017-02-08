package com.writebook.writebook;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Joel on 11/06/2015.
 */
public class LibraryAdapter extends CursorAdapter {
    public static final String LOG_TAG = LibraryAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_FAVOURITE = 0;
    private static final int VIEW_TYPE_NORMAL_ENTRIES = 1;

    private boolean mUseDetailLayout = true;

    public static class ViewHolder {
        public final TextView titleView;
        public final TextView authorView;
        public final TextView timeReadView;

        public ViewHolder(View view) {
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            authorView = (TextView) view.findViewById(R.id.list_item_author_textview);
            timeReadView = (TextView) view.findViewById(R.id.list_item_time_textview);
        }
    }

    public LibraryAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int position = cursor.getPosition();
        int viewType = getItemViewType(position);
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_FAVOURITE: {
                layoutId = R.layout.list_view_libray_detail;
                break;
            }
            case VIEW_TYPE_NORMAL_ENTRIES: {
                layoutId = R.layout.list_view_library;
                break;
            }
        }
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int position = cursor.getPosition();
        int viewType = getItemViewType(position);

        String title = cursor.getString(LibraryFragment.COL_LIBRARY_TITLE);
        viewHolder.titleView.setText(title);

        String author = cursor.getString(LibraryFragment.COL_LIBRARY_AUTHOR);
        viewHolder.authorView.setText(author);

        String time_read = cursor.getString(LibraryFragment.COL_LIBRARY_TIME_READ);
        viewHolder.timeReadView.setText(time_read + " min");

        String category = cursor.getString(LibraryFragment.COL_LIBRARY_CATEGORY);
        ColorDrawable colorDrawable = Utility.getColorCategory(context, category);

        switch (viewType) {
            case VIEW_TYPE_FAVOURITE: {
                ColorDrawable colorBar = Utility.getColorCategory(context, category);
                //((Activity) context).getActionBar().setBackgroundDrawable(colorBar);

                view.findViewById(R.id.list_item_category_color_layout)
                        .setBackgroundDrawable(colorDrawable);

                TextView categoryText = (TextView) view.findViewById(R.id.list_item_category_textview);
                String categoryTextStr = Utility.getCategoryFromJsonCat(context,
                        cursor.getString(LibraryFragment.COL_LIBRARY_CATEGORY));
                categoryText.setText(categoryTextStr);

                TextView votesText = (TextView) view.findViewById(R.id.list_item_votes_textview);
                Integer votes = cursor.getInt(LibraryFragment.COL_LIBRARY_VOTES);
                votesText.setText(votes.toString());

                final String storyId = cursor.getString(LibraryFragment.COL_LIBRARY_STORY_ID);
                ImageView imageVotesView = (ImageView) view.findViewById(R.id.list_like_icon);

                if (!Utility.storyVoted(context, storyId)) {
                    imageVotesView.setImageResource(R.drawable.ic_heart_outline);
                } else {
                    imageVotesView.setImageResource(R.drawable.ic_heart);
                }

                break;
            }
            case VIEW_TYPE_NORMAL_ENTRIES: {
                view.findViewById(R.id.list_item_category_color)
                        .setBackgroundDrawable(colorDrawable);
                break;
            }
        }

    }

    public void setUseDetailLayout(boolean useDetailLayout) {
        mUseDetailLayout = useDetailLayout;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mUseDetailLayout)
            return VIEW_TYPE_FAVOURITE;
        return VIEW_TYPE_NORMAL_ENTRIES;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
}
