package com.writebook.writebook.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.writebook.writebook.MainActivity;
import com.writebook.writebook.R;
import com.writebook.writebook.Utility;
import com.writebook.writebook.data.WritebookContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Vector;

/**
 * Created by Joel on 17/06/2015.
 */
public class WritebookSyncAdapter extends AbstractThreadedSyncAdapter{
    public static final String LOG_TAG = WritebookSyncAdapter.class.getSimpleName();
    public static final int SYNC_INTERVAL = 60 * 360;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final int WRITEBOOK_NOTIFICATION_ID = 3004;

    private static final String JSON_TITLE = "title";
    private static final String JSON_AUTHOR = "author";
    private static final String JSON_CATEGORY = "category";
    private static final String JSON_STORY_ID = "_id";
    private static final String JSON_DATE = "date";
    private static final String JSON_TIME_READ = "time_read";
    private static final String JSON_VOTES = "votes";
    private static final String JSON_STORY_BODY = "body";
    private static final String JSON_LANGUAGE = "language";

    public WritebookSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String libraryJsonData = null;

        boolean withError = false;
        String storyID = extras.getString(JSON_STORY_ID);

        try {
            final String WRITEBOOK_BASE_URL = "http://writebook-writebookapi.rhcloud.com/api";
            final String LIBRARY_WITH_STORY_AND_ID = "library";
            final String LIBRARY_TITLE_PATH = "libraryTitles";
            final String LIBRARY_ID_SAVED = Utility.getLastStoryIdSaved(getContext());

            Uri buildUri;
            boolean withStoryBody = false;
            if (storyID == null) {
                buildUri = Uri.parse(WRITEBOOK_BASE_URL).buildUpon()
                        .appendPath(LIBRARY_TITLE_PATH)
                        .appendPath(LIBRARY_ID_SAVED)
                        .build();
            }
            else {
                withStoryBody = true;

                buildUri = Uri.parse(WRITEBOOK_BASE_URL).buildUpon()
                        .appendPath(LIBRARY_WITH_STORY_AND_ID)
                        .appendPath(storyID)
                        .build();
            }

            URL url = new URL(buildUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null)
                return;
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                return;
            }

            libraryJsonData = buffer.toString();

            stringToJsonLibrary(libraryJsonData, withStoryBody,
                    extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL));

        } catch (SocketTimeoutException e) {
            withError = true;
            Log.e(LOG_TAG, "Error Timeout");
        }catch (IOException e) {
            withError = true;
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            withError = true;
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

            if (extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL))
                sendSyncBroadcast(getContext(), withError);
        }
    }

    private static void sendSyncBroadcast(Context context, boolean withError) {
        Intent intent;
        if (withError)
            intent = new Intent(context.getString(R.string.inmediatly_sync_fail));
        else
            intent = new Intent(context.getString(R.string.inmediatly_sync_finish));

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private void notifyNewStory(int nStoryInserted) {
        Context context = getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_new_story_notification_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_new_story_notifications_default)));

        if (displayNotifications && nStoryInserted > 0) {


            String notifyTitle = getContext().getString(R.string.new_story_notification_title);
            String notifyBody = Integer.toString(nStoryInserted) + " " +
                    getContext().getString(R.string.new_story_notification_message);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getContext())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle(notifyTitle)
                            .setContentText(notifyBody);

            Intent resultIntent = new Intent(context, MainActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager =
                    (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            mNotificationManager.notify(WRITEBOOK_NOTIFICATION_ID, mBuilder.build());

        }
    }

    private void stringToJsonLibrary(String data, boolean itemWithBody, boolean manualSync) throws JSONException{

        if (!itemWithBody) {
            JSONArray libraryJson = new JSONArray(data);
            Integer tam = libraryJson.length();
            Vector<ContentValues> contentVector = new Vector<ContentValues>(libraryJson.length());

            for (int i = 0; i < tam; i++) {
                JSONObject libEnt = libraryJson.getJSONObject(i);
                ContentValues libEntryValues = dataToContentValue(libEnt);
                contentVector.add(libEntryValues);
            }

            int inserted = 0;
            if (contentVector.size() > 0) {
                ContentValues[] cvArray = new ContentValues[contentVector.size()];
                contentVector.toArray(cvArray);
                inserted = getContext().getContentResolver().bulkInsert(WritebookContract.LibraryEntry.CONTENT_URI, cvArray);
                removeOldData();
            }

            if (!manualSync)
                notifyNewStory(inserted);

        }
        else {
            JSONObject libEnt = new JSONObject(data);
            ContentValues libEntryValues = dataToContentValue(libEnt);

            Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(libEnt.getString(JSON_STORY_ID));
            getContext().getContentResolver().update(uri, libEntryValues, null, null);
        }
    }

    private ContentValues dataToContentValue(JSONObject libEnt) throws JSONException {
        String storyID = libEnt.getString(JSON_STORY_ID);
        String title = libEnt.getString(JSON_TITLE);
        String author = libEnt.getString(JSON_AUTHOR);
        String category = libEnt.getString(JSON_CATEGORY);
        Long date = libEnt.getLong(JSON_DATE);
        Integer time_read = libEnt.getInt(JSON_TIME_READ);
        Integer votes = libEnt.getInt(JSON_VOTES);
        String language = libEnt.getString(JSON_LANGUAGE);

        String storyBody = "";
        if (libEnt.has(JSON_STORY_BODY))
            storyBody = libEnt.getString(JSON_STORY_BODY);

        ContentValues libEntryValues = new ContentValues();
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_ID_STORY, storyID);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_TITLE, title);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_AUTHOR, author);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_CATEGORY, category);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_DATE, date);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_TIME_READ, time_read);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_VOTES, votes);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_STORY_BODY, storyBody);
        libEntryValues.put(WritebookContract.LibraryEntry.COLUMN_LANGUAGE, language);

        return libEntryValues;
    }

    private void removeOldData() {
        int limitDatabase = Utility.getMaxSavedStories(getContext());
        String sortOrder = WritebookContract.LibraryEntry.COLUMN_ID_STORY + " ASC";
        String projection[] = {WritebookContract.LibraryEntry.COLUMN_ID_STORY};
        Uri uri = WritebookContract.LibraryEntry.buildLibraryTitlesUri();

        Cursor cursor = getContext().getContentResolver().query(
                uri,
                projection,
                null,
                null,
                sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            int numEntries = cursor.getCount();
            int numEntriesToRemove = numEntries - limitDatabase;

            String selectEntryToRemove = WritebookContract.LibraryEntry.TABLE_NAME +
                    "." + WritebookContract.LibraryEntry.COLUMN_ID_STORY + " = ? " ;

            boolean existNext = true;
            while (numEntriesToRemove > 0 && existNext) {
                int position = cursor.getColumnIndex(WritebookContract.LibraryEntry.COLUMN_ID_STORY);
                String storyId = cursor.getString(position);

                int removed = getContext().getContentResolver().delete(
                        uri,
                        selectEntryToRemove,
                        new String[]{storyId}
                );

                numEntriesToRemove--;
                existNext = cursor.moveToNext();
            }
        }
        cursor.close();
    }

    private static boolean existStoryBodyDatabase(Context contecxt, String storyID) {
        boolean existData = false;
        Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyID);
        Cursor cursor = contecxt.getContentResolver().query(
                uri,
                null,
                null,
                null,
                null);

        if (cursor.moveToFirst()) {
            int dataIdIndex = cursor.getColumnIndex(
                    WritebookContract.LibraryEntry.COLUMN_STORY_BODY);
            String tmpStr = cursor.getString(dataIdIndex);

            if (!tmpStr.equals(""))
                existData = true;
        }
        cursor.close();

        return existData;
    }




    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }

    }


    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);

        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static void syncImmediatelyWithBodyStory(Context context, String storyID) {

        if (!existStoryBodyDatabase(context, storyID)) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putString(JSON_STORY_ID, storyID);

            ContentResolver.requestSync(getSyncAccount(context),
                    context.getString(R.string.content_authority), bundle);
        }
        else {
            sendSyncBroadcast(context, false);
        }

    }

    public static Account getSyncAccount(Context context) {

        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        if ( null == accountManager.getPassword(newAccount) ) {
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {

        WritebookSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
