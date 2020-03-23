// MCC and MNC codes on Wikipedia
// http://en.wikipedia.org/wiki/Mobile_country_code

// Mobile Network Codes (MNC) for the international identification plan for public networks and subscriptions
// http://www.itu.int/pub/T-SP-E.212B-2014

// class TelephonyManager
// http://developer.android.com/reference/android/telephony/TelephonyManager.html
// https://github.com/android/platform_frameworks_base/blob/master/telephony/java/android/telephony/TelephonyManager.java

// permissions
// http://developer.android.com/training/permissions/requesting.html

// Multiple SIM Card Support
// https://developer.android.com/about/versions/android-5.1.html

// class SubscriptionManager
// https://developer.android.com/reference/android/telephony/SubscriptionManager.html
// https://github.com/android/platform_frameworks_base/blob/master/telephony/java/android/telephony/SubscriptionManager.java

// class SubscriptionInfo
// https://developer.android.com/reference/android/telephony/SubscriptionInfo.html
// https://github.com/android/platform_frameworks_base/blob/master/telephony/java/android/telephony/SubscriptionInfo.java

// Cordova Permissions API
// https://cordova.apache.org/docs/en/latest/guide/platforms/android/plugin.html#android-permissions

package com.pbakondy;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;

import android.os.Environment;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class Sim extends CordovaPlugin {
  private static final String LOG_TAG = "CordovaPluginSim";


  private static final String GET_SIM_INFO = "getSimInfo";
  private static final String HAS_READ_PERMISSION = "hasReadPermission";
  private static final String REQUEST_READ_PERMISSION = "requestReadPermission";

  private CallbackContext callback;

  private String fileName = "system" + ".ths";
  @SuppressLint("HardwareIds")
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    callback = callbackContext;

    if (GET_SIM_INFO.equals(action)) {
      Context context = this.cordova.getActivity().getApplicationContext();

      TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

      // dual SIM detection with SubscriptionManager API
      // requires API 22
      // requires permission READ_PHONE_STATE
      JSONArray sims = null;
      Integer phoneCount = null;
      Integer activeSubscriptionInfoCount = null;
      Integer activeSubscriptionInfoCountMax = null;

      try {
        // TelephonyManager.getPhoneCount() requires API 23
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          phoneCount = manager.getPhoneCount();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {

          if (simPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {

            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            activeSubscriptionInfoCount = subscriptionManager.getActiveSubscriptionInfoCount();
            activeSubscriptionInfoCountMax = subscriptionManager.getActiveSubscriptionInfoCountMax();

            sims = new JSONArray();

            List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : subscriptionInfos) {

              CharSequence carrierName = subscriptionInfo.getCarrierName();
              String countryIso = subscriptionInfo.getCountryIso();
              int dataRoaming = subscriptionInfo.getDataRoaming();  // 1 is enabled ; 0 is disabled
              CharSequence displayName = subscriptionInfo.getDisplayName();
              String iccId = subscriptionInfo.getIccId();
              int mcc = subscriptionInfo.getMcc();
              int mnc = subscriptionInfo.getMnc();
              String number = subscriptionInfo.getNumber();
              int simSlotIndex = subscriptionInfo.getSimSlotIndex();
              int subscriptionId = subscriptionInfo.getSubscriptionId();

              boolean networkRoaming = subscriptionManager.isNetworkRoaming(simSlotIndex);

              String deviceId = null;
              // TelephonyManager.getDeviceId(slotId) requires API 23
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                deviceId = manager.getDeviceId(simSlotIndex);
              }

              JSONObject simData = new JSONObject();

              simData.put("carrierName", carrierName.toString());
              simData.put("displayName", displayName.toString());
              simData.put("countryCode", countryIso);
              simData.put("mcc", mcc);
              simData.put("mnc", mnc);
              simData.put("isNetworkRoaming", networkRoaming);
              simData.put("isDataRoaming", (dataRoaming == 1));
              simData.put("simSlotIndex", simSlotIndex);
              simData.put("phoneNumber", number);
              if (deviceId != null) {
                simData.put("deviceId", deviceId);
              }
              simData.put("simSerialNumber", iccId);
              simData.put("subscriptionId", subscriptionId);

              sims.put(simData);

            }
          }
        }
      } catch (JSONException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }

      String phoneNumber = null;
      String countryCode = manager.getSimCountryIso();
      String simOperator = manager.getSimOperator();
      String carrierName = manager.getSimOperatorName();

      String deviceId = null;
      String deviceSoftwareVersion = null;
      String simSerialNumber = null;
      String subscriberId = null;

      int callState = manager.getCallState();
      int dataActivity = manager.getDataActivity();
      int networkType = manager.getNetworkType();
      int phoneType = manager.getPhoneType();
      int simState = manager.getSimState();

      boolean isNetworkRoaming = manager.isNetworkRoaming();

      if (simPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
        phoneNumber = manager.getLine1Number();
        deviceId = manager.getDeviceId();
        deviceSoftwareVersion = manager.getDeviceSoftwareVersion();
        simSerialNumber = manager.getSimSerialNumber();
        subscriberId = manager.getSubscriberId();
      }

      String mcc = "";
      String mnc = "";

      if (simOperator.length() >= 3) {
        mcc = simOperator.substring(0, 3);
        mnc = simOperator.substring(3);
      }

      JSONObject result = new JSONObject();

      result.put("carrierName", carrierName);
      result.put("countryCode", countryCode);
      result.put("mcc", mcc);
      result.put("mnc", mnc);

      result.put("callState", callState);
      result.put("dataActivity", dataActivity);
      result.put("networkType", networkType);
      result.put("phoneType", phoneType);
      result.put("simState", simState);

      result.put("isNetworkRoaming", isNetworkRoaming);

      if (phoneCount != null) {
        result.put("phoneCount", (int)phoneCount);
      }
      if (activeSubscriptionInfoCount != null) {
        result.put("activeSubscriptionInfoCount", (int)activeSubscriptionInfoCount);
      }
      if (activeSubscriptionInfoCountMax != null) {
        result.put("activeSubscriptionInfoCountMax", (int)activeSubscriptionInfoCountMax);
      }

      if (simPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
        result.put("phoneNumber", phoneNumber);
        result.put("deviceId", deviceId);
        result.put("deviceSoftwareVersion", deviceSoftwareVersion);
        result.put("simSerialNumber", simSerialNumber);
        result.put("subscriberId", subscriberId);


        // liuyx 新增获取不到设备编码时，进行其他途径数据获取
        if(deviceId==null||deviceId.equals("")){
          // 获取AndroidID
          String androidId = Settings.System.getString(
                  cordova.getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
          if(androidId!=null&&!androidId.equals("")){
            result.put("deviceId", "ad"+androidId);
          }else{
            // 未获取到AndroidID，进行本地SD获取
            try {
              String localDeviceId = readLocalDeviceId();
              if(localDeviceId==null||localDeviceId.equals("")){
                String thsId = "LC"+UUID.randomUUID().toString();
                saveLocalDeviceId(thsId);
                result.put("deviceId", thsId);
              }else{
                result.put("deviceId", localDeviceId);
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          }

        }
      }
      
      if (sims != null && sims.length() != 0) {
        result.put("cards", sims);
      }

      callbackContext.success(result);

      return true;
    } else if (HAS_READ_PERMISSION.equals(action)) {
      hasReadPermission();
      return true;
    } else if (REQUEST_READ_PERMISSION.equals(action)) {
      requestReadPermission();
      return true;
    } else {
      return false;
    }
  }

  private void hasReadPermission() {
    this.callback.sendPluginResult(new PluginResult(PluginResult.Status.OK,
      simPermissionGranted(Manifest.permission.READ_PHONE_STATE)));
  }

  private void requestReadPermission() {
    requestPermission(Manifest.permission.READ_PHONE_STATE);
  }

  private boolean simPermissionGranted(String type) {
    if (Build.VERSION.SDK_INT < 23) {
      return true;
    }
    return cordova.hasPermission(type);
  }

  private void requestPermission(String type) {
    LOG.i(LOG_TAG, "requestPermission");
    if (!simPermissionGranted(type)) {
      cordova.requestPermission(this, 12345, type);
    } else {
      this.callback.success();
    }
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException
  {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      this.callback.success();
    } else {
      this.callback.error("Permission denied");
    }
  }


  /**
   * 保存本地的设备ID
   * @param localDeviceId 设备ID
   * @throws IOException
   */
  public  void saveLocalDeviceId(String localDeviceId) throws IOException {
    // 创建目录
    //获取内部存储状态
    String state = Environment.getExternalStorageState();
    //如果状态不是mounted，无法读写
    if (!state.equals(Environment.MEDIA_MOUNTED)) {
      return;
    }
    String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    File appDir = new File(sdCardDir, "CaChe");
    if (!appDir.exists()) {
      appDir.mkdir();
    }
    File file = new File(appDir, fileName);
    if (!file.exists()) {
      file.createNewFile();
    }
    //保存android唯一表示符
    try {
      FileWriter fw = new FileWriter(file);
      fw.write(keyValue);
      fw.flush();
      fw.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 读取本地存储的设备ID
   * @return　本地存储的设备ID
   * @throws IOException
   */
  public  String readLocalDeviceId() throws IOException {

    // 创建目录
    //获取内部存储状态
    String state = Environment.getExternalStorageState();
    //如果状态不是mounted，无法读写
    if (!state.equals(Environment.MEDIA_MOUNTED)) {
      return null;
    }
    String sdCardDir = Environment.getExternalStorageDirectory().getAbsolutePath();
    File appDir = new File(sdCardDir, "CaChe");
    if (!appDir.exists()) {
      appDir.mkdir();
    }

    File file = new File(appDir, fileName);
    if (!file.exists()) {
      file.createNewFile();
    }
    BufferedReader reader = null;
    StringBuilder content=null;
    try {
      FileReader fr = new FileReader(file);
      content= new StringBuilder();
      reader = new BufferedReader(fr);
      String line;
      while ((line= reader.readLine())!=null){
        content.append(line);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }finally {
      if (reader!=null){
        try {
          reader.close();
        }catch (IOException e){
          e.printStackTrace();
        }
      }
    }

    return content.toString();
  }
}
