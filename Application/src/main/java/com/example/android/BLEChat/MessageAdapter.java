package com.example.android.BLEChat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Rabbit徐 on 2015/7/22.
 */
public class MessageAdapter extends BaseAdapter {
    private Context mContext;
    private List<CheckMessage> mData;

    public MessageAdapter(Context context,List<CheckMessage> data)
    {
        this.mContext=context;
        this.mData=data;
    }

    public void Refresh()
    {
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return mData.size();
    }

    @Override
    public Object getItem(int Index)
    {
        return mData.get(Index);
    }

    @Override
    public long getItemId(int Index)
    {
        return Index;
    }

    @Override
    public View getView(int Index, View mView, ViewGroup mParent)
    {
        TextView Content;
        switch(mData.get(Index).getType())
        {
            case CheckMessage.MessageType_Time:

                break;
            case CheckMessage.MessageType_From:
                mView=LayoutInflater.from(mContext).inflate(R.layout.message_out, null);
                Content=(TextView)mView.findViewById(R.id.out);
                Content.setText("Me: "+mData.get(Index).getContent());
                break;
            case CheckMessage.MessageType_To:
                mView=LayoutInflater.from(mContext).inflate(R.layout.message, null);
                Content=(TextView)mView.findViewById(R.id.in);
                Content.setText(mData.get(Index).getName()+": "+mData.get(Index).getContent());
                break;
        }
        return mView;
    }

}
