package it.andreafilippi.whatsnearme.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BtReceiverTask extends AsyncTask<Void, Void, String> {

    private Context context;
    private BluetoothAdapter btAdapter;
    private BluetoothServerSocket bss;
    private BluetoothSocket bs;
    private String btConnectionName;
    private UUID btConnectionUUID;
    private AlertDialog dialog;
    private ProgressBar progressBar;
    private TextView messageTextView;

    public BtReceiverTask(Context context,
                          BluetoothAdapter btAdapter,
                          String btConnectionName,
                          UUID btConnectionUUID,
                          AlertDialog dialog, ProgressBar progressBar, TextView messageTextView) {
        this.context = context;
        this.btAdapter = btAdapter;
        this.btConnectionName = btConnectionName;
        this.btConnectionUUID = btConnectionUUID;
        this.dialog = dialog;
        this.progressBar = progressBar;
        this.messageTextView = messageTextView;
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

        progressBar.setVisibility(View.GONE);
        if (result != null) {
            dialog.setTitle("Link ricevuto!");

            // stile di un link
            SpannableString spannableString = new SpannableString(result);
            spannableString.setSpan(new UnderlineSpan(), 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 0, result.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageTextView.setText(spannableString);
            messageTextView.setVisibility(View.VISIBLE);

            messageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messageTextView.getText().toString()));
                    intent.setPackage(null);
                    context.startActivity(intent);
                }
            });
            messageTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Link Maps", result);
                    clipboard.setPrimaryClip(clip);
                    Utils.makeToastShort(context, "Messaggio copiato negli appunti");

                    return true;
                }
            });
        } else {
            dialog.setTitle("Errore");
            messageTextView.setText("Errore nella ricezione del messaggio.");
            messageTextView.setVisibility(View.VISIBLE);
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

        progressBar.setVisibility(View.GONE);
        messageTextView.setText("Connessione annullata.");
        messageTextView.setVisibility(View.VISIBLE);
    }
}
