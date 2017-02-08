package com.writebook.writebook.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by Joel on 12/06/2015.
 */
public class WritebookContract {

    public static final String CONTENT_AUTHORITY = "com.writebook.writebook";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_LIBRARY = "library";
    public static final String STORY_BODY = "story_body";
    public static final String LAST_STORY_ID = "last_story_id";

    public static long normalizeDate(long startDate) {
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }


    public static final class LibraryEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_LIBRARY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LIBRARY;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LIBRARY;

        // Table name
        public static final String TABLE_NAME = "library";

        public static final String COLUMN_ID_STORY = "id_story";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TIME_READ = "time_read";
        public static final String COLUMN_VOTES = "votes";
        public static final String COLUMN_STORY_BODY = "story_body";
        public static final String COLUMN_LANGUAGE = "language";
        public static final String COLUMN_VOTED = "voted";

        public static Uri buildLibraryUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildLibraryTitlesUri() {
            return CONTENT_URI;
        }

        public static Uri buildLibraryWithStoryID(String storyID) {
            return CONTENT_URI.buildUpon()
                    .appendPath(STORY_BODY)
                    .appendPath(storyID).build();
        }

        public static Uri buildLastStoryIdUri() {
            return CONTENT_URI.buildUpon()
                    .appendPath(LAST_STORY_ID).build();
        }

        public static String getStoryID(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }
}
