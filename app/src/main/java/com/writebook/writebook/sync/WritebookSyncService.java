package com.writebook.writebook.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Joel on 17/06/2015.
 */
public class WritebookSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static WritebookSyncAdapter sWritebookSyncAdapter;

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sWritebookSyncAdapter == null) {
                sWritebookSyncAdapter = new WritebookSyncAdapter(getApplicationContext(),
                        true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sWritebookSyncAdapter.getSyncAdapterBinder();
    }
}
