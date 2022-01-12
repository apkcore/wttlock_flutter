//
// import 'dart:async';
//
// import 'package:flutter/services.dart';
//
// class WttlockFlutter {
//   static const MethodChannel _channel = MethodChannel('wttlock_flutter');
//
//   static Future<String?> get platformVersion async {
//     final String? version = await _channel.invokeMethod('getPlatformVersion');
//     return version;
//   }
// }

import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:wttlock_flutter/bean/extended_bluetooth_device.dart';
import 'package:wttlock_flutter/bean/init_lock_result.dart';

import 'bean/control_lock_result.dart';
import 'bean/passage_mode_config.dart';
import 'bean/reset_passcode_result.dart';

typedef OnResult = void Function(int code, Object? data);

class TTResult<T> {
  int? code;
  T? data;

  TTResult({this.code, this.data});

  @override
  String toString() {
    return 'TTResult{code: $code, data: $data}';
  }
}

/// 插件常用功能
/// https://open.ttlock.com/doc/sdk/v3/android/lockInterface

class TtlockPlugin {
  static const String _COM = "com.wave.ttlock_plugin";

  static const MethodChannel _channel = MethodChannel('$_COM/plugin');

  static const EventChannel _scanEventChannel =
  EventChannel('$_COM/startScanLock');
  static const EventChannel _addICCardEventChannel =
  EventChannel('$_COM/addIcCard');
  static const EventChannel _addFingerEventChannel =
  EventChannel('$_COM/addFinger');
  static const EventChannel _blueStateEventChannel =
  EventChannel('$_COM/blueState');

  static StreamSubscription? _addICCardStreamSubscription;
  static StreamSubscription? _scanStreamSubscription;
  static StreamSubscription? _addFingerStreamSubscription;
  static StreamSubscription? _blueStateStreamSubscription;

  static void debugLog({open = false}) {
    _channel.invokeMethod('debugLog', {"open": open});
  }

  /// 1.用于判断蓝牙是否可用
  static Future<TTResult<bool>> isBLEEnabled() async {
    TTResult<bool> result = TTResult();
    try {
      bool b = await _channel.invokeMethod('isBLEEnabled');
      result.code = 0;
      result.data = b;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 2.请求打开蓝牙
  static Future<TTResult> requestBleEnable() async {
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('requestBleEnable');
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  static void blueStateListener(OnResult cb) {
    _blueStateStreamSubscription?.cancel();
    _blueStateStreamSubscription =
        _blueStateEventChannel.receiveBroadcastStream().listen((result) {
          cb(0, result);
        }, onError: (e) {
          cb(-1, e);
          print("error $e");
        }, onDone: () {
          print("done -- ");
        }, cancelOnError: true);
  }

  static void stopBlueListen() {
    _blueStateStreamSubscription?.cancel();
  }

  /// 3.蓝牙操作前的准备工作
  static Future<TTResult> prepareBTService() async {
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('prepareBTService');
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 4.停止扫描
  static Future<TTResult> stopScanLock() async {
    _scanStreamSubscription?.cancel();
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('stopScanLock');
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 5.启动扫描锁
  static void startScanLock(OnResult cb) {
    _scanStreamSubscription?.cancel();
    _scanStreamSubscription =
        _scanEventChannel.receiveBroadcastStream().listen((result) {
          if (result != null && result is String) {
            Map map = jsonDecode(result);
            var d = ExtendedBluetoothDevice.fromJson(map);
            d.sourceJSON = result;
//        print("receiveBroadcastStream $d");
            cb(0, d);
          }
        }, onError: (e) {
          cb(-1, e);
          print("error $e");
        }, onDone: () {
          print("done -- ");
        }, cancelOnError: true);
  }

  /// 6.停止蓝牙服务并且释放资源
  static Future<TTResult> stopBTService() async {
    _scanStreamSubscription?.cancel();
    _addICCardStreamSubscription?.cancel();
    _addFingerStreamSubscription?.cancel();
    _blueStateStreamSubscription?.cancel();
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('stopBTService');
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 7.初始化锁
  static Future<TTResult<InitLockResult>> initLock(
      String extendedBluetoothDevice) async {
    TTResult<InitLockResult> result = TTResult();
    try {
      String json = await _channel.invokeMethod(
          'initLock', {"extendedBluetoothDevice": extendedBluetoothDevice});
      print('`init lock---- $json');
      if (json != null) {
        Map map = jsonDecode(json);
        InitLockResult initLockResult = InitLockResult();
        initLockResult.lockData = map['lockData'] ?? '';
        initLockResult.specialValue = map['specialValue'] ?? 0;
        result.code = 0;
        result.data = initLockResult;
      }
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 8.重置电子钥匙 重置电子钥匙,成功之后, lockFlagPos将会发生改变。
  static Future<TTResult<int>> resetEkey(lockData, lockMac) async {
    lockMac = toColonMac(lockMac);
    TTResult<int> result = TTResult();
    try {
      int data = await _channel.invokeMethod(
          'resetEkey', {"lockData": lockData, "lockMac": lockMac});
      result.code = 0;
      result.data = data;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 9.重置锁 重置锁意味着恢复出厂设置,如果你想要再次使用该锁,你需要重新初始化该锁。
  static Future<TTResult> resetLock(lockData, lockMac) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod(
          'resetLock', {"lockData": lockData, "lockMac": lockMac});
      result.code = 0;
    } catch (e) {
      print('error--------- $e');
      result.code = -1;
    }
    return result;
  }

  /// 10.开关锁操作(ControlAction.LOCK and ControlAction.UNLOCK)
  static Future<TTResult<ControlLockResult>> controlLock(
      int controlAction, String lockData, String lockMac) async {
    lockMac = toColonMac(lockMac);
    TTResult<ControlLockResult> result = TTResult();
    try {
      String json = await _channel.invokeMethod('controlLock', {
        "controlAction": controlAction,
        "lockData": lockData,
        "lockMac": lockMac
      });
      if (json != null) {
        Map map = jsonDecode(json);
        ControlLockResult controlLockResult = ControlLockResult();
        controlLockResult.lockAction = map['lockAction'];
        controlLockResult.uniqueId = map['uniqueId'];
        controlLockResult.battery = map['battery'];
        result.code = 0;
        result.data = controlLockResult;
      }
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 11.获取音频的开关状态
  static Future<TTResult<bool>> getMuteModeState(
      String lockData, String lockMac) async {
    lockMac = toColonMac(lockMac);
    TTResult<bool> result = TTResult();
    try {
      bool b = await _channel.invokeMethod('getMuteModeState', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = b;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 12.设置音频开关状态 true – on, false – off
  static Future<TTResult<bool>> setMuteMode(
      String lockData, String lockMac, bool enable) async {
    TTResult<bool> result = TTResult();
    try {
      bool b = await _channel.invokeMethod('setMuteMode', {
        "lockData": lockData,
        "lockMac": lockMac,
        "enable": enable,
      });
      result.code = 0;
      result.data = b;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 13.设置远程开锁开关 true – on, false – off
  static Future<TTResult<int>> setRemoteUnlockSwitchState(
      String lockData, String lockMac, bool enable) async {
    lockMac = toColonMac(lockMac);
    TTResult<int> result = TTResult();
    try {
      int i = await _channel.invokeMethod('setRemoteUnlockSwitchState', {
        "lockData": lockData,
        "lockMac": lockMac,
        "enable": enable,
      });
      result.code = 0;
      result.data = i;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 14.获取远程开锁开关状态
  static Future<TTResult<bool>> getRemoteUnlockSwitchState(
      String lockData, String lockMac) async {
    lockMac = toColonMac(lockMac);
    TTResult<bool> result = TTResult();
    try {
      bool b = await _channel.invokeMethod('getRemoteUnlockSwitchState', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = b;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 15.校准锁时间
  static Future<TTResult> setLockTime(
      String lockData, String lockMac, int timestamp) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('setLockTime', {
        "lockData": lockData,
        "lockMac": lockMac,
        "timestamp": timestamp,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 16.获取锁时间
  static Future<TTResult<int>> getLockTime(
      String lockData,
      String lockMac,
      ) async {
    TTResult<int> result = TTResult();
    lockMac = toColonMac(lockMac);
    try {
      int i = await _channel.invokeMethod('getLockTime', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = i;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 17.获取锁日志 LogType.NEW – 获取最新的操作记录,既上次获取之后新增的。 LogType. ALL – 锁里面存储的完整记录。
  static Future<TTResult<String>> getOperationLog(
      int logType,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getOperationLog', {
        "logType": logType,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 18.获取锁电量
  static Future<TTResult<int>> getBatteryLevel(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<int> result = TTResult();
    try {
      int i = await _channel.invokeMethod('getBatteryLevel', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = i;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 19.设置自动闭锁时间
  static Future<TTResult> setAutomaticLockingPeriod(
      int seconds,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<int> result = TTResult();
    try {
      await _channel.invokeMethod('setAutomaticLockingPeriod', {
        "seconds": seconds,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 20.设置自定义密码（4 – 9位密码）
  static Future<TTResult<String>> createCustomPasscode(
      String passcode,
      int startDate,
      int endDate,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('createCustomPasscode', {
        "passcode": passcode,
        "startDate": startDate,
        "endDate": endDate,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 21.修改密码 newPasscode为空表示不修改密码。 startDate 和endDate都为0表示不修改有效期
  static Future<TTResult> modifyPasscode(
      String originalCode,
      String newCode,
      int startDate,
      int endDate,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('modifyPasscode', {
        "originalCode": originalCode,
        "newCode": newCode,
        "startDate": startDate,
        "endDate": endDate,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 22.删除密码
  static Future<TTResult> deletePasscode(
      String passcode,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('deletePasscode', {
        "passcode": passcode,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 23.重置密码(之前生成的密码都将失效)
  static Future<TTResult<ResetPasscodeResult>> resetPasscode(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    //"{\"pwdInfo\":$pwdInfo,\"timestamp\":$timestamp}"
    TTResult<ResetPasscodeResult> result = TTResult();
    try {
      String json = await _channel.invokeMethod('resetPasscode', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      if (json != null) {
        Map map = jsonDecode(json);
        ResetPasscodeResult result2 = ResetPasscodeResult();
        result2.pwdInfo = map['pwdInfo'];
        result2.timestamp = map['timestamp'];
        result.code = 0;
        result.data = result2;
      } else {
        result.code = -1;
      }
    } catch (e) {
      print('resetPasscode error $e');
      result.code = -1;
    }
    return result;
  }

  /// 24.获取锁里所有的有效密码
  static Future<TTResult<String>> getAllValidPasscodes(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getAllValidPasscodes', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 25.修改管理码
  static Future<TTResult<String>> modifyAdminPasscode(
      String newPasscode,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('modifyAdminPasscode', {
        "newPasscode": newPasscode,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 25.1.读取管理码
  static Future<TTResult<String>> getAdminPasscode(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getAdminPasscode', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 26.添加IC卡
  /// startDate和endDate都为0表示添加永久期限的IC卡。
  /// onEnterAddMode:进入添加IC模式，可以放入卡片进行添加操作。
  static void addICCard(int startDate, int endDate, String lockData,
      String lockMac, OnResult cb) {
    lockMac = toColonMac(lockMac);
    _addICCardStreamSubscription?.cancel();
    _addICCardStreamSubscription = _addICCardEventChannel
        .receiveBroadcastStream(
        '{"startDate":$startDate,"endDate":$endDate,"lockData":"$lockData","lockMac":"$lockMac"}')
        .listen((result) {
      if (result == null) {
        print('进入添加IC模式');
        cb(0, null);
      } else if (result is int) {
        print('添加IC卡为：$result');
        cb(1, result);
      }
    }, onError: (e) {
      cb(-1, null);
      print("error $e");
    }, onDone: () {
      print("done -- ");
    }, cancelOnError: true);
  }

  /// 27.修改IC卡有效期
  static Future<TTResult> modifyICCardValidityPeriod(
      int startDate,
      int endDate,
      String cardNum,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('modifyICCardValidityPeriod', {
        "startDate": startDate,
        "endDate": endDate,
        "cardNum": cardNum,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 28.获取所有有效的IC卡
  static Future<TTResult<String>> getAllValidICCards(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getAllValidICCards', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 29.删除IC卡
  static Future<TTResult> deleteICCard(
      String cardNum,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    print('$cardNum,$lockMac,$lockData');
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('deleteICCard', {
        "cardNum": cardNum,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 30.清空IC卡
  static Future<TTResult> clearAllICCard(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('clearAllICCard', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 31.添加指纹
  /// startDate 和endDate都为0意味着添加永久指纹。
  /// onEnterAddMode:锁进入添加模式可以添加指纹。
  /// totalCount:需要采集指纹的总次数。
  /// currentCount :当前采集指纹的次数。
  static void addFingerprint(int startDate, int endDate, String lockData,
      String lockMac, OnResult cb) {
    lockMac = toColonMac(lockMac);
    _addFingerStreamSubscription?.cancel();
    _addFingerStreamSubscription = _addFingerEventChannel
        .receiveBroadcastStream(
        '{"startDate":$startDate,"endDate":$endDate,"lockData":"$lockData","lockMac":"$lockMac"}')
        .listen((result) {
      if (result != null && result is String) {
        Map map = jsonDecode(result);
        String type = map['type'];
        if (type == 'onAddFingerpintFinished') {
          //"{\"type\":\"onAddFingerpintFinished\",\"fingerprintNum\":$fingerprintNum}"
          cb(2, map['fingerprintNum']);
        } else if (type == 'onEnterAddMode') {
          //onEnterAddMode:锁进入添加模式可以添加指纹。
          //totalCount:需要采集指纹的总次数。
          //"{\"type\":\"onEnterAddMode\",\"totalCount\":$totalCount}"
          cb(0, map['totalCount']);
        } else if (type == 'onCollectFingerprint') {
          //currentCount :当前采集指纹的次数。
          //"{\"type\":\"onCollectFingerprint\",\"currentCount\":$currentCount}"
          cb(1, map['currentCount']);
        }
      }
    }, onError: (e) {
      print("error $e");
    }, onDone: () {
      print("done -- ");
    }, cancelOnError: true);
  }

  /// 32.获取所有有效的指纹
  static Future<TTResult<String>> getAllValidFingerprints(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getAllValidFingerprints', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 33.删除指纹
  static Future<TTResult<String>> deleteFingerprint(
      String fingerprintNum,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('deleteFingerprint', {
        "fingerprintNum": fingerprintNum,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 34.清空所有指纹
  static Future<TTResult> clearAllFingerprints(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('clearAllFingerprints', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 35.修改指纹有效期
  static Future<TTResult> modifyFingerprintValidityPeriod(
      int startDate,
      int endDate,
      String fingerprintNum,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('modifyFingerprintValidityPeriod', {
        "startDate": startDate,
        "endDate": endDate,
        "fingerprintNum": fingerprintNum,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 36.获取常开模式数据
  static Future<TTResult<String>> getPassageMode(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult<String> result = TTResult();
    try {
      String str = await _channel.invokeMethod('getPassageMode', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
      result.data = str;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// todo 转json 37.设置常开模式
  static Future<TTResult> setPassageMode(
      PassageModeConfig passageModeConfig,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('setPassageMode', {
        "passageModeConfig": passageModeConfig,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 38.删除常开模式
  static Future<TTResult> deletePassageMode(
      PassageModeConfig passageModeConfig,
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('deletePassageMode', {
        "passageModeConfig": passageModeConfig,
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  /// 39.清空常开模式
  static Future<TTResult> clearPassageMode(
      String lockData,
      String lockMac,
      ) async {
    lockMac = toColonMac(lockMac);
    TTResult result = TTResult();
    try {
      await _channel.invokeMethod('clearPassageMode', {
        "lockData": lockData,
        "lockMac": lockMac,
      });
      result.code = 0;
    } catch (e) {
      result.code = -1;
    }
    return result;
  }

  static String toColonMac(String mac) {

    if (null == mac) {
      return mac;
    }

    mac = mac.trim();

    if (mac.length == 0) {
      return mac;
    }

    if (mac.length != 12 || mac.contains(':')) {
      assert(mac.length == 17, 'no correct mac($mac)');
      return mac;
    } else {
      String macColon = mac.substring(0, 2);
      for (int i = 2; i < mac.length; i += 2) {
        macColon += ':' + mac.substring(i, i + 2);
      }
      return macColon;
    }
  }
}

