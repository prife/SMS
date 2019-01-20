package com.wetest.tookit.report;

import com.wetest.tookit.log.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class ReportSmsMsgRequest {
    enum MSG_TYPE {
        NORMAL("normal"),
        HEARTBEAT("heartbeat"),;

        private String value;
        MSG_TYPE(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private String phoneNumber;
    private String textMsg;
    private String secretId;
    private float balance;
    private MSG_TYPE msgtype;

    // FIXME: ????
    private String sign;
    private int time;
    private int t;
    private int r;

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phonenumber) {
        this.phoneNumber = phonenumber;
    }

    public String getTextMsg() {
        return textMsg;
    }

    public void setTextMsg(String textMsg) {
        this.textMsg = textMsg;
    }

    JSONObject toJSON(MSG_TYPE type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("phonenumber", phoneNumber);
            jsonObject.put("textmsg", textMsg);
            jsonObject.put("balance", balance);
            jsonObject.put("msgtype", type.toString());

            // FIXME:???
            jsonObject.put("secretid", secretId);
            jsonObject.put("sign", sign);
            jsonObject.put("t", Integer.toString(t));
            jsonObject.put("r", Integer.toString(r));
            jsonObject.put("time", Integer.toString(time));
            return jsonObject;
        } catch (JSONException e) {
            Logger.getLogger().error("json error:", e);
        }

        return null;
    }

}
