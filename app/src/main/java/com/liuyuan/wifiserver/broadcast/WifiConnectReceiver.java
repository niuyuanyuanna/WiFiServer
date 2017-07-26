package com.liuyuan.wifiserver.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

import com.liuyuan.wifiserver.constant.Global;
import com.liuyuan.wifiserver.service.WifiHotManager;
import com.liuyuan.wifiserver.service.WifiHotManager.WifiBroadCastOperator;

/**
 * 
 * @Description: TODO(wifi connect broadcast listener)
 * @version V1.0
 */
public class WifiConnectReceiver extends BroadcastReceiver {
	private static final String TAG = WifiConnectReceiver.class.getSimpleName();

	private WifiHotManager.WifiBroadCastOperator mWifiOperator;
	private WifiManager mWifiManager;

	public WifiConnectReceiver(WifiBroadCastOperator mWifiOperator) {
		this.mWifiOperator = mWifiOperator;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	    Log.i(TAG, "into onReceive(Context context, Intent intent)");
		if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
			//receivew
			Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (null != parcelableExtra) {
				NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
				mWifiManager = (WifiManager) context
						.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				String SSID = wifiInfo.getSSID();
				
				switch (networkInfo.getState()) {
				/**State : CONNECTING, CONNECTED, SUSPENDED, DISCONNECTING, DISCONNECTED, UNKNOWN**/
				case CONNECTED:
					Log.d("APActivity", "CONNECTED");
					if(  SSID.contains(Global.HOTPOT_NAME)) {
					    //refresh
						mWifiOperator.disPlayWifiConnResult(true, wifiInfo);
					} else {
						mWifiOperator.disPlayWifiConnResult(false, wifiInfo);
					}
					break;
				case CONNECTING:
					Log.d("APActivity", "CONNECTING");
					break;
				case DISCONNECTED:
					Log.d("APActivity", "DISCONNECTED");
					break;
				case DISCONNECTING:
					Log.d("APActivity", "DISCONNECTING");
					break;
				case SUSPENDED:
					Log.d("APActivity", "SUSPENDED");
					break;
				case UNKNOWN:
					Log.d("APActivity", "UNKNOWN");
					break;
				default:
					break;
				}
			}
		}
	}

}
