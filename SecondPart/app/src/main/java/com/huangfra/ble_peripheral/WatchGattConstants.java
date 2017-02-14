package com.huangfra.ble_peripheral;

import android.annotation.TargetApi;
import android.bluetooth.le.ScanFilter;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.Collections;
import java.util.List;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WatchGattConstants {
    public static final String WATCH_NAME = "ANCSClient";
    public static final UUID WATCH_CONTROL_SERVICE_UUID = UUID.fromString("7905F431-B5CE-4E99-A40F-4B1E122D00D0");
    public static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID WATCH_Notifycation_UUID = UUID.fromString("9FBF120D-6301-42D9-8C58-25E699A21DBD");
    public static final UUID WATCH_Control_point_UUID = UUID.fromString("69D1D8F3-45E1-49A8-9821-9BBDFDAAD9D9");
    public static final UUID WATCH_Data_Source_UUID = UUID.fromString("22EAC6E9-24D6-4BB5-BE44-B36ACE7C7BFB");

    //public static final UUID WATCH_OTHER_UUID = UUID.fromString("fe2ecd6e-09ca-42e5-80ca-03dfd303f0cd");
    private WatchGattConstants(){

    }

    public static final List<ScanFilter> Watch_FILTERS = Collections.singletonList(
            new ScanFilter.Builder().setServiceUuid(new ParcelUuid(WATCH_CONTROL_SERVICE_UUID)).build() );
}
