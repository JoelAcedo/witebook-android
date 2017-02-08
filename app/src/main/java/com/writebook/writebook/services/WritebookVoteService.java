package com.writebook.writebook.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.writebook.writebook.data.WritebookContract;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class WritebookVoteService extends IntentService {
    public static final String LOG_TAG =WritebookVoteService.class.getSimpleName();
    private static final String ACTION_VOTE = "com.writebook.writebook.services.action.VOTE";
    private static final String ACTION_UNVOTE = "com.writebook.writebook.services.action.UNVOTE";

    private static final String EXTRA_STORY_ID = "com.writebook.writebook.services.extra.STORY_ID";

    public static void startActionVote(Context context, String storyId) {
        Intent intent = new Intent(context, WritebookVoteService.class);
        intent.setAction(ACTION_VOTE);
        intent.putExtra(EXTRA_STORY_ID, storyId);

        context.startService(intent);
    }

    public static void startActionUnvote(Context context, String storyId) {
        Intent intent = new Intent(context, WritebookVoteService.class);
        intent.setAction(ACTION_UNVOTE);
        intent.putExtra(EXTRA_STORY_ID, storyId);

        context.startService(intent);
    }

    public WritebookVoteService() {
        super("WritebookVoteService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_VOTE.equals(action)) {
                String storyId = intent.getStringExtra(EXTRA_STORY_ID);
                handleActionVote(storyId, true);
            } else if (ACTION_UNVOTE.equals(action)) {
                String storyId = intent.getStringExtra(EXTRA_STORY_ID);
                handleActionVote(storyId, false);
            }
        }
    }

    private void handleActionVote(String storyId, boolean vote) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String voteJsonData = null;

        try {
            final String WRITEBOOK_BASE_URL = "http://writebook-writebookapi.rhcloud.com/api";
            final String LIBRARY_WITH_STORY_AND_ID = "library";
            final String VOTE_URL = "vote";
            final String UNVOTE_URL = "unvote";

            Uri buildUri;

            if (vote) {
                buildUri = Uri.parse(WRITEBOOK_BASE_URL).buildUpon()
                        .appendPath(LIBRARY_WITH_STORY_AND_ID)
                        .appendPath(storyId)
                        .appendPath(VOTE_URL)
                        .build();
            } else {
                buildUri = Uri.parse(WRITEBOOK_BASE_URL).buildUpon()
                        .appendPath(LIBRARY_WITH_STORY_AND_ID)
                        .appendPath(storyId)
                        .appendPath(UNVOTE_URL)
                        .build();
            }

            URL url = new URL(buildUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoInput(true);
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

            voteJsonData = buffer.toString();

            stringToJsonVote(voteJsonData, storyId);

        } catch (SocketTimeoutException e) {
            Log.e(LOG_TAG, "Error Timeout");
        }catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static final String JSON_VOTES = "votes";

    private void stringToJsonVote(String data, String storyId) throws JSONException {

        JSONObject jsonObject = new JSONObject(data);
        Integer votes = jsonObject.getInt(JSON_VOTES);

        Uri uri = WritebookContract.LibraryEntry.buildLibraryWithStoryID(storyId);

        ContentValues values = new ContentValues();
        values.put(WritebookContract.LibraryEntry.COLUMN_VOTES, votes);

        getContentResolver().update(
                uri,
                values,
                null,
                null);
    }

}
