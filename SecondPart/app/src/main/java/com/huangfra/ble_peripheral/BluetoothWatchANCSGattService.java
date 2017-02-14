package com.huangfra.ble_peripheral;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * Created by richard on 16-9-29.
 */
public class BluetoothWatchANCSGattService extends BluetoothGattService {
    private static final UUID WatchControlServiceUUID = UUID.fromString("7905F431-B5CE-4E99-A40F-4B1E122D00D0");

    private static final UUID PhoneNotifyReportUUID = UUID.fromString("9FBF120D-6301-42D9-8C58-25E699A21DBD");
    private static final UUID ControlPointUUID = UUID.fromString("69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9");
    private static final UUID DataSourceUUID = UUID.fromString("22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB");

    public BluetoothGattCharacteristic mPhoneNotifyReport = new BluetoothGattCharacteristic(PhoneNotifyReportUUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ| BluetoothGattCharacteristic.PERMISSION_WRITE);
    public BluetoothGattCharacteristic ControlPoin = new BluetoothGattCharacteristic(ControlPointUUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ| BluetoothGattCharacteristic.PERMISSION_WRITE);
    public BluetoothGattCharacteristic DataSource = new BluetoothGattCharacteristic(DataSourceUUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE| BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ| BluetoothGattCharacteristic.PERMISSION_WRITE);

    public BluetoothWatchANCSGattService() {
        super(WatchControlServiceUUID, SERVICE_TYPE_PRIMARY);

        createCharacter();
    }

    private void createCharacter(){

//        mPhoneNotifyReport.setValue(new byte[] { 0x00,0x43,0x00,0x00,0x00,0x00,0x01, (byte) 0xFF, (byte) 0xFF,0x05  });
        mPhoneNotifyReport.setValue(new byte[] { 0x00,0x01,0x00,0x01,0x43,0x00,0x00,0x00  });
        addCharacteristic(mPhoneNotifyReport);
        addCharacteristic(ControlPoin);
        addCharacteristic(DataSource);
    }
    public void senddata(byte bytes[])
    {
        mPhoneNotifyReport.setValue(bytes);
        addCharacteristic(mPhoneNotifyReport);
    }
}
