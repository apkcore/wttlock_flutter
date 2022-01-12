package com.wave.wttlock_flutter;

import android.bluetooth.BluetoothDevice;

import com.ttlock.bl.sdk.api.ExtendedBluetoothDevice;
import com.wave.wttlock_flutter.bean.ExtendedDev;

public class Util {
    public static ExtendedBluetoothDevice changeExtended(ExtendedDev extendedDev,BluetoothDevice device){
        ExtendedBluetoothDevice extendedBluetoothDevice = new ExtendedBluetoothDevice();
        extendedBluetoothDevice.setDate(extendedDev.date);
        extendedBluetoothDevice.disconnectStatus= extendedDev.disconnectStatus;
        extendedBluetoothDevice.groupId = extendedDev.groupId;
        extendedBluetoothDevice.setBicycleLock(extendedDev.isBicycleLock);
        extendedBluetoothDevice.setCyLinder(extendedDev.isCyLinder);
        extendedBluetoothDevice.setDfuMode(extendedDev.isDfuMode);
        extendedBluetoothDevice.setGlassLock(extendedDev.isGlassLock);
        extendedBluetoothDevice.setLockcar(extendedDev.isLockcar);
        extendedBluetoothDevice.setNoLockService(extendedDev.isNoLockService);
        extendedBluetoothDevice.setPadLock(extendedDev.isPadLock);
        extendedBluetoothDevice.setRemoteControlDevice(extendedDev.isRemoteControlDevice);
        extendedBluetoothDevice.setRoomLock(extendedDev.isRoomLock);
        extendedBluetoothDevice.setSafeLock(extendedDev.isSafeLock);
        extendedBluetoothDevice.setTouch(extendedDev.isTouch);
//        extendedBluetoothDevice.isUnlock = extendedDev.isUnlock;
        extendedBluetoothDevice.setWristband(extendedDev.isWristband);
//        extendedBluetoothDevice.getLockType =  (extendedDev.lockType);
        extendedBluetoothDevice.orgId = extendedDev.orgId;
        extendedBluetoothDevice.setParkStatus(extendedDev.parkStatus);
        extendedBluetoothDevice.setProtocolType(extendedDev.protocolType);
        extendedBluetoothDevice.setProtocolVersion(extendedDev.protocolVersion);
        extendedBluetoothDevice.setRemoteUnlockSwitch(extendedDev.remoteUnlockSwitch);
        extendedBluetoothDevice.setScene(extendedDev.scene);
//        extendedBluetoothDevice.txPowerLevel = extendedDev.txPowerLevel;
        extendedBluetoothDevice.setBatteryCapacity(extendedDev.batteryCapacity);
        extendedBluetoothDevice.setDevice(device);
        extendedBluetoothDevice.setSettingMode(extendedDev.isSettingMode);
        extendedBluetoothDevice.setAddress(extendedDev.mAddress);
        extendedBluetoothDevice.setName(extendedDev.name);
        extendedBluetoothDevice.setRssi(extendedDev.rssi);
        extendedBluetoothDevice.setScanRecord(extendedDev.scanRecord);
        return extendedBluetoothDevice;
    }
}
