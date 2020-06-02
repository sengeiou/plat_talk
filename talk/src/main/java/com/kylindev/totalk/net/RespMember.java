package com.kylindev.totalk.net;

import java.util.ArrayList;


public class RespMember
{
    private int code;

    private String reason;

    private ArrayList<Member> userlist;

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
    public void setUserlist(ArrayList<Member> userlist){
        this.userlist = userlist;
    }
    public ArrayList<Member> getUserlist(){
        return this.userlist;
    }
}
