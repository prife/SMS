package com.wetest.tookit.report;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.freeme.sms.Factory;
import com.freeme.sms.R;
import com.freeme.sms.model.SmsMessage;
import com.freeme.sms.util.PhoneUtils;
import com.wetest.tookit.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class RequestManager {
    private static final String TAG = "wetestsms";
    private static final String KEY_REPORT_URL = "def_report_url";

    private static final RequestManager myInstance = new RequestManager();

    public static RequestManager getInstance() {
        return myInstance;
    }

    public void makePostRequest(String url, String headerJson, String bodyJson, OnWtResponseListener faResponseListener) {
        WtRequest request = new WtRequest("POST", faResponseListener);
        request.setHeaderJson(headerJson);
        request.setBodyJson(bodyJson);
        request.execute(url);
    }

    public void makeGetRequest(String url, String headerJson, OnWtResponseListener faResponseListener) {
        WtRequest wtRequest = new WtRequest("GET", faResponseListener);
        wtRequest.setHeaderJson(headerJson);
        wtRequest.execute(url);
    }

    private static class WtRequest extends AsyncTask<String, Integer, WtResponse> {
        private final OnWtResponseListener mOnWtResponseListener;
        private final String mHttpMethod;
        private String mHeaderJson;
        private String mBodyJson;

        public WtRequest(String httpMethod, OnWtResponseListener faResponseListener) {
            mOnWtResponseListener = faResponseListener;
            mHttpMethod = httpMethod;
        }

        @Override
        protected WtResponse doInBackground(String... params) {
            WtResponse wtResponse = new WtResponse();
            InputStream inputStream = null;
            try {
                java.net.URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setDoInput(true);
                if (httpURLConnection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
                    ((HttpsURLConnection) httpURLConnection).setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
                }
                httpURLConnection.setRequestMethod(mHttpMethod);
                if (mHeaderJson != null && mHeaderJson.length() > 0) {
                    try {
                        JSONObject header = new JSONObject(mHeaderJson);
                        Iterator<String> iterator = header.keys();
                        while (iterator.hasNext()) {
                            String key = iterator.next();
                            String value = header.getString(key);
                            httpURLConnection.addRequestProperty(key, value);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    httpURLConnection.setRequestProperty("Content-Type", "application/json");
                }

                if (mHttpMethod.equals("POST") && mBodyJson != null) {
                    OutputStream os = httpURLConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(mBodyJson);
                    writer.flush();
                    writer.close();
                    os.close();
                }

                httpURLConnection.connect();
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == 200) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }
                String response = readInputStream(inputStream);
                wtResponse.setResponse(response);
                wtResponse.setCode(responseCode);
                return wtResponse;
            } catch (Exception e) {
                e.printStackTrace();
                wtResponse.setResponse(e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return wtResponse;
        }


        @Override
        protected void onPostExecute(WtResponse wtResponse) {
            super.onPostExecute(wtResponse);
            mOnWtResponseListener.onResponse(wtResponse);
        }

        private String readInputStream(InputStream stream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }

        public void setHeaderJson(String headerJson) {
            mHeaderJson = headerJson;
        }

        public void setBodyJson(String bodyJson) {
            mBodyJson = bodyJson;
        }

    }

    public interface OnWtResponseListener {
        void onResponse(WtResponse wtResponse);
    }

    public static class WtResponse {
        private int code = 0;
        private String response = "";

        public WtResponse() {
        }

        public int getCode() {
            return code;
        }

        public String getResponse() {
            return response;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String toString() {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Response", response);
                jsonObject.put("Code", code);
                return jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    public String makeJsonBody(String phonenumber, String textmsg) {
        Log.i(TAG, "phonenumber: " + phonenumber + " textmsg: " + textmsg);

        ReportSmsMsgRequest data = new ReportSmsMsgRequest();
        //data.setSecretId("ckdywKId9idmcPMJHUYsMPQ9qm");
        String secretkey = "ckdywKId9idmcPMJHUYsMPQ9qm";
        data.setSecretId("wetest_cloud");
        data.setT((int) (System.currentTimeMillis() / 1000));
        Random random = new Random(System.currentTimeMillis());
        int r = random.nextInt(1000000000);
        data.setR(r);
        ArrayList<String> arr = new ArrayList<>();
        arr.add(data.getSecretId());
        arr.add(secretkey);
        arr.add(Long.toString(data.getT()));
        arr.add(Long.toString(data.getR()));
        Collections.sort(arr);
        String tmp = "";
        for (int i = 0; i < arr.size(); i++) {
            tmp += arr.get(i);
        }
        Log.i(TAG, "param str: " + tmp);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException", e);
            return null;
        }

        byte[] bytes = digest.digest(tmp.getBytes());
        String sha1Str = byteArrayToHexString(bytes);
        Log.i(TAG, "sign str: " + sha1Str);
        data.setSign(sha1Str);
        data.setTime(data.getT());
        data.setPhoneNumber(phonenumber);
        data.setTextMsg(textmsg);
        JSONObject body = data.toJSON(ReportSmsMsgRequest.MSG_TYPE.NORMAL);
        Log.i(TAG, "post str: " + body.toString());
        Logger.getLogger().info("makeJsonBody: " + body.toString());
        return "secretid=" + data.getSecretId() +
                "&t=" + data.getT() +
                "&r=" + data.getR() +
                "&sign=" + data.getSign() +
                "&time=" + data.getTime() +
                "&phonenumber=" + URLEncoder.encode(data.getPhoneNumber()) +
                "&textmsg=" + URLEncoder.encode(data.getTextMsg());
    }

    private static String byteArrayToHexString(final byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static String getReportUrl(final boolean allowOverride) {
        String url = "";
        if (allowOverride) {
            url = Factory.get().getSmsPrefs().getString(KEY_REPORT_URL, url);
        }

        if (TextUtils.isEmpty(url)) {
            url = Factory.get().getApplicationContext().getString(R.string.def_server_number);
        }

        return url;
    }

    public static void saveReportUrl(String url) {
        Factory.get().getSmsPrefs().putString(KEY_REPORT_URL, url);
    }

    public void report(Context context, SmsMessage smsMessage) {
        String reportStr = makeJsonBody(PhoneUtils.get(
                smsMessage.mSubId).getSelfRawNumber(true), smsMessage.mBody);
        if (context == null || reportStr == null || reportStr.isEmpty()) {
            Logger.getLogger().error("error: context=" + context + "reportStr=" + reportStr);
            return;
        }

        String url = getReportUrl(true);
        Logger.getLogger().info("report to: " + url);
        makePostRequest(url, null, reportStr, new OnReportSmsWtResponseListener(context));
    }
}