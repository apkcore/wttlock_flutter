class ExtendedBluetoothDevice {
  int? date;
  int ?disconnectStatus;
  int ?groupId;
  bool? isBicycleLock;
  bool? isCyLinder;
  bool ?isDfuMode;
  bool? isGlassLock;
  bool ?isLockcar;
  bool ?isNoLockService;
  bool? isPadLock;
  bool ?isRemoteControlDevice;
  bool ?isRoomLock;
  bool? isSafeLock;
  bool? isTouch;
  bool? isUnlock;
  bool? isWristband;
  int ?lockType;
  int? orgId;
  int? parkStatus;
  int ?protocolType;
  int? protocolVersion;
  int? remoteUnlockSwitch;
  int? scene;
  int ?txPowerLevel;
  int? batteryCapacity;
  Device ?device;
  bool? isSettingMode;
  String? mAddress;
  String? name;
  int ?rssi;
  List? scanRecord;
  String ?sourceJSON;

  ExtendedBluetoothDevice(
      {this.date,
      this.disconnectStatus,
      this.groupId,
      this.isBicycleLock,
      this.isCyLinder,
      this.isDfuMode,
      this.isGlassLock,
      this.isLockcar,
      this.isNoLockService,
      this.isPadLock,
      this.isRemoteControlDevice,
      this.isRoomLock,
      this.isSafeLock,
      this.isTouch,
      this.isUnlock,
      this.isWristband,
      this.lockType,
      this.orgId,
      this.parkStatus,
      this.protocolType,
      this.protocolVersion,
      this.remoteUnlockSwitch,
      this.scene,
      this.txPowerLevel,
      this.batteryCapacity,
      this.device,
      this.isSettingMode,
      this.mAddress,
      this.name,
      this.rssi,
      this.scanRecord,
      this.sourceJSON});
  factory ExtendedBluetoothDevice.fromJson(Map map) {
    ExtendedBluetoothDevice d = ExtendedBluetoothDevice();
    d.date = map["date"];
    d.disconnectStatus = map["disconnectStatus"];
    d.groupId = map["groupId"];
    d.isBicycleLock = map["isBicycleLock"];
    d.isCyLinder = map["isCyLinder"];
    d.isDfuMode = map["isDfuMode"];
    d.isGlassLock = map["isGlassLock"];
    d.isLockcar = map["isLockcar"];
    d.isNoLockService = map["isNoLockService"];
    d.isPadLock = map["isPadLock"];
    d.isRemoteControlDevice = map["isRemoteControlDevice"];
    d.isRoomLock = map["isRoomLock"];
    d.isSafeLock = map["isSafeLock"];
    d.isTouch = map["isTouch"];
    d.isUnlock = map["isUnlock"];
    d.isWristband = map["isWristband"];
    d.lockType = map["lockType"];
    d.orgId = map["orgId"];
    d.parkStatus = map["parkStatus"];
    d.protocolType = map["protocolType"];
    d.protocolVersion = map["protocolVersion"];
    d.remoteUnlockSwitch = map["remoteUnlockSwitch"];
    d.scene = map["scene"];
    d.txPowerLevel = map["txPowerLevel"];
    d.batteryCapacity = map["batteryCapacity"];
    Device de = Device();
    de.mAddress = map["device"]["mAddress"];
    d.device = de;
    d.isSettingMode = map["isSettingMode"];
    d.mAddress = map["mAddress"];
    d.name = map["name"];
    d.rssi = map["rssi"];
    d.scanRecord = map["scanRecord"];
    return d;
  }

  @override
  String toString() {
    return 'ExtendedBluetoothDevice{date: $date, disconnectStatus: $disconnectStatus, groupId: $groupId, isBicycleLock: $isBicycleLock, isCyLinder: $isCyLinder, isDfuMode: $isDfuMode, isGlassLock: $isGlassLock, isLockcar: $isLockcar, isNoLockService: $isNoLockService, isPadLock: $isPadLock, isRemoteControlDevice: $isRemoteControlDevice, isRoomLock: $isRoomLock, isSafeLock: $isSafeLock, isTouch: $isTouch, isUnlock: $isUnlock, isWristband: $isWristband, lockType: $lockType, orgId: $orgId, parkStatus: $parkStatus, protocolType: $protocolType, protocolVersion: $protocolVersion, remoteUnlockSwitch: $remoteUnlockSwitch, scene: $scene, txPowerLevel: $txPowerLevel, batteryCapacity: $batteryCapacity, device: $device, isSettingMode: $isSettingMode, mAddress: $mAddress, name: $name, rssi: $rssi, scanRecord: $scanRecord}';
  }
}

class Device {
  String? mAddress;

  @override
  String toString() {
    return 'Device{mAddress: $mAddress}';
  }
}
