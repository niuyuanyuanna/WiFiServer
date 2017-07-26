package com.liuyuan.wifiserver.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.liuyuan.wifiserver.service.WifiHotManager.WifiBroadCastOperator;

import java.util.List;

/**
 * 
 * @Description: TODO(wifi scan result broadcast listener) 
 * @author Snail  (Zhanghf QQ:651555765)
 * @date 2014-4-25 上午9:06:06 
 * @version V1.0
 */
public class WifiScanRsultReciver extends BroadcastReceiver{
    
    private WifiBroadCastOperator mWifiOperator;
    private WifiManager mWifiManager;
    public static final String TAG = WifiScanRsultReciver.class.getSimpleName();
    /**scan wifi connect list**/
    private List<ScanResult> wifiList;

    public WifiScanRsultReciver(WifiBroadCastOperator mWifiBroadCastOperator) {
        this.mWifiOperator = mWifiBroadCastOperator;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "into onReceive(Context context, Intent intent)");
        if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equalsIgnoreCase(intent.getAction())) { 
            mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            //sav scan results
            wifiList = mWifiManager.getScanResults();
            //refresh
            mWifiOperator.disPlayWifiScanResult(wifiList);
        }
    }

}
