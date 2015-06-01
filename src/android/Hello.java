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
//import com.fujitsu.pfu.mobile.device.ScanSnapSettings;

import com.fujitsu.pfu.mobile.device.PFUDevice;
import com.fujitsu.pfu.mobile.device.PFUDeviceError;
import com.fujitsu.pfu.mobile.device.PFUDeviceManager;
import com.fujitsu.pfu.mobile.device.PFUNotification;
import com.fujitsu.pfu.mobile.device.PFUSSDevice;
import com.fujitsu.pfu.mobile.device.PFUSSDeviceManager;
import com.fujitsu.pfu.mobile.device.SSDeviceScanSettings;
import com.fujitsu.pfu.mobile.device.SSNotification;
import com.fujitsu.pfu.mobile.device.SSDeviceError;
import com.fujitsu.pfu.mobile.device.SSDevicePageInfo;



import android.util.Log;
import java.lang.reflect.Method;

public class Hello extends CordovaPlugin {
    private static final String LOG_TAG = "SnapScanPlugin";

	private Context app_context = null;
	private Activity activity = null;
	private CallbackContext callbackContext = null;
	private IntentFilter intentfilter = new IntentFilter();
	private LocalBroadcastManager localBroadcastManager = null;
	private BroadcastReceiver broadcastReceiver = null;


	//private static final int FILE_FORMAT = 0; (0 is jpeg 1 is pdf)
	private static final int FILE_FORMAT = 1;
	private static final String OUTPUT_PATH = "/mnt/sdcard/scansnap";
	private static final String DEVICE_PASSWORD = "2155";
	private PFUDeviceManager pfuDeviceManager;
	private PFUSSDevice device = null;
	public String scan_action = "single";

	private String convertIntToString(int i){
		return Integer.toString(i);
	}

	@Override
	public boolean execute(String action, String data, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
      	Log.v(LOG_TAG, "Init Test Logs datas : " + data);
		setupBroadcastManager();

		final String myTransportedDatas = data.replaceAll("\\W",""); // clean string ( very quick fix to del ["..."] )
      	Log.v(LOG_TAG, "myTransportedDatas : " + myTransportedDatas);

		if (action.equals("greet")) {
            cordova.getActivity().runOnUiThread(new Runnable() {
            	String datas = new String(myTransportedDatas);
                public void run() {
					String message = "Hello, " + datas;

					createCallback(message , true, true);
                }
            });
            return true;
		}
		// test if the required action refers to the 'search' method
		if (action.equals("search")){
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
					Search();
                }
            });
            return true;
		}
		// test if the required action refers to the 'scan' method
		if (action.equals("scan")){
            cordova.getActivity().runOnUiThread(new Runnable() {
				String datas = myTransportedDatas;
                public void run() {
					Scan(datas); // single or double page
				}
            });
            return true;
		}
		// no action matches, return error
		return false;
	}
	public void createCallback(String info, boolean keepCallback, boolean success){
		PluginResult result = null;
		if (success == false){
			result = new PluginResult(PluginResult.Status.ERROR, info);			
		} else {
			result = new PluginResult(PluginResult.Status.OK, info);
		}
        result.setKeepCallback(keepCallback);
        this.callbackContext.sendPluginResult(result);
    }
	public void setupBroadcastManager(){
		// if activity and received have not been created yet, create them
		if(activity == null || broadcastReceiver == null){
			// Get the main activity
			activity = this.cordova.getActivity();
			// Get app context
			app_context = this.cordova.getActivity().getApplicationContext();
			// Use the local broadcast manager
			localBroadcastManager = LocalBroadcastManager.getInstance(activity);
			// Add actions
			intentfilter.addAction(PFUNotification.ACTION_PFU_DEVICE_DID_CONNECT);
			intentfilter.addAction(PFUNotification.ACTION_PFU_LIST_OF_DEVICES_DID_CHANGE);
			intentfilter.addAction(SSNotification.ACTION_SS_DEVICE_DID_FINISH_SCAN);
			// extra receiver for scan manager actions
			intentfilter.addAction(SSNotification.ACTION_SS_DEVICE_DID_FINISH_MAKE_PDF);
			intentfilter.addAction(SSNotification.EXTRA_DATA_SS_DEVICE_DID_FINISH_MAKE_PDF);
			intentfilter.addAction(SSNotification.ACTION_SS_DEVICE_DID_SCAN_PAGE);
			intentfilter.addAction(SSNotification.EXTRA_DATA_SS_DEVICE_DID_SCAN_PAGE);

			// Create and connect a new broadcast receiver
			broadcastReceiver = new MyBroadcastReceiver();
			localBroadcastManager.registerReceiver(broadcastReceiver, intentfilter);
		}
	}
	private void Search()
	{
		device = null; // reset the device for sure remake all processus
		// Create specific a device manager
		pfuDeviceManager = PFUDeviceManager.getDeviceManagerWithType(PFUSSDeviceManager.class, PFUSSDeviceManager.PFUDEVICETYPE_SCANSNAP, app_context);
		

		// Search for available devices
		PFUDeviceError devError = pfuDeviceManager.searchForDevices(PFUDeviceManager.PFUSCANSNAP_ALL);
		int errorCode = devError.getErrorCode();
		if (errorCode != 0){
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Device canot be searched error : "+convertIntToString(errorCode));
			result.setKeepCallback(true);
			callbackContext.sendPluginResult(result);
		}
		Log.v(LOG_TAG, "Search:: errorCode searchForDevices");
		Log.v(LOG_TAG, this.convertIntToString(errorCode));
		//this.createCallback("Search is begin " , true, true);

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
			// logs
			int errorCode = devErr.getErrorCode();
			Log.v(LOG_TAG, "onPFUDeviceConnect:: errorCode conect");
			Log.v(LOG_TAG, this.convertIntToString(errorCode));
			
			// my device is not connect error
			if (errorCode != 0){
				PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Device canot be conected error : "+convertIntToString(errorCode));
    			result.setKeepCallback(true);
    			callbackContext.sendPluginResult(result);
			}
			//String name = device.getDeviceName();
			//Log.v(LOG_TAG, "Scan:: onPFUDeviceConnect:: "+name+" is connected ! ");
			

			//this.createCallback(name+" is connected ! " , true, true);

		}
	}
	public void Scan(String status)
	{
		Log.v (LOG_TAG, "Scan Entry Param : " + status);

		// Create and setup scan settings
		SSDeviceScanSettings m_scanSetting = new SSDeviceScanSettings();
		m_scanSetting.setFileFormat(FILE_FORMAT);
		m_scanSetting.setSaveFolderPath(OUTPUT_PATH); // Set image destination path

		if (status.equals("double")){ // set recto/verso
			Log.v (LOG_TAG, "DOUBLE PAGE active : " + status);
			m_scanSetting.setMultiFeed(1);
		}
		// Get the connected device
		device = (PFUSSDevice)pfuDeviceManager.getConnectedDevice();
		// Start scan process

		if(device != null){
			PFUDeviceError devErr = device.beginScanSession();
			int errorCode = devErr.getErrorCode();
			
			Log.v(LOG_TAG, "Scan:: errorCode is beginScanSession");
			Log.v(LOG_TAG, this.convertIntToString(errorCode));

			devErr = device.scanDocuments(m_scanSetting);
			errorCode = devErr.getErrorCode();

			Log.v(LOG_TAG, "Scan:: errorCode is beginScanSession");
			Log.v(LOG_TAG, this.convertIntToString(errorCode));

			// my device canot be conected.. callback them
			if (errorCode != 0){
				PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error on scan function : "+ convertIntToString(errorCode));
    			result.setKeepCallback(true);
    			callbackContext.sendPluginResult(result);
			}
			// return the path file in callback
			//this.createCallback(filePath , true);
			//this.createCallback("Scan command sent." , true, true);
			
			// send control callback for test if command is proper sended
			//callbackContext.success("Scan command sent.");
		

		} else {
			Log.v(LOG_TAG, "Scan:: device is null -> no scan, no callback");
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Error on scan function : device is null ");
			result.setKeepCallback(true);
			callbackContext.sendPluginResult(result);
		}
	}
	//end class

	// Handler for received Intents
	class MyBroadcastReceiver extends BroadcastReceiver {
		private Hello hello = new Hello();
	    private static final String LOG_TAG = "SnapScanPlugin/MyBroadcastReceiver";

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
				// CALBACK FOR SEARCH THE DEVICE
				// A new devices has connected, popup a message
				//callbackContext.success("Connected to " +device.getDeviceName().toString());
				
				/*PluginResult result = new PluginResult(PluginResult.Status.OK, "Connected to " +device.getDeviceName().toString());
    			result.setKeepCallback(true);
    			callbackContext.sendPluginResult(result);*/

    			// directly scan action ?
    			Scan("single");

			}
			// callback for finish scan action 
			// PDF
			if(intent.getAction().equals(SSNotification.ACTION_SS_DEVICE_DID_FINISH_MAKE_PDF)) {
			  	// Get the pdf path
			  	String pdfPath = intent.getStringExtra(SSNotification.EXTRA_DATA_SS_DEVICE_DID_FINISH_MAKE_PDF);
				Log.v(LOG_TAG, "Scan:: scan in pdf is finish, path of file :");
				Log.v(LOG_TAG, pdfPath);
			  	
			  	// test the send the broadcast end scan
				PluginResult result = new PluginResult(PluginResult.Status.OK, pdfPath);
    			result.setKeepCallback(false);
    			callbackContext.sendPluginResult(result);


			}
			// JPEG
			if(intent.getAction().equals(SSNotification.ACTION_SS_DEVICE_DID_SCAN_PAGE)) {
			  	SSDevicePageInfo information = (SSDevicePageInfo)intent.getSerializableExtra(SSNotification.EXTRA_DATA_SS_DEVICE_DID_SCAN_PAGE);

			  	String frontJPEGPath = information.getFilePathFront();
			  	// If duplex...
			  	String backJPEGPath = information.getFilePathBack();

				Log.v(LOG_TAG, "Scan:: scan in JPEG is finish, path of file 1 et 2 :");
				Log.v(LOG_TAG, "Scan:: frontJPEGPath : "+frontJPEGPath);
				Log.v(LOG_TAG, "Scan:: backJPEGPath : "+backJPEGPath);

			  	JSONObject returnObject = new JSONObject();
			  	try { // wanted.
					returnObject.put("frontPage", frontJPEGPath);
					returnObject.put("backPage", backJPEGPath);
				} catch (JSONException e) {
					Log.v(LOG_TAG, "SCAN : JSON EXCEPTION");
				    // TODO Auto-generated catch block
				    e.printStackTrace();
				}

				Log.v(LOG_TAG, "Scan:: scan in JPEG is finish, jsonobject for callback :");
				Log.v(LOG_TAG, returnObject.toString());
				
				// return final object with filesPath
				PluginResult result = new PluginResult(PluginResult.Status.OK, returnObject);
    			result.setKeepCallback(false);
    			callbackContext.sendPluginResult(result);

				// return the two url of pages in a calback (json object)
				//callbackContext.success("Connected to " +device.getDeviceName().toString());

			  	//hello.createCallback(returnObject.toString() , true, true);

			}


		}
	}
	
}