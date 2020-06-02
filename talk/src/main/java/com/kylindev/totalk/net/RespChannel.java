package com.kylindev.totalk.net;

import java.util.ArrayList;

public class RespChannel
{
    private int code;

    private String reason;

    private ArrayList<Channel> channellist;

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
    public void setChannellist(ArrayList<Channel> channellist){
        this.channellist = channellist;
    }
    public ArrayList<Channel> getChannellist(){
        return this.channellist;
    }
}
