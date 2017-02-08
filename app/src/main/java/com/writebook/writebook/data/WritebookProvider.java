package com.writebook.writebook.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by Joel on 12/06/2015.
 */
public class WritebookProvider extends ContentProvider {
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private WritebookDbHelper mOpenHelper;

    private static final int LIBRARY = 100;
    private static final int LIBRARY_WITH_STORY_ID = 101;
    private static final int LAST_STORY_ID = 102;

    private static final String sStoryWithID =
            WritebookContract.LibraryEntry.TABLE_NAME +
                    "." + WritebookContract.LibraryEntry.COLUMN_ID_STORY + " = ? " ;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = WritebookContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, WritebookContract.PATH_LIBRARY, LIBRARY);
        matcher.addURI(authority, WritebookContract.PATH_LIBRARY + "/" + WritebookContract.STORY_BODY + "/*",
                LIBRARY_WITH_STORY_ID);
        matcher.addURI(authority, WritebookContract.PATH_LIBRARY + "/" + WritebookContract.LAST_STORY_ID,
                LAST_STORY_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new WritebookDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case LIBRARY:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WritebookContract.LibraryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case LIBRARY_WITH_STORY_ID:
                String storyID = WritebookContract.LibraryEntry.getStoryID(uri);

                retCursor = mOpenHelper.getReadableDatabase().query(
                        WritebookContract.LibraryEntry.TABLE_NAME,
                        projection,
                        sStoryWithID,
                        new String[]{storyID},
                        null,
                        null,
                        sortOrder
                );
                break;
            case LAST_STORY_ID:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        WritebookContract.LibraryEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder,
                        "1"
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case LIBRARY:
                return WritebookContract.LibraryEntry.CONTENT_TYPE;
            case LIBRARY_WITH_STORY_ID:
                return WritebookContract.LibraryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case LIBRARY:
                normalizeDate(values);
                long _id = db.insert(WritebookContract.LibraryEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = WritebookContract.LibraryEntry.buildLibraryUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        if ( null == selection ) selection = "1";
        switch (match) {
            case LIBRARY:
                rowsDeleted = db.delete(WritebookContract.LibraryEntry.TABLE_NAME,
                        selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        if ( null == selection ) selection = "1";
        switch (match) {
            case LIBRARY:
                normalizeDate(values);
                rowsUpdated = db.update(WritebookContract.LibraryEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case LIBRARY_WITH_STORY_ID:
                String storyID = WritebookContract.LibraryEntry.getStoryID(uri);

                normalizeDate(values);
                rowsUpdated = db.update(WritebookContract.LibraryEntry.TABLE_NAME,
                        values,
                        sStoryWithID,
                        new String[]{storyID});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LIBRARY:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        long _id = db.insert(WritebookContract.LibraryEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }


    private void normalizeDate(ContentValues values) {
        if (values.containsKey(WritebookContract.LibraryEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WritebookContract.LibraryEntry.COLUMN_DATE);
            values.put(WritebookContract.LibraryEntry.COLUMN_DATE, WritebookContract.normalizeDate(dateValue));
        }
    }
}
