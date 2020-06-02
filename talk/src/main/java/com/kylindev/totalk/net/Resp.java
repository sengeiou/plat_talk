package com.kylindev.totalk.net;

class Resp
{
    public Resp(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    private int code;

    private String reason;

    public void setCode(int code){
        this.code = code;
    }
    public int getCode(){
        return this.code;
    }
    public void setReason(String reason){
        this.reason = reason;
    }
    public String getReason(){
        return this.reason;
    }
}
