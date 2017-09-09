package com.example.android.BLEChat;

/**
 * Created by Rabbit徐 on 2015/7/22.
 */
public class CheckMessage {

    public static final int MessageType_Time=0;
    public static final int MessageType_From=1;
    public static final int MessageType_To=2;

    /*public CheckMessage(int Type,String Content)
    {
        this.mType=Type;
        this.mContent=Content;
    }*/

    public CheckMessage(long id, long time, int Type,String name, String Content)
    {
        this.id = id;
        this.time = time;
        this.mType = Type;
        this.name = name;
        this.mContent=Content;
    }

    private long id;
    private long time;
    private int mType;
    private String name;
    private String mContent;


    public long getId(){
        return id;
    }
    public void setId(long n){
        id = n;
    }
    public long getTime(){
        return time;
    }
    public void setTime(long t){
        time = t;
    }
    public int getType() {
        return mType;
    }
    public void setType(int mType) {
        this.mType = mType;
    }
    public String getName(){
        return name;
    }
    public void setName(String n){
        name = n;
    }
    public String getContent() {
        return mContent;
    }
    public void setContent(String mContent) {
        this.mContent = mContent;
    }
}
