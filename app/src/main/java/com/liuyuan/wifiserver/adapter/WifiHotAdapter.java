
package com.liuyuan.wifiserver.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.liuyuan.wifiserver.R;

import java.util.List;

/**
 * 
 * @Description: TODO(Wifi Hotpot list Adapter)
 * @version V1.0
 */
public class WifiHotAdapter extends BaseAdapter {

    private List<ScanResult> mResults;
    private Context mContext;

    public WifiHotAdapter(List<ScanResult> mResults, Context mContext) {
        this.mResults = mResults;
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return mResults == null ? 0 : mResults.size();
    }

    @Override
    public Object getItem(int position) {
        return mResults == null ? null : mResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        
        ViewHolder viewHolder;
        if(null == convertView) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.wifihot_lv_item, null);
            //ui
            viewHolder.tvName = (TextView) convertView.findViewById(R.id.hotName);
            viewHolder.tvLevel = (TextView) convertView.findViewById(R.id.hotLevel);
            convertView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) convertView.getTag(); 
        }
        //data
        ScanResult result = mResults.get(position);
        viewHolder.tvName.setText(result.SSID);
        viewHolder.tvLevel.setText("Level : "+result.level);
      
        return convertView;
    }
    
    public void refreshData(List<ScanResult> wifiList) {
        this.mResults = wifiList;
        this.notifyDataSetChanged();
    }
    
    public void clearData() {
        if(mResults != null && mResults.size() > 0) {
            mResults.clear();
            mResults = null;
        }
    }

    class ViewHolder{
        private TextView tvName;
        private TextView tvLevel;
    }
}
