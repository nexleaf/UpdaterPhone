package org.nexleaf.shared;

import edu.ucla.cens.Updater.utils.AppManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Shared utility functions, shared across all nexleaf projects.
 * Note: this class has to be instantiated after AppManager of this project has 
 * been instantiated first.
 */
public class SharedUtils {
    public static final String TAG = SharedUtils.class.getSimpleName();
    
    /**
     * Context to be used for operations such as getAssets
     */
    private static Context context;
    
    // init
    static {
        context = AppManager.get().getContext();        
    }

    /**
     * Queries current radio mode.
     * @return true if radio is on, false otherwise
     */
    public static boolean isRadioOn() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        boolean rc = (info != null);
        Log.d(TAG, "isRadioOn: returning " + rc);
        return rc;
    }

    /**
     * Sends a request for radio to be turned on.
     * The request is sent even if the state already seems to be as requested.
     * @param sender: application name of the app that is sending request
     * @param useCase: use case id. e.g. 1-1 for rebooter sending Test/Ping SMS
     * @param block: if true, blocks until radio is on or timeout occurs (invokes waitForRadioOn)
     * @param onInterval radio off delay/interval, in minutes, after which radio 
     *   will be turned off again. If 0, will use the default value.
     * @return true if radio appears to be already in the requested state, false
     *   otherwise
     */
    public static boolean requestRadioOn(String sender, String useCase, boolean block, int onIntervalMinutes) {
        Intent intent = new Intent();
        intent.setAction(SharedConstants.ACTION_RADIO_ON);
        intent.putExtra("sender", sender);
        intent.putExtra("useCase", useCase);
        if (onIntervalMinutes != 0) {
            intent.putExtra("onInterval", (long) (onIntervalMinutes * 60000L));
        }
        Log.d(TAG, "requestRadioOn: ACTION_RADIO_ON " + sender + ", " + useCase);
        context.sendBroadcast(intent);
        boolean rc = isRadioOn();
        if (!rc && block) {
            rc = waitForRadioOn();
        }
        return rc;
    }
    
    /**
     * Wait until the radio is on, or a timeout occurs.
     * This is approximate and will wait until SendReceiver service that controls
     * airplane mode etc. has had a chance to set radio on. 
     * @return radioOn status. Will be true in timeout didn't occur, false otherwise.
     */
    public static boolean waitForRadioOn() {
        // use continuations
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        long start = System.currentTimeMillis();
        while (true) {
            if (connManager.getActiveNetworkInfo() != null) {
                Log.d(TAG, "waitForRadioOn: radio turned on after  " + (System.currentTimeMillis() - start) + " ms.");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                break;
            }
            // check for timeout
            //Log.d(TAG, "waitForRadioOn: waiting for " + (System.currentTimeMillis() - start) + " ms.");
            if (System.currentTimeMillis() - start > SharedConstants.DEFAULT_WAIT_RADIO_ON) {
                Log.w(TAG, "waitForRadioOn: timed out after " + (System.currentTimeMillis() - start) + "ms.");
                break;
            }
        }
        return connManager.getActiveNetworkInfo() != null;
    }

    
    /**
     * Broadcasts a request for SMS to be sent.
     * @param sender: application name of the app that is sending request
     * @param useCase: use case id. e.g. 1-1 for rebooter sending Test/Ping SMS
     * @param phoneNumber:  phone number to send to
     * @param body:  body/text to send
     * @param expires: time, in minutes after which the request expires and won't be tried again
     */
    public static void requestSendSms(String sender, String useCase, String phoneNumber, String body, int expires) {
        Intent intent = new Intent();
        intent.setAction(SharedConstants.ACTION_SEND_SMS);
        intent.putExtra("sender", sender);
        intent.putExtra("useCase", useCase);
        intent.putExtra("phoneNumber", phoneNumber);
        intent.putExtra("body", body);
        intent.putExtra("expires", expires);
        Log.d(TAG, "requestSendSms: ACTION_SEND_SMS " + sender + ", " + useCase);
        context.sendBroadcast(intent);
    }    
    
}
