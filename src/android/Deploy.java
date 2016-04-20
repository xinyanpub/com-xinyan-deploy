package com.xinyan;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.net.Uri;
import android.os.Environment;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Deploy extends CordovaPlugin {
    Context myContext = null;

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.myContext = this.cordova.getActivity().getApplicationContext();
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        final String url = args.getString(0);

        if (action.equals("download")) {
            logMessage("DOWNLOAD", "Downloading updates");
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    downloadUpdate(url, callbackContext);
                }
            });
            return true;
        } else if (action.equals("setup")) {
            logMessage("SETUP", "start");

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    setup(url, callbackContext);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private void downloadUpdate(String url, CallbackContext callbackContext) {
        final DownloadTask downloadTask = new DownloadTask(this.myContext, callbackContext);
        downloadTask.execute(url);
    }

    /**
     * Extract the downloaded archive
     *
     * @param zip
     * @param location
     */
    private void setup(String path, CallbackContext callbackContext) {
        logMessage("setup", "start");
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            callbackContext.error("DEPLOY_HTTP_ERROR");
        }

        String fileName = getFileName(url.getPath());
        File file = new File(Environment.getExternalStorageDirectory(),
                fileName);

        if (file.exists()) {
            logMessage("apk path:", Uri.fromFile(file).toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            this.cordova.getActivity().startActivity(intent);
            callbackContext.success("done");
            logMessage("setup", "Done");
        }
    }

    private void logMessage(String tag, String message) {
        if (true) {
            Log.i("Xinyan.DEPLOY." + tag, message);
        }
    }

    private String getFileName(String path) {
        int index = path.lastIndexOf('/');
        String fileName = path.substring(index);

        if (!fileName.endsWith(".apk")) {
            fileName = fileName + ".apk";
        }

        return fileName;
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context myContext;
        private CallbackContext callbackContext;

        public DownloadTask(Context context, CallbackContext callbackContext) {
            this.myContext = context;
            this.callbackContext = callbackContext;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                float fileLength = new Float(connection.getContentLength());

                logMessage("DOWNLOAD", "File size: " + fileLength);

                // download the file
                input = connection.getInputStream();

                //  output = this.myContext.openFileOutput("jzy.apk", Context.MODE_PRIVATE);
                String fileName = getFileName(url.getPath());
                logMessage("DOWNLOAD", "File name: " + fileName);

                File file = new File(Environment.getExternalStorageDirectory(),
                        fileName);
                output = new FileOutputStream(file);

                byte data[] = new byte[4096];
                float total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;

                    output.write(data, 0, count);

                    // Send the current download progress to a callback
                    if (fileLength > 0) {
                        float progress = (total / fileLength) * new Float("100.0f");
                        logMessage("DOWNLOAD", "Progress: " + (int) progress + "%");
                        PluginResult progressResult = new PluginResult(PluginResult.Status.OK, (int) progress);
                        progressResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(progressResult);
                    }
                }
            } catch (Exception e) {
                callbackContext.error("Something failed with the download...");
                logMessage("download", e.toString());
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {

                }

                if (connection != null)
                    connection.disconnect();
            }

            callbackContext.success("true");

            return null;
        }
    }
}
