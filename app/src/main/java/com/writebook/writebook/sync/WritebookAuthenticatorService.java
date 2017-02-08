package com.writebook.writebook.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Joel on 17/06/2015.
 */
public class WritebookAuthenticatorService extends Service {
    private WritebookAuthenticator mWritebookAuthenticator;

    @Override
    public void onCreate() {
        mWritebookAuthenticator = new WritebookAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mWritebookAuthenticator.getIBinder();
    }
}
