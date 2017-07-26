
package com.liuyuan.wifiserver.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.liuyuan.wifiserver.service.WifiHotManager.OperationsType;
import com.liuyuan.wifiserver.service.WifiHotManager.WifiBroadCastOperator;

/**
 * @Description: TODO(the specified SSID wifi state broadcast listener)
 * @version V1.0
 */
public class WifiStateReceiver extends BroadcastReceiver {

    private WifiBroadCastOperator mWifiOperator;
    private String mSsid;
    private OperationsType operationsType;
    public static final String TAG = WifiStateReceiver.class.getSimpleName();

    public WifiStateReceiver(WifiBroadCastOperator mWifiOperator, String ssid) {
        this.mWifiOperator = mWifiOperator;
        mSsid = ssid;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "into onReceive(Context context, Intent intent)");
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            Log.d("WIFI状态", "wifiState-->" + wifiState);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLING:  // 0
                    break;
                case WifiManager.WIFI_STATE_DISABLED:  // 1
                    break;
                case WifiManager.WIFI_STATE_ENABLING:  // 2
                    break;
                case WifiManager.WIFI_STATE_ENABLED:  // 3
                    if (operationsType != null) {
                        mWifiOperator.operationByType(operationsType, mSsid);
                    }
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN: // 4
                    break;
                default:
                        break;
            }
        }

    }

    public void setOperationsType(OperationsType operationsType) {
        this.operationsType = operationsType;
    }

}
