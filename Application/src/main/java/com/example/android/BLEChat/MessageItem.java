package com.example.android.BLEChat;

/**
 * Created by nccu_dct on 15/9/15.
 */
public class MessageItem {
    private long time;
    private String content;
    private long id;
    private int type;


    public MessageItem(){
        id = 0;
        time = 0;
        content = "";
    }
    public MessageItem(long t, int y, String m){
        id = 0;
        time = t;
        type = y;
        content = m;
    }
    public void setTime(long t){
        time = t;
    }
    public long getTime(){
        return time;
    }
    public void setContent(String m){
        content = m;
    }
    public String getContent(){
        return content;
    }
    public void setId(long num){
        id = num;
    }
    public long getId(){
        return id;
    }
    public void setType(int t){
        type = t;
    }
    public int getType(){
        return type;
    }
}
