package com.writebook.writebook;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.writebook.writebook.data.WritebookContract;

/**
 * Created by Joel on 11/06/2015.
 */
public class Utility {
    public static final String JSON_FANTASY = "fantasy";
    public static final String JSON_ROMANTIC = "romantic";
    public static final String JSON_HISTORIC = "historic";
    public static final String JSON_DRAMA = "drama";
    public static final String JSON_FICTION = "fiction";
    public static final String JSON_REAL = "real";
    public static final String JSON_HORROR = "horror";
    public static final String JSON_HUMOR = "humor";
    public static final String JSON_SUSPENSE =  "suspense";


    public static String getCategoryFromJsonCat(Context context, String cat) {
        switch (cat) {
            case JSON_FANTASY:
                return context.getResources().getString(R.string.cat_fantasy);
            case JSON_ROMANTIC:
                return context.getResources().getString(R.string.cat_romantic);
            case JSON_HISTORIC:
                return context.getResources().getString(R.string.cat_history);
            case JSON_DRAMA:
                return context.getResources().getString(R.string.cat_drama);
            case JSON_FICTION:
                return context.getResources().getString(R.string.cat_fiction);
            case JSON_REAL:
                return context.getResources().getString(R.string.cat_real);
            case JSON_HORROR:
                return context.getResources().getString(R.string.cat_horror);
            case JSON_HUMOR:
                return context.getResources().getString(R.string.cat_humor);
            case JSON_SUSPENSE:
                return context.getResources().getString(R.string.cat_suspense);
            default:
                return context.getResources().getString(R.string.cat_other);
        }
    }

    public static ColorDrawable getColorCategory(Context context, String cat) {

        switch (cat) {
            case JSON_FANTASY:
                return new ColorDrawable(context.getResources().getColor(R.color.deep_purple_300));
            case JSON_ROMANTIC:
                return new ColorDrawable(context.getResources().getColor(R.color.red_400));
            case JSON_HISTORIC:
                return new ColorDrawable(context.getResources().getColor(R.color.brown_400));
            case JSON_DRAMA:
                return new ColorDrawable(context.getResources().getColor(R.color.green_A400));
            case JSON_FICTION:
                return new ColorDrawable(context.getResources().getColor(R.color.blue_400));
            case JSON_REAL:
                return new ColorDrawable(context.getResources().getColor(R.color.orange_400));
            case JSON_HORROR:
                return new ColorDrawable(context.getResources().getColor(R.color.grey_400));
            case JSON_HUMOR:
                return new ColorDrawable(context.getResources().getColor(R.color.yellow_400));
            case JSON_SUSPENSE:
                return new ColorDrawable(context.getResources().getColor(R.color.blue_grey_400));
            default:
                return new ColorDrawable(context.getResources().getColor(R.color.purple_400));
        }
    }

    public static Float getStoryTextSize(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String textSize = prefs.getString(context.getString(R.string.pref_text_size_key),
                context.getString(R.string.pref_text_size_default));

        return Float.valueOf(textSize);
    }

    public static String getStoryLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String storyLang = prefs.getString(context.getString(R.string.pref_select_language_stories_key),
                context.getString(R.string.pref_select_language_default));
        return storyLang;
    }

    public static final String DEFAULT_LAST_STORY_ID = "000000000000000000000000";

    public static String getLastStoryIdSaved(Context context) {
        String sortOrder = WritebookContract.LibraryEntry.COLUMN_ID_STORY + " DESC";
        Uri libraryUri = WritebookContract.LibraryEntry.buildLastStoryIdUri();
        String projection[] = {WritebookContract.LibraryEntry.COLUMN_ID_STORY};

        Cursor cursor = context.getContentResolver().query(
                libraryUri,
                projection,
                null,
                null,
                sortOrder
        );

        String lastStoryId = DEFAULT_LAST_STORY_ID;

        if (cursor.moveToFirst()) {
            int indexCursor = cursor.getColumnIndex(WritebookContract.LibraryEntry.COLUMN_ID_STORY);
            lastStoryId = cursor.getString(indexCursor);
        }

        cursor.close();

        return lastStoryId;
    }

    public static String getLastStoryIdSavedWithSelectedLanguage(Context context) {
        String sortOrder = WritebookContract.LibraryEntry.COLUMN_ID_STORY + " DESC";
        Uri libraryUri = WritebookContract.LibraryEntry.buildLastStoryIdUri();
        String projection[] = {WritebookContract.LibraryEntry.COLUMN_ID_STORY};
        String language = Utility.getStoryLanguage(context);
        String sStoryWithLanguage = WritebookContract.LibraryEntry.TABLE_NAME +
                "." + WritebookContract.LibraryEntry.COLUMN_LANGUAGE + " = ? " ;

        Cursor cursor = null;
        if (language.equals("any")) {
            cursor = context.getContentResolver().query(
                    libraryUri,
                    projection,
                    null,
                    null,
                    sortOrder
            );
        } else {
            cursor = context.getContentResolver().query(
                    libraryUri,
                    projection,
                    sStoryWithLanguage,
                    new String[]{language},
                    sortOrder
            );
        }

        String lastStoryId = DEFAULT_LAST_STORY_ID;

        if (cursor != null && cursor.moveToFirst()) {
            int indexCursor = cursor.getColumnIndex(WritebookContract.LibraryEntry.COLUMN_ID_STORY);
            lastStoryId = cursor.getString(indexCursor);

            cursor.close();
        }

        return lastStoryId;
    }

    public static boolean storyVoted(Context context, String storyId) {
        Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyId);
        String projection[] = {WritebookContract.LibraryEntry.COLUMN_VOTED};

        boolean voted = false;

        Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int indexCursor = cursor.getColumnIndex(WritebookContract.LibraryEntry.COLUMN_VOTED);
            int votedInteger = cursor.getInt(indexCursor);

            if (votedInteger != 0)
                voted = true;

            cursor.close();
        }

        return voted;
    }


    public static void setStoryVoted(Context context, String storyId, boolean vote) {
        Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyId);

        ContentValues values = new ContentValues();
        if (vote)
            values.put(WritebookContract.LibraryEntry.COLUMN_VOTED, 1);
        else
            values.put(WritebookContract.LibraryEntry.COLUMN_VOTED, 0);

        context.getContentResolver().update(
                uri,
                values,
                null,
                null);
    }

    public static int getMaxSavedStories(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String maxSavedStoriesStr = prefs.getString(context.getString(R.string.pref_max_stories_saved_key),
                context.getString(R.string.pref_max_stories_saved_default));
        int maxSavedStories = Integer.valueOf(maxSavedStoriesStr);
        return maxSavedStories;
    }
}
