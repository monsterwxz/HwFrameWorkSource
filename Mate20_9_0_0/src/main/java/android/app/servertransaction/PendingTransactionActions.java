package android.app.servertransaction;

import android.app.ActivityManager;
import android.app.ActivityThread.ActivityClientRecord;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Log;
import android.util.LogWriter;
import com.android.internal.util.IndentingPrintWriter;

public class PendingTransactionActions {
    private boolean mCallOnPostCreate;
    private Bundle mOldState;
    private boolean mReportRelaunchToWM;
    private boolean mRestoreInstanceState;
    private StopInfo mStopInfo;

    public static class StopInfo implements Runnable {
        private static final String TAG = "ActivityStopInfo";
        private ActivityClientRecord mActivity;
        private CharSequence mDescription;
        private PersistableBundle mPersistentState;
        private Bundle mState;

        public void setActivity(ActivityClientRecord activity) {
            this.mActivity = activity;
        }

        public void setState(Bundle state) {
            this.mState = state;
        }

        public void setPersistentState(PersistableBundle persistentState) {
            this.mPersistentState = persistentState;
        }

        public void setDescription(CharSequence description) {
            this.mDescription = description;
        }

        public void run() {
            try {
                ActivityManager.getService().activityStopped(this.mActivity.token, this.mState, this.mPersistentState, this.mDescription);
            } catch (RemoteException ex) {
                IndentingPrintWriter pw = new IndentingPrintWriter(new LogWriter(5, TAG), "  ");
                pw.println("Bundle stats:");
                Bundle.dumpStats(pw, this.mState);
                pw.println("PersistableBundle stats:");
                Bundle.dumpStats(pw, this.mPersistentState);
                if (!(ex instanceof TransactionTooLargeException) || this.mActivity.packageInfo.getTargetSdkVersion() >= 24) {
                    throw ex.rethrowFromSystemServer();
                }
                Log.e(TAG, "App sent too much data in instance state, so it was ignored", ex);
            }
        }
    }

    public PendingTransactionActions() {
        clear();
    }

    public void clear() {
        this.mRestoreInstanceState = false;
        this.mCallOnPostCreate = false;
        this.mOldState = null;
        this.mStopInfo = null;
    }

    public boolean shouldRestoreInstanceState() {
        return this.mRestoreInstanceState;
    }

    public void setRestoreInstanceState(boolean restoreInstanceState) {
        this.mRestoreInstanceState = restoreInstanceState;
    }

    public boolean shouldCallOnPostCreate() {
        return this.mCallOnPostCreate;
    }

    public void setCallOnPostCreate(boolean callOnPostCreate) {
        this.mCallOnPostCreate = callOnPostCreate;
    }

    public Bundle getOldState() {
        return this.mOldState;
    }

    public void setOldState(Bundle oldState) {
        this.mOldState = oldState;
    }

    public StopInfo getStopInfo() {
        return this.mStopInfo;
    }

    public void setStopInfo(StopInfo stopInfo) {
        this.mStopInfo = stopInfo;
    }

    public boolean shouldReportRelaunchToWindowManager() {
        return this.mReportRelaunchToWM;
    }

    public void setReportRelaunchToWindowManager(boolean reportToWm) {
        this.mReportRelaunchToWM = reportToWm;
    }
}
