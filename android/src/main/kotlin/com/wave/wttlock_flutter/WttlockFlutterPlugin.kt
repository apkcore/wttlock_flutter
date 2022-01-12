package com.wave.wttlock_flutter

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice
import com.ttlock.bl.sdk.api.TTLockClient
import com.ttlock.bl.sdk.callback.*
import com.ttlock.bl.sdk.constant.LogType
import com.ttlock.bl.sdk.entity.LockError
import com.ttlock.bl.sdk.entity.PassageModeConfig
import com.wave.wttlock_flutter.bean.AddFingerBean
import com.wave.wttlock_flutter.bean.AddIcCardBean
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONArray

/** WttlockFlutterPlugin */
class WttlockFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware  {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  companion object {
    const val TAG = "TtlockPlugin"
    const val COM = "com.wave.ttlock_plugin"
    private var context: Activity? = null
    private var handler: Handler? = null
    private var methodChannel: MethodChannel? = null
    private var scanStreamChannel: EventChannel? = null
    private var addICCardStreamChannel: EventChannel? = null
    private var addFingerStreamChannel: EventChannel? = null
    private var blueStateStreamChannel: EventChannel? = null
    private var openLog = false

  }
  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "${COM}/plugin")
    methodChannel?.setMethodCallHandler(WttlockFlutterPlugin())
    scanStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, "${COM}/startScanLock")
    scanStreamChannel?.setStreamHandler(StartScanListen())
    addICCardStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, "${COM}/addIcCard")
    addICCardStreamChannel?.setStreamHandler(AddIdCardListen())
    addFingerStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, "${COM}/addFinger")
    addFingerStreamChannel?.setStreamHandler(AddFingerListen())
    blueStateStreamChannel = EventChannel(flutterPluginBinding.binaryMessenger, "${COM}/blueState")
    blueStateStreamChannel?.setStreamHandler(BlueStateListen())
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "debugLog" -> {
        openLog = call.argument<Boolean>("open") ?: false
      }
      "isBLEEnabled" -> {
        //1.用于判断蓝牙是否可用
        result.success(TTLockClient.getDefault().isBLEEnabled(context))
      }
      "requestBleEnable" -> {
        //2.请求打开蓝牙
        TTLockClient.getDefault().requestBleEnable(context)
        result.success(null)
      }
      "prepareBTService" -> {
        //3.蓝牙操作前的准备工作
        print("初始化通通锁 $context")
        context?.let { TTLockClient.getDefault().prepareBTService(it) }
        result.success(null)
      }
      "stopBTService" -> {
        //4.停止蓝牙服务并且释放资源
        TTLockClient.getDefault().stopBTService()
        result.success(null)
      }
//            "startScanLock" -> {
//                //5.启动扫描锁
//                print("开始扫描")
////                var cb  = call.argument<>("cb")
//                TTLockClient.getDefault().startScanLock(object : ScanLockCallback {
//                    override fun onFail(error: LockError?) {
//                        print("onFail ---- $error")
//                    }
//
//                    override fun onScanLockSuccess(device: ExtendedBluetoothDevice) {
//                        print("onScanLockSuccess ---- $device")
////                        result.success(List<>)
//                    }
//                })
//            }
      "stopScanLock" -> {
        //6.停止扫描
        TTLockClient.getDefault().stopScanLock()
        result.success(null)
      }
      "initLock" -> {
        // 7.初始化锁
        val js = call.argument<String>("extendedBluetoothDevice")
        val device = Gson().fromJson<ExtendedBluetoothDevice>(js, ExtendedBluetoothDevice::class.java)
        TTLockClient.getDefault().initLock(device, object : InitLockCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onInitLockSuccess(lockData: String?, specialValue: Int) {
            result.success("{\"specialValue\":$specialValue,\"lockData\":\"$lockData\"}")
          }
        })
      }
      "resetEkey" -> {
        //8.重置电子钥匙
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().resetEkey(lockData, lockMac, object : ResetKeyCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onResetKeySuccess(lockFlagPos: Int) {
            result.success(lockFlagPos)
          }

        })
      }
      "resetLock" -> {
        //9.重置锁
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().resetLock(lockData, lockMac, object : ResetLockCallback {
          override fun onResetLockSuccess() {
            result.success(null)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

        })
      }
      "controlLock" -> {
        // 10.开关锁操作(ControlAction.LOCK and ControlAction.UNLOCK)
        val controlAction = call.argument<Int>("controlAction") ?: 0
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""

        TTLockClient.getDefault().controlLock(controlAction, lockData, lockMac, object : ControlLockCallback {
          override fun onControlLockSuccess(lockAction: Int, battery: Int, uniqueId: Int) {
            result.success("{\"lockAction\":$lockAction,\"battery\":$battery,\"uniqueId\":$uniqueId}")
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

        })
      }
      "getMuteModeState" -> {
        //11.获取音频的开关状态
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getMuteModeState(lockData, lockMac, object : GetLockMuteModeStateCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetMuteModeStateSuccess(enabled: Boolean) {
            result.success(enabled)
          }

        })
      }
      "setMuteMode" -> {
        //12.设置音频开关状态
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        val enable = call.argument<Boolean>("enable") ?: true
        TTLockClient.getDefault().setMuteMode(enable, lockData, lockMac, object : SetLockMuteModeCallback {
          override fun onSetMuteModeSuccess(enabled: Boolean) {
            result.success(enabled)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }
        })
      }
      "setRemoteUnlockSwitchState" -> {
        //13.设置远程开锁开关
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        val enable = call.argument<Boolean>("enable") ?: true
        TTLockClient.getDefault().setRemoteUnlockSwitchState(enable, lockData, lockMac, object : SetRemoteUnlockSwitchCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onSetRemoteUnlockSwitchSuccess(specialValue: Int) {
            result.success(specialValue)
          }

        })
      }
      "getRemoteUnlockSwitchState" -> {
        //14.获取远程开锁开关状态
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getRemoteUnlockSwitchState(lockData, lockMac, object : GetRemoteUnlockStateCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetRemoteUnlockSwitchStateSuccess(enabled: Boolean) {
            result.success(enabled)
          }
        })
      }
      "setLockTime" -> {
        //15.校准锁时间
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        val timestamp = call.argument<Long>("timestamp") ?: 0
        TTLockClient.getDefault().setLockTime(timestamp, lockData, lockMac, object : SetLockTimeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onSetTimeSuccess() {
            result.success(null)
          }
        })
      }
      "getLockTime" -> {
        //16.获取锁时间
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getLockTime(lockData, lockMac, object : GetLockTimeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetLockTimeSuccess(lockTimestamp: Long) {
            result.success(lockTimestamp)
          }

        })
      }
      "getOperationLog" -> {
        //17.获取锁日志
        val logType = call.argument<Int>("logType") ?: LogType.ALL
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getOperationLog(logType, lockData, lockMac, object : GetOperationLogCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetLogSuccess(log: String?) {
            result.success(log)
          }

        })
      }
      "getBatteryLevel" -> {
        //18.获取锁电量
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getBatteryLevel(lockData, lockMac, object : GetBatteryLevelCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetBatteryLevelSuccess(electricQuantity: Int) {
            result.success(electricQuantity)
          }

        })
      }
      "setAutomaticLockingPeriod" -> {
        //19.设置自动闭锁时间
        val seconds = call.argument<Int>("seconds") ?: 0
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().setAutomaticLockingPeriod(seconds, lockData, lockMac, object : SetAutoLockingPeriodCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onSetAutoLockingPeriodSuccess() {
            result.success(null)
          }

        })
      }
      "createCustomPasscode" -> {
        //20.设置自定义密码（4 – 9位密码）
        val passcode = call.argument<String>("passcode") ?: ""
        val startDate = call.argument<Long>("startDate") ?: 0
        val endDate = call.argument<Long>("endDate") ?: 0
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().createCustomPasscode(passcode, startDate, endDate, lockData, lockMac, object : CreateCustomPasscodeCallback {
          override fun onCreateCustomPasscodeSuccess(passcode: String?) {
            result.success(passcode)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

        })
      }
      "modifyPasscode" -> {
        //21.修改密码
        val originalCode = call.argument<String>("originalCode") ?: ""
        val newCode = call.argument<String>("newCode") ?: ""
        val startDate = call.argument<Long>("startDate") ?: 0
        val endDate = call.argument<Long>("endDate") ?: 0
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().modifyPasscode(originalCode, newCode, startDate, endDate, lockData, lockMac, object : ModifyPasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onModifyPasscodeSuccess() {
            result.success(null)
          }

        })
      }
      "deletePasscode" -> {
        //22.删除密码
        val passcode = call.argument<String>("passcode") ?: ""
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().deletePasscode(passcode, lockData, lockMac, object : DeletePasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onDeletePasscodeSuccess() {
            result.success(null)
          }

        })
      }
      "resetPasscode" -> {
        //23.重置密码(之前生成的密码都将失效)
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().resetPasscode(lockData, lockMac, object : ResetPasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onResetPasscodeSuccess(pwdInfo: String?, timestamp: Long) {
            result.success("{\"pwdInfo\":\"$pwdInfo\",\"timestamp\":$timestamp}")
          }

        })
      }
      "getAllValidPasscodes" -> {
        //24.获取锁里所有的有效密码
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getAllValidPasscodes(lockData, lockMac, object : GetAllValidPasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetAllValidPasscodeSuccess(passcodeStr: String?) {
            result.success(passcodeStr)
          }

        })
      }
      "modifyAdminPasscode" -> {
        // 25.修改管理码
        val newPasscode = call.argument<String>("newPasscode") ?: ""
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().modifyAdminPasscode(newPasscode, lockData, lockMac, object : ModifyAdminPasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onModifyAdminPasscodeSuccess(passcode: String?) {
            result.success(passcode)
          }

        })
      }
      "getAdminPasscode" -> {
        // 25.1.读取管理码
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getAdminPasscode(lockData, lockMac, object : GetAdminPasscodeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetAdminPasscodeSuccess(passcode: String?) {
            result.success(passcode)
          }

        })
      }
      "modifyICCardValidityPeriod" -> {
        //27.修改IC卡有效期
        val startDate = call.argument<Long>("startDate") ?: 0
        val endDate = call.argument<Long>("endDate") ?: 0
        val cardNum = call.argument<String>("cardNum") ?: ""
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().modifyICCardValidityPeriod(startDate, endDate, cardNum, lockData, lockMac, object : ModifyICCardPeriodCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onModifyICCardPeriodSuccess() {
            result.success(null)
          }

        })
      }
      "getAllValidICCards" -> {
        //28.获取所有有效的IC卡
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getAllValidICCards(lockData, lockMac, object : GetAllValidICCardCallback {
          override fun onGetAllValidICCardSuccess(cardDataStr: String?) {
            result.success(cardDataStr)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }
        })
      }
      "deleteICCard" -> {
        //29.删除IC卡
        val cardNum = call.argument<String>("cardNum") ?: ""
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().deleteICCard(cardNum, lockData, lockMac, object : DeleteICCardCallback {
          override fun onDeleteICCardSuccess() {
            result.success(null)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

        })
      }
      "clearAllICCard" -> {
        //30.清空IC卡
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().clearAllICCard(lockData, lockMac, object : ClearAllICCardCallback {
          override fun onClearAllICCardSuccess() {
            result.success(null)
          }

          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

        })
      }
      "getAllValidFingerprints" -> {
        // 32.获取所有有效的指纹
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getAllValidFingerprints(lockData, lockMac, object : GetAllValidFingerprintCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetAllFingerprintsSuccess(fingerprintStr: String?) {
            result.success(fingerprintStr)
          }

        })
      }
      "deleteFingerprint" -> {
        //33.删除指纹
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        val fingerprintNum = call.argument<String>("fingerprintNum") ?: ""
        TTLockClient.getDefault().deleteFingerprint(fingerprintNum, lockData, lockMac, object : DeleteFingerprintCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onDeleteFingerprintSuccess() {
            result.success(null)
          }

        })
      }
      "clearAllFingerprints" -> {
        //34.清空所有指纹
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().clearAllFingerprints(lockData, lockMac, object : ClearAllFingerprintCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onClearAllFingerprintSuccess() {
            result.success(null)
          }

        })
      }
      "modifyFingerprintValidityPeriod" -> {
        //35.修改指纹有效期
        val startDate = call.argument<Long>("startDate") ?: 0
        val endDate = call.argument<Long>("endDate") ?: 0
        val fingerprintNum = call.argument<String>("fingerprintNum") ?: ""
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().modifyFingerprintValidityPeriod(startDate, endDate, fingerprintNum, lockData, lockMac, object : ModifyFingerprintPeriodCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onModifyPeriodSuccess() {
            result.success(null)
          }

        })
      }
      "getPassageMode" -> {
        //36.获取常开模式数据
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().getPassageMode(lockData, lockMac, object : GetPassageModeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onGetPassageModeSuccess(passageModeData: String?) {
            result.success(passageModeData)
          }

        })
      }
      "setPassageMode" -> {
        // 37.设置常开模式
        val modeData = Gson().fromJson<PassageModeConfig>(call.argument<String>("passageModeConfig")
          ?: "", PassageModeConfig::class.java)
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().setPassageMode(modeData, lockData, lockMac, object : SetPassageModeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onSetPassageModeSuccess() {
            result.success(null)
          }
        })
      }
      "deletePassageMode" -> {
        // 38.删除常开模式
        val modeData = Gson().fromJson<PassageModeConfig>(call.argument<String>("passageModeConfig")
          ?: "", PassageModeConfig::class.java)
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().deletePassageMode(modeData, lockData, lockMac, object : DeletePassageModeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onDeletePassageModeSuccess() {
            result.success(null)
          }
        })
      }
      "clearPassageMode" -> {
        // 39.清空常开模式
        val lockData = call.argument<String>("lockData") ?: ""
        val lockMac = call.argument<String>("lockMac") ?: ""
        TTLockClient.getDefault().clearPassageMode(lockData, lockMac, object : ClearPassageModeCallback {
          override fun onFail(error: LockError?) {
            handler?.post {
              result.error(error?.errorCode, error?.errorMsg, error?.description)
            }
          }

          override fun onClearPassageModeSuccess() {
            result.success(null)
          }
        })
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    methodChannel?.setMethodCallHandler(null)
    methodChannel = null
    scanStreamChannel?.setStreamHandler(null)
    scanStreamChannel = null
    addICCardStreamChannel?.setStreamHandler(null)
    addICCardStreamChannel = null
    addFingerStreamChannel?.setStreamHandler(null)
    addFingerStreamChannel = null
    blueStateStreamChannel?.setStreamHandler(null)
    blueStateStreamChannel = null
    context = null
    TTLockClient.getDefault().stopBTService()
    handler?.removeCallbacksAndMessages(null)
  }

  override fun onDetachedFromActivity() {
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    context = binding.activity
    handler = Handler(Looper.getMainLooper())
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }

  private fun print(msg: String) {
    if (openLog) {
      Log.d(TAG, msg)
    }
  }

  // 监听蓝牙打开状态
  class BlueStateListen : EventChannel.StreamHandler {
    private var receiver: BroadcastReceiver? = null

    private fun createReceiver(events: EventChannel.EventSink?) {
      receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
          if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
              BluetoothAdapter.STATE_ON -> {
                events?.success(2)
                Log.d("TAG", "STATE_ON")
              }
              BluetoothAdapter.STATE_TURNING_ON -> {
                events?.success(1)
                Log.d("TAG", "STATE_ON")
              }

              BluetoothAdapter.STATE_TURNING_OFF -> {
                events?.success(3)
                Log.d("TAG", "STATE_TURNING_OFF")
              }
              BluetoothAdapter.STATE_OFF -> {
                events?.success(0)
                Log.d("TAG", "STATE_OFF")
              }
            }
          }
        }

      }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      val filter = IntentFilter()
      filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
      createReceiver(events)
      context?.registerReceiver(receiver, filter)
    }

    override fun onCancel(arguments: Any?) {
      receiver?.let {
        context?.unregisterReceiver(it)
      }
    }

  }

  class StartScanListen : EventChannel.StreamHandler {
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      //5.启动扫描锁
      print("开始扫描")
      TTLockClient.getDefault().startScanLock(object : ScanLockCallback {
        override fun onFail(error: LockError?) {
          print("onFail ---- $error")
          events?.error(error?.errorCode, error?.errorMsg, error?.description)
        }

        override fun onScanLockSuccess(device: ExtendedBluetoothDevice) {
          print("onScanLockSuccess ---- $device")
//                val deviceJob = JSONObject()
//                deviceJob.put("name", device.name)
//                deviceJob.put("address", device.address)
//                deviceJob.put("rssi", device.rssi)
//                deviceJob.put("protocolType", device.protocolType)
//                deviceJob.put("protocolVersion", device.protocolVersion)
//                deviceJob.put("scene", device.scene)
//                deviceJob.put("groupId", device.groupId)
//                deviceJob.put("orgId", device.orgId)
//                deviceJob.put("lockType", device.lockType)
//                deviceJob.put("isTouch", device.isTouch)
//                deviceJob.put("isSettingMode", device.isSettingMode)
//                deviceJob.put("isWristband", device.isWristband)
////                deviceJob.put("isUnlock",device.is)
////                deviceJob.put("txPowerLevel",device.txPowerLevel)
//                deviceJob.put("batteryCapacity", device.batteryCapacity)
//                deviceJob.put("date", device.date)
//                deviceJob.put("device", device.device)
          val scanRecordJarr = JSONArray()
          device.scanRecord?.forEach {
            scanRecordJarr.put(it)
          }
//                deviceJob.put("scanRecord", scanRecordJarr)
          val gson = Gson()
          events?.success(gson.toJson(device).toString())
        }
      })
    }

    override fun onCancel(arguments: Any?) {
      TTLockClient.getDefault().stopScanLock()
    }
  }

  class AddIdCardListen : EventChannel.StreamHandler {
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      //26.添加IC卡
      if (arguments != null && arguments is String) {
        val addIcCardBean = Gson().fromJson<AddIcCardBean>(arguments, AddIcCardBean::class.java)
        TTLockClient.getDefault().addICCard(addIcCardBean.startDate, addIcCardBean.endDate, addIcCardBean.lockData, addIcCardBean.lockMac, object : AddICCardCallback {
          override fun onFail(error: LockError?) {
            events?.error(error?.errorCode, error?.errorMsg, error?.description)
          }

          override fun onAddICCardSuccess(cardNum: Long) {
            events?.success(cardNum)
          }

          override fun onEnterAddMode() {
            events?.success(null)
          }

        })
      }
    }

    override fun onCancel(arguments: Any?) {

    }

  }

  class AddFingerListen : EventChannel.StreamHandler {
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
      //31.添加指纹
      if (arguments != null && arguments is String) {
        val addFingerBean = Gson().fromJson<AddFingerBean>(arguments, AddFingerBean::class.java)
        TTLockClient.getDefault().addFingerprint(addFingerBean.startDate, addFingerBean.endDate, addFingerBean.lockData, addFingerBean.lockMac, object : AddFingerprintCallback {
          override fun onAddFingerpintFinished(fingerprintNum: Long) {
            events?.success("{\"type\":\"onAddFingerpintFinished\",\"fingerprintNum\":$fingerprintNum}")
          }

          override fun onFail(error: LockError?) {
            events?.error(error?.errorCode, error?.errorMsg, error?.description)
          }

          override fun onEnterAddMode(totalCount: Int) {
            //onEnterAddMode:锁进入添加模式可以添加指纹。
            //totalCount:需要采集指纹的总次数。
            events?.success("{\"type\":\"onEnterAddMode\",\"totalCount\":$totalCount}")
          }

          override fun onCollectFingerprint(currentCount: Int) {
            //currentCount :当前采集指纹的次数。
            events?.success("{\"type\":\"onCollectFingerprint\",\"currentCount\":$currentCount}")
          }

        })
      }
    }

    override fun onCancel(arguments: Any?) {

    }

  }
}