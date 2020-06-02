package com.kylindev.totalk.net;

public class Member {
        private String uid;

        private String name;

        private int currentChannelId;

        private String nick;

        public void setUid(String uid){
        this.uid = uid;
    }
        public String getUid(){
        return this.uid;
    }
        public void setName(String name){
        this.name = name;
    }
        public String getName(){
        return this.name;
    }
        public void setCurrentChannelId(int currentChannelId){
        this.currentChannelId = currentChannelId;
    }
        public int getCurrentChannelId(){
        return this.currentChannelId;
    }
        public void setNick(String nick){
        this.nick = nick;
    }
        public String getNick(){
        return this.nick;
    }
}
