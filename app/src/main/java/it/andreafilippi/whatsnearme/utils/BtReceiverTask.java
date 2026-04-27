package it.andreafilippi.whatsnearme.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BtReceiverTask extends AsyncTask<Void, Integer, String> {

    public interface BtCallback {
        void onUpdate(String msg);
        void onProgress(int p);
    }

    private final BluetoothAdapter btAdapter;
    private BluetoothServerSocket bss;
    private BluetoothSocket bs;
    private final String btConnectionName;
    private final UUID btConnectionUUID;
    private final WeakReference<BtCallback> callbackRef;

    public BtReceiverTask(BluetoothAdapter btAdapter,
                          String btConnectionName,
                          UUID btConnectionUUID,
                          BtCallback callback) {
        this.btAdapter = btAdapter;
        this.btConnectionName = btConnectionName;
        this.btConnectionUUID = btConnectionUUID;
        this.callbackRef = new WeakReference<>(callback);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected String doInBackground(Void... voids) {
        String readMessage = null;
        try {
            bss = btAdapter.listenUsingRfcommWithServiceRecord(btConnectionName, btConnectionUUID);

            bs = null;
            while (!isCancelled()) {
                try {
                    bs = bss.accept(3000);
                    break;
                } catch (IOException ignored) {}
            }
            if (isCancelled()) return null;
            bss.close();
            try {
                InputStream is = bs.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes = is.read(buffer);
                readMessage = new String(buffer, 0, bytes);
                Log.d("BT CONNECTION", readMessage);

                bs.getOutputStream().write(0);

                bs.close();
            } catch (IOException e) {
                Log.e("BT CONNECTION", e.toString());
            }
        } catch (IOException e) {
            Log.e("BT CONNECTION", e.toString());
        }

        return readMessage;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        BtCallback callback = callbackRef.get();
        if (callback != null) {
            callback.onProgress(100);
            callback.onUpdate(result);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        if (bss != null) {
            try {
                bss.close();
            } catch (IOException e) {}
        }
        if (bs != null) {
            try {
                bs.close();
            } catch (IOException e) {}
        }

        BtCallback callback = callbackRef.get();
        if (callback != null) {
            callback.onProgress(100);
            callback.onUpdate("CANCELLED");
        }
    }
}
