package com.example.broadcastapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {
    private static final int DOWNLOAD_LOADER = 1;
    private static final String DOWNLOAD_URL_KEY = "com.example.broadcastapp.download_url";
    // 1. Buat konstanta utk action broadcast custom-nya
    private static final String ACTION_DOWNLOAD_COMPLETED = "com.example.broadcastapp.download_completed";

    private TextView tvMessage;
    // 2. Buat field utk menyimpan object penerima broadcast-nya
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMessage = findViewById(R.id.tv_message);

        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();

        if (info != null && info.isConnected()) {
            tvMessage.setText("Koneksi jaringan tersedia");
            // download file.
        } else {
            tvMessage.setText("Tidak ada koneksi jaringan");
        }

        /*
        IntentFilter filter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
        PowerStatusReceiver receiver = new PowerStatusReceiver();
        registerReceiver(receiver, filter);
        */

        // 3. inisialisasi object penerima broadcast.
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 4. ambil dan cek kesesuaian action-nya.
                String action = intent.getAction();
                if (action.equals(ACTION_DOWNLOAD_COMPLETED)) {
                    // 5. Lakukan apa yang akan dikerjakan saat menerima broadcast.
                    Toast.makeText(context, "Download is completed", Toast.LENGTH_SHORT).show();
                }
            }
        };
        // 6. Daftarkan object penerima broadcast itu.
        registerReceiver(receiver, new IntentFilter(ACTION_DOWNLOAD_COMPLETED));
    }

    @Override
    protected void onDestroy() {
        // 7. Jangan lupa utk menghapus pendaftaran saat activity ditutup.
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncTaskLoader<String>(this) {
            @Nullable
            @Override
            public String loadInBackground() {
                if (args == null) return null;
                String url = args.getString(DOWNLOAD_URL_KEY);
                try {
                    URL reqURL = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) reqURL.openConnection();
                    InputStream stream = null;
                    try {
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(10000);
                        conn.setRequestMethod("GET");
                        conn.setDoInput(true);
                        conn.connect();
                        int response = conn.getResponseCode();
                        stream = conn.getInputStream();
                        String result = convertStreamToString(stream);
                        // 8. Tulis kode utk mengirim broadcast-nya.
                        sendBroadcast(new Intent(ACTION_DOWNLOAD_COMPLETED));
                        return result;
                    } finally {
                        conn.disconnect();
                        if (stream != null) stream.close();
                    }
                } catch (MalformedURLException e) {
                    tvMessage.setText("URL yang diberikan keliru");
                } catch (IOException e) {
                    tvMessage.setText("Koneksi gagal dilakukan");
                }
                return null;
            }

            @Override
            protected void onStartLoading() {
                forceLoad();
            }

            private String convertStreamToString(InputStream stream)
                    throws IOException, UnsupportedEncodingException {
                StringBuilder builder = new StringBuilder();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                if (builder.length() == 0) return null;
                return builder.toString();
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {
        if (data == null) return;
        tvMessage.setText(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    private void downloadURL(String url) {
        Bundle args = new Bundle();
        args.putString(DOWNLOAD_URL_KEY, url);

        LoaderManager manager = LoaderManager.getInstance(this);
        Loader<String> loader = manager.getLoader(DOWNLOAD_LOADER);

        if (loader == null) {
            manager.initLoader(DOWNLOAD_LOADER, args, this);
        } else {
            manager.restartLoader(DOWNLOAD_LOADER, args, this);
        }
    }

    public void downloadClick(View v) {
        EditText edtURL = findViewById(R.id.edt_url);
        if (edtURL.getText().length() == 0) {
            tvMessage.setText("URL masih kosong");
            return;
        }
        String url = edtURL.getText().toString();
        downloadURL(url);
    }
}