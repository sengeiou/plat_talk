package com.kylindev.totalk.net;

class Channel {

        private int id;

        private String name;

        private boolean temporary;

        private String members;

        public void setId(int id){
        this.id = id;
    }
        public int getId(){
        return this.id;
    }
        public void setName(String name){
        this.name = name;
    }
        public String getName(){
        return this.name;
    }
        public void setTemporary(boolean temporary){
        this.temporary = temporary;
    }
        public boolean getTemporary(){
        return this.temporary;
    }
        public void setMembers(String members){
        this.members = members;
    }
        public String getMembers(){
        return this.members;
    }

}
