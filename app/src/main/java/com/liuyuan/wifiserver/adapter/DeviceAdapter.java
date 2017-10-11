package com.liuyuan.wifiserver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.liuyuan.wifiserver.R;

import java.util.ArrayList;

/**
 * Created by liuyuan on 2017/9/30.
 */

public class DeviceAdapter extends BaseAdapter {
    private ArrayList<String> deviceIp;
    private Context mContext;
    public DeviceAdapter(ArrayList deviceIp,Context mContext){
        this.deviceIp = deviceIp;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return deviceIp == null ? 0 : deviceIp.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceIp == null ? null : deviceIp.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;
        if(null == convertView) {
            mViewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.device_lv_item, null);
            //ui
            mViewHolder.tvName = (TextView) convertView.findViewById(R.id.deviceIp);
            convertView.setTag(mViewHolder);
        }else {
            mViewHolder = (DeviceAdapter.ViewHolder) convertView.getTag();
        }
        //data
        String theIp = deviceIp.get(position);
        mViewHolder.tvName.setText("device " +position + ": "+ theIp + "has connected...");
        return convertView;
    }

    public void refreshData(ArrayList deviceIp) {
        this.deviceIp = deviceIp;
        this.notifyDataSetChanged();
    }

    public void clearData() {
        if(deviceIp != null && deviceIp.size() > 0) {
            deviceIp.clear();
            deviceIp = null;
        }
    }

    class ViewHolder{
        private TextView tvName;
    }
}


