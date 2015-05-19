package com.example.plugin;

// cordova
import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

// Android
import android.content.BroadcastReceiver;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import java.util.ArrayList;
//PFU ScanSnap SDK classes
import com.fujitsu.pfu.mobile.device.PFUDevice;
import com.fujitsu.pfu.mobile.device.PFUDeviceError;
import com.fujitsu.pfu.mobile.device.PFUDeviceManager;
import com.fujitsu.pfu.mobile.device.PFUNotification;
import com.fujitsu.pfu.mobile.device.PFUSSDevice;
import com.fujitsu.pfu.mobile.device.PFUSSDeviceManager;
import com.fujitsu.pfu.mobile.device.SSDeviceScanSettings;
import com.fujitsu.pfu.mobile.device.SSNotification;
import com.fujitsu.pfu.mobile.device.SSDeviceError;



public class Hello extends CordovaPlugin {

	private Context app_context = null;
	private Activity activity = null;
	private CallbackContext callbackContext = null;
	private IntentFilter intentfilter = new IntentFilter();
	private LocalBroadcastManager localBroadcastManager = null;
	private BroadcastReceiver broadcastReceiver = null;


	private static final String OUTPUT_PATH = "/mnt/sdcard/scansnap";
	private static final String DEVICE_PASSWORD = "1234";
	private PFUDeviceManager pfuDeviceManager;
	private PFUSSDevice device = null;

	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;

		setupBroadcastManager();


		if (action.equals("greet")) {
			String name = data.getString(0);
			String message = "Hello, " + name;
			this.createCallback(message , true);
			//callbackContext.success(message);
			return true;
		}
		// test if the required action refers to the 'search' method
		if (action.equals("search")){
			this.Search();
			return true;
		}
		// test if the required action refers to the 'scan' method
		if (action.equals("scan")){
			this.Scan();
			return true;
		}




		// no action matches, return error
		return false;
	}
	private void createCallback(String info, boolean keepCallback){
		PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.callbackContext.sendPluginResult(result);
    }
	public void setupBroadcastManager(){
		// if activity and received have not been created yet, create them
		if(activity == null || broadcastReceiver == null){
			app_context = this.cordova.getActivity().getApplicationContext();
			// Get the main activity
			activity = this.cordova.getActivity();
			// Use the local broadcast manager
			localBroadcastManager = LocalBroadcastManager.getInstance(activity);
			// Add actions
			intentfilter.addAction(PFUNotification.ACTION_PFU_DEVICE_DID_CONNECT);
			intentfilter.addAction(PFUNotification.ACTION_PFU_LIST_OF_DEVICES_DID_CHANGE);
			intentfilter.addAction(SSNotification.ACTION_SS_DEVICE_DID_FINISH_SCAN);
			// Create and connect a new broadcast receiver
			broadcastReceiver = new MyBroadcastReceiver();
			localBroadcastManager.registerReceiver(broadcastReceiver, intentfilter);
		}
	}
	private void Search()
	{
		// Create specific a device manager
		pfuDeviceManager = PFUDeviceManager.getDeviceManagerWithType(PFUSSDeviceManager.class, PFUSSDeviceManager.PFUDEVICETYPE_SCANSNAP, app_context);
		// Search for available devices
		PFUDeviceError devError = pfuDeviceManager.searchForDevices(PFUDeviceManager.PFUSCANSNAP_ALL);
	}
	private void onPFUDeviceConnect()
	{
		// Get list of available devices
		ArrayList<PFUDevice> list = (ArrayList<PFUDevice>)
		pfuDeviceManager.getDeviceList();
		if(list != null){
			// Connect to the first device of the list
			device = (PFUSSDevice)list.get(0);
			// Set default password
			device.setPassword(DEVICE_PASSWORD);
			// Connect to the device
			PFUDeviceError devErr = device.connect();
		}
	}
	private void Scan()
	{
		// Create and setup scan settings
		SSDeviceScanSettings m_scanSetting = new SSDeviceScanSettings();
		m_scanSetting.setSaveFolderPath(OUTPUT_PATH); // Set image destination path
		// Get the connected device
		//device = (PFUSSDevice)m_mng.getConnectedDevice();
		// Start scan process
		if(device != null){
			device.beginScanSession();
			device.scanDocuments(m_scanSetting);
			callbackContext.success("Scan command sent.");
		}
	}
	//end class

	// Handler for received Intents
	class MyBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			// Add here any other event handlers
			// ACTION_PFU_LIST_OF_DEVICES_DID_CHANGE event received
			if(intent.getAction().equals(PFUNotification.ACTION_PFU_LIST_OF_DEVICES_DID_CHANGE)) {
			// Connect to new device
				onPFUDeviceConnect();
			}
			if(intent.getAction().equals(SSNotification.ACTION_SS_DEVICE_DID_FINISH_SCAN)) {
				// When scan is finished, end the scan session and disconnect the scanner
				device.endScanSession();
				device.disconnect();
			}
			if(intent.getAction().equals(PFUNotification.ACTION_PFU_DEVICE_DID_CONNECT))
			{
				// A new devices has connected, popup a message
				callbackContext.success("Connected to " +device.getDeviceName().toString());
			}
		}
	}
	
}

