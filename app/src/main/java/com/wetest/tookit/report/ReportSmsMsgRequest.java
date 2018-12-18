package com.wetest.tookit.report;

import org.json.JSONObject;

public class ReportSmsMsgRequest {
    private String secretid;
    private String sign;
    private int t;
    private int r;
    private int time;
    private String phonenumber;
    private String textmsg;

    public String getSecretid() {
        return secretid;
    }
    public void setSecretid(String secretid) {
        this.secretid = secretid;
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
//    public int getTestid() {
//        return testid;
//    }
//    public void setTestid(int testid) {
//        this.testid = testid;
//    }
//    public int getDeviceid() {
//        return deviceid;
//    }
//    public void setDeviceid(int deviceid) {
//        this.deviceid = deviceid;
//    }
    public int getTime() {
        return time;
    }
    public void setTime(int time) {
        this.time = time;
    }
//    public int getErrorcode() {
//        return errorcode;
//    }
//    public void setErrorcode(int errorcode) {
//        this.errorcode = errorcode;
//    }
//    public String getErrormsg() {
//        return errormsg;
//    }
//    public void setErrormsg(String errormsg) {
//        this.errormsg = errormsg;
//    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getTextmsg() {
        return textmsg;
    }

    public void setTextmsg(String textmsg) {
        this.textmsg = textmsg;
    }

    JSONObject toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("secretid",secretid);
            jsonObject.put("sign",sign);
            jsonObject.put("t",Integer.toString(t));
            jsonObject.put("r",Integer.toString(r));
            jsonObject.put("time", Integer.toString(time));
            jsonObject.put("phonenumber",phonenumber);
            jsonObject.put("textmsg",textmsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
