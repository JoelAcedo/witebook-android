package com.writebook.writebook.auth;

import android.app.Activity;

import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.writebook.writebook.R;

import java.io.InputStream;
import java.net.URL;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 0;
    private GoogleApiClient mGoogleApiClient;
    private boolean mIntentInProgress;
    private boolean mIsResolving = false;
    private boolean mShouldResolve = false;

    private SignInButton mSignInButton;
    private ConnectionResult mConnectionResult;
    private boolean mSignedInUser;
    private LinearLayout profileFrame, signInFrame;
    private TextView profileUser, profileEmail;
    private ImageView profileImage;

    //Facebookc
    private CallbackManager mCallbackManager;
    private LoginButton mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_login);

        //Facebook
        mCallbackManager = CallbackManager.Factory.create();
        mLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        mLoginButton.setBackgroundResource(R.drawable.com_facebook_button_background);
        mLoginButton.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
        mLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException e) {

            }
        });



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .build();

        mSignInButton = (SignInButton) findViewById(R.id.google_signin_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "Google sign in clicked");
                if (view.getId() == R.id.google_signin_button)
                    googlePlusLogin();
            }
        });

        profileFrame = (LinearLayout) findViewById(R.id.profile_frame);
        signInFrame = (LinearLayout) findViewById(R.id.signin_frame);
        profileUser = (TextView) findViewById(R.id.profile_user);
        profileEmail = (TextView) findViewById(R.id.profile_email);
        profileImage = (ImageView) findViewById(R.id.profile_image);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    protected void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0)
                    .show();
            return;
        }

        if (!mIntentInProgress) {
            mConnectionResult = connectionResult;
            if (mSignedInUser)
                resolveSignInError();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        if (requestCode == RC_SIGN_IN) {
            if (responseCode == RESULT_OK)
                mSignedInUser = false;
            mIntentInProgress = false;
            if (!mGoogleApiClient.isConnecting())
                mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mSignedInUser = false;
        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
        getProfileInformation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    private void updateProfile(boolean isSignedIn) {
        if (isSignedIn) {
            signInFrame.setVisibility(View.GONE);
            profileFrame.setVisibility(View.VISIBLE);
        } else {
            signInFrame.setVisibility(View.VISIBLE);
            profileFrame.setVisibility(View.GONE);
        }
    }

    private void getProfileInformation() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);

                profileUser.setText(personName);
                profileEmail.setText(email);

                new LoadProfileImage(profileImage).execute(personPhotoUrl);
                updateProfile(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void googlePlusLogin() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignedInUser = true;
            resolveSignInError();
        }
    }

    private void googlePlusLogout() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            updateProfile(false);
        }
    }

    private void signIn(View view) {
        googlePlusLogin();
    }

    public void logout(View view) {
        googlePlusLogout();
    }

    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView downloadedImage;

        public LoadProfileImage(ImageView imageView) {
            this.downloadedImage = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            Uri uri = Uri.parse(imageUrl).buildUpon()
                    .appendQueryParameter("sz", "320")
                    .build();

            Bitmap icon = null;
            try {
                URL url = new URL(uri.toString());
                InputStream inputStream = url.openStream();
                icon = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return icon;
        }

        protected void onPostExecute(Bitmap result) {
            downloadedImage.setImageBitmap(result);
        }
    }

}

