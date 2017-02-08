package com.writebook.writebook.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.writebook.writebook.data.WritebookContract.LibraryEntry;

/**
 * Created by Joel on 12/06/2015.
 */
public class WritebookDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 5;
    static final String DATABASE_NAME = "writebook.db";

    public WritebookDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_LIBRARY_TABLE = "CREATE TABLE " + LibraryEntry.TABLE_NAME + " (" +
                LibraryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LibraryEntry.COLUMN_ID_STORY + " TEXT UNIQUE NOT NULL, " +
                LibraryEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
                LibraryEntry.COLUMN_AUTHOR + " TEXT NOT NULL, " +
                LibraryEntry.COLUMN_CATEGORY + " TEXT NOT NULL, " +
                LibraryEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                LibraryEntry.COLUMN_TIME_READ + " INTEGER NOT NULL, " +
                LibraryEntry.COLUMN_VOTES + " INTEGER NOT NULL, "+
                LibraryEntry.COLUMN_STORY_BODY + " TEXT, " +
                LibraryEntry.COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                LibraryEntry.COLUMN_VOTED + " INTEGER DEFAULT 0 " +
                " );";

        db.execSQL(SQL_CREATE_LIBRARY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LibraryEntry.TABLE_NAME);
        onCreate(db);
    }
}
