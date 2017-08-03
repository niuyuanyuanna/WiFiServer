package com.liuyuan.wifiserver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.liuyuan.wifiserver.R;
import com.liuyuan.wifiserver.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends BaseAdapter{
    
    private Context mContext;
    private List<ChatMessage> messsages;
    

    public ChatAdapter(Context mContext, List<ChatMessage> messsages) {
        this.mContext = mContext;
        this.messsages = messsages;
    }

    @Override
    public int getCount() {
        return messsages == null ? 0 : messsages.size();
    }

    @Override
    public Object getItem(int position) {
        return messsages == null ? null : messsages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (null == convertView) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.chat_layout, null);
            // UI
            viewHolder.deviceName = (TextView) convertView.findViewById(R.id.deviceName);
            viewHolder.deviceAddress = (TextView) convertView.findViewById(R.id.deviceAddress);
            viewHolder.msgTime = (TextView) convertView.findViewById(R.id.msgTime);
            viewHolder.msg = (TextView) convertView.findViewById(R.id.chatText);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        
        viewHolder.deviceName.setText(messsages.get(position).getDeviceName());
        viewHolder.deviceAddress.setText(messsages.get(position).getNetAddress());
        viewHolder.msgTime.setText(messsages.get(position).getMsgTime());
        viewHolder.msg.setText(messsages.get(position).getMsg());
     
        return convertView;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView msgTime;
        TextView msg;
    }

    public void refreshDeviceList(List<ChatMessage> chatMessages) {
        this.messsages = chatMessages;
        this.notifyDataSetChanged();
    }

}
