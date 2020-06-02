package com.kylindev.totalk.net;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Test {


    public static int getChannelId(String userName) {
        //申明给服务端传递一个json串
        //创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //创建一个RequestBody(参数1：数据类型 参数2传递的json串)
        RequestBody body = new FormBody.Builder()
                .add("pageIndex", "1")
                .add("pageSize", "20")
                .add("api_token","wocao").build();

        //创建一个请求对象
        Request request = new Request.Builder()
                .url("http://140.143.236.23/pttDev/api/?method=getChannelsInPage")
                .post(body)
                .build();
        //发送请求获取响应
        try {
            Response response=okHttpClient.newCall(request).execute();
            //判断请求是否成功
            if(response.isSuccessful()){
                //打印服务端返回结果
                Log.e("wocao token",response.body().string());
                Gson gson=new Gson();
                RespChannel respChannel=gson.fromJson(response.body().toString(),RespChannel.class);
                if(respChannel.getCode()==200){
                    ArrayList<Channel> channels=respChannel.getChannellist();
                    for(Channel channel:channels){
                        if(channel.getName().equals(userName)){
                            return channel.getId();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    public static ArrayList<Member> getMembers(String cid) {
        //申明给服务端传递一个json串
        //创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //创建一个RequestBody(参数1：数据类型 参数2传递的json串)
        RequestBody body = new FormBody.Builder()
                .add("cid", cid)
                .add("api_token","wocao").build();

        //创建一个请求对象
        Request request = new Request.Builder()
                .url("http://140.143.236.23/pttDev/api/?method=getMembers")
                .post(body)
                .build();
        //发送请求获取响应
        try {
            Response response=okHttpClient.newCall(request).execute();
            //判断请求是否成功
            if(response.isSuccessful()){
                //打印服务端返回结果
                Log.e("wocao token",response.body().string());
                String s=response.body().string();
                Gson gson=new Gson();
                RespMember resp=gson.fromJson(s,RespMember.class);
                if(resp.getCode()==200){
                    ArrayList<Member> members= resp.getUserlist();
                    return members;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }





    public static boolean addMembers(String cid,String users) {
        //申明给服务端传递一个json串
        //创建一个OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //创建一个RequestBody(参数1：数据类型 参数2传递的json串)
        RequestBody body = new FormBody.Builder()
                .add("cid", cid)
                .add("memberids",users)
                .add("api_token","wocao").build();

        //创建一个请求对象
        Request request = new Request.Builder()
                .url("http://140.143.236.23/pttDev/api/?method=addMember")
                .post(body)
                .build();
        //发送请求获取响应
        try {
            Response response=okHttpClient.newCall(request).execute();
            //判断请求是否成功
            if(response.isSuccessful()){
                //打印服务端返回结果
                Log.e("wocao token",response.body().string());
                String s=response.body().string();
                Gson g=new Gson();
                Resp resp= g.fromJson(s, Resp.class);
                if(resp.getCode()==200){
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
