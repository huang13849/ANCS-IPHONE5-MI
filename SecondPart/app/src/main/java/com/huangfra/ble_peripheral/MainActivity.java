package com.huangfra.ble_peripheral;

import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothGatt;

import android.bluetooth.BluetoothGattCallback;

import android.bluetooth.BluetoothGattCharacteristic;

import android.bluetooth.BluetoothGattDescriptor;

import android.bluetooth.BluetoothGattServer;

import android.bluetooth.BluetoothGattServerCallback;

import android.bluetooth.BluetoothGattService;

import android.bluetooth.BluetoothManager;

import android.bluetooth.BluetoothProfile;

import android.bluetooth.le.AdvertiseCallback;

import android.bluetooth.le.AdvertiseData;

import android.bluetooth.le.AdvertiseSettings;

import android.bluetooth.le.BluetoothLeAdvertiser;

import android.content.BroadcastReceiver;

import android.content.Context;

import android.content.Intent;

import android.content.IntentFilter;

import android.content.pm.PackageManager;

import android.os.Bundle;

import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;

import android.support.v7.app.AppCompatActivity;

import android.text.method.ScrollingMovementMethod;

import android.util.Log;

import android.widget.TextView;

import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.List;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final int BleDisconnect = 0;// this is same to onConnectionStateChange()'s state
    public static final int BleAncsConnected = 10;// connected to iOS's ANCS
    public static final int BleBuildStart = 1;// after connectGatt(), before onConnectionStateChange()
    public static final int BleBuildConnectedGatt = 2; // onConnectionStateChange() state==2
    public static final int BleBuildDiscoverService = 3;// discoverServices()... this block
    public static final int BleBuildDiscoverOver = 4; // discoverServices() ok
    public static final int BleBuildDiscovered = 5; // discoverServices() BleBuildDiscovered callback
    public static final int BleBuildSetingANCS = 6; // settingANCS eg. need pwd...
    public static final int BleBuildNotify = 7; // notify arrive

    private static final String TAG = "Main";

    private static final String BLE_SERVER = "huangWatch";

    private static final String WATCH_UUID = "0000a7d2-0000-1000-8000-00805f9b34fb";

    private static final String WATCH_UUID_ORI = "65432461-1EFE-4ADB-BC7E-9F7F8E27FDC1";

    private static final int SCREEN_MESSAGE = 1;

    public static final int NOTIFY_MSG = 0;

    public static final int DATASOURCE_MSG = 1;

    private BluetoothManager mBluetoothManager;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothWatchANCSGattService ANCSService;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private AdvertiseCallback mAdvertiseCallback;

    private BluetoothGattServerCallback mBluetoothGattServerCallback;

    BluetoothGattServer gattServer;

    BluetoothDevice mDevice;

    BluetoothGattCharacteristic mCharacteristic;

    private static final String mSERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";

    private static final String mCHARACTERISTIC_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    private TextView textView;

    private BluetoothGattService serviceANCS;

    private BluetoothGattCharacteristic controlPoinCharacteristic;

    private BluetoothGattCharacteristic dataSourceCharacteristic;

    private BluetoothGattCharacteristic notifycationCharacteristic;

    boolean isWritable, isWriteNS_DespOk;

    private ANCSParser mANCSHandler;

    public int mBleState;

    private Context mContext;

    public final static int NotificationAttributeIDTitle = 1;

    public final static int NotificationAttributeIDSubtitle = 2;

    public final static int NotificationAttributeIDMessage = 3;

    public final static int NotificationAttributeIDMessageSize = 4;

    public final static int NotificationAttributeIDDate = 5;

    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);

        textView.setMovementMethod(new ScrollingMovementMethod());

        mANCSHandler = ANCSParser.getDefault(this);

        mContext = this.getApplicationContext();

        initBluetooth();

        startNewAdvert();

        /*startOldAdvert();*/

    }


    private boolean initBluetooth() {

        this.ANCSService = new BluetoothWatchANCSGattService();

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "mBluetoothManager is null.");
            return false;
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "mBluetoothAdapter is null.");
            return false;
        }

        mBluetoothAdapter.setName(BLE_SERVER);

        this.mBluetoothLeAdvertiser = this.mBluetoothAdapter.getBluetoothLeAdvertiser();

        return true;
    }


    private void startNewAdvert() {

        textView.append("startNewAdvert ... " + "\n");

        if (mBluetoothAdapter == null ||
                !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE support.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (this.mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, "No Bluetooth Le Advertiser support.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!this.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "BLE Multiple Advertisement Not Supported.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean bRet = startAdvertising();
        if (!bRet) {
            textView.append(" startAdvertising fail.");
            return;
        }

        initBlueToothGattServerCallback();

        startAdvertisingService();
    }

    private boolean startAdvertising() {

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        registerReceiver(this.mReceiver, filter);

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        AdvertiseSettings.Builder advertSetting = new AdvertiseSettings.Builder();


        if (this.mAdvertiseCallback != null) {
            textView.append("BLE advertising already ongoing");
            return true;
        }

        this.mBluetoothAdapter.setName(BLE_SERVER);

        advertSetting.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);

        advertSetting.setTimeout(0);
        //advertSetting.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);

        AdvertiseSettings localAdvertiseSettings = advertSetting.build();

        advertSetting.setConnectable(true);

        dataBuilder.setIncludeDeviceName(true);

        dataBuilder.setIncludeTxPowerLevel(true);

        dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(WATCH_UUID)));

        mAdvertiseCallback = new AdvertiseCallback() {

            @Override

            public void onStartSuccess(AdvertiseSettings settingsInEffect) {

                super.onStartSuccess(settingsInEffect);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("startAdvertising() Advertise 成功!!" + "\n");
                    }
                });
            }


            @Override

            public void onStartFailure(final int errorCode) {

                super.onStartFailure(errorCode);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("startAdvertising() Advertise 失敗 ... " + errorCode + "\n");
                    }
                });

            }

        };

        if (this.mAdvertiseCallback == null) {
            Log.e(TAG, "Unable to alloc advertise callback");
            return false;
        }

        try {

            this.mBluetoothLeAdvertiser.startAdvertising(localAdvertiseSettings, dataBuilder.build(), this.mAdvertiseCallback);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context paramAnonymousContext, Intent paramAnonymousIntent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(paramAnonymousIntent.getAction())) {
                int state = paramAnonymousIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                textView.append(TAG + "onReceive state =" + state);
                if ((state == BluetoothAdapter.STATE_OFF) || (state == BluetoothAdapter.STATE_TURNING_OFF)) {
                    textView.append(TAG + " ERROR!! onReceive Bluetooth disabled during advertising, stop");
                }
            }
        }
    };

    private void startAdvertisingService() {

        try {
            gattServer = this.mBluetoothManager.openGattServer(this.getApplicationContext(), mBluetoothGattServerCallback);
            if (gattServer == null) {
                Log.e(TAG, "Unable to open Gatt Server");
                return;
            }

        } catch (Exception e) {
            gattServer = null;
            e.printStackTrace();
            return;
        }

        setGattServerService();


    }

    private void initBlueToothGattServerCallback() {

        mBluetoothGattServerCallback = new BluetoothGattServerCallback() {

            @Override

            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, final byte[] value) {

                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边]onCharacteristicWriteRequest value ~ " + new String(value));
                    }
                });

                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);

            }


            @Override

            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边] onCharacteristicReadRequest");
                    }
                });

                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "ABC".getBytes());

            }


            @Override

            public void onConnectionStateChange(final BluetoothDevice device, int status, int newState) {

                super.onConnectionStateChange(device, status, newState);

                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    mDevice = device;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("[周边]有设备连入... " + device.getName() + " " + device.getAddress() + "\n");
                        }
                    });

                    device.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                    mDevice = null;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("[周边] onConnectionStateChange STATE_DISCONNECTED" + "\n");
                        }
                    });

                }

            }

            @Override
            public void onNotificationSent(BluetoothDevice device, final int status) {
                super.onNotificationSent(device, status);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边] onNotificationSent " + status);
                    }
                });

            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边] onDescriptorReadRequest ");
                    }
                });
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边] onDescriptorWriteRequest ");
                    }
                });
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                super.onExecuteWrite(device, requestId, execute);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[周边] onExecuteWrite ");
                    }
                });

            }
        };
    }

    public void setGattServerService() {


        BluetoothGattService service = new BluetoothGattService(UUID.fromString(mSERVICE_UUID), BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(mCHARACTERISTIC_UUID),

                BluetoothGattCharacteristic.PROPERTY_NOTIFY |

                        BluetoothGattCharacteristic.PROPERTY_READ |

                        BluetoothGattCharacteristic.PROPERTY_WRITE,

                BluetoothGattCharacteristic.PERMISSION_READ |

                        BluetoothGattCharacteristic.PERMISSION_WRITE);


        service.addCharacteristic(mCharacteristic);

        gattServer.addService(this.ANCSService);

    }

    private void startOldAdvert() {

        textView.append("startOldAdvert...");

        AdvertiseSettings.Builder settingBuilder = new AdvertiseSettings.Builder();

        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);

        settingBuilder.setConnectable(false);

        settingBuilder.setTimeout(0);

        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW);

        AdvertiseSettings settings = settingBuilder.build();


        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(WATCH_UUID_ORI)));

        AdvertiseData advertiseData = dataBuilder.build();

        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, mAdvertiseCallback);
    }


    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[中心] 周边设备已连接" + "\n");
                    }
                });

                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.append("[中心] 周边设备连接断开" + "\n");
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, final int status) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String statusStr = (status == 0 ? "寻找服务" : "未发现服务");
                    textView.append("[中心]onServicesDiscovered " + statusStr + "\n");
                }
            });

            if (status == BluetoothGatt.GATT_SUCCESS) {

                List<BluetoothGattService> services = gatt.getServices();

       /*         for (final BluetoothGattService serv : services) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("[中心]UUID = " + serv.getUuid().toString() + "\n");
                        }
                    });
                }
      */

                serviceANCS = gatt.getService(WatchGattConstants.WATCH_CONTROL_SERVICE_UUID);

                isWriteNS_DespOk = isWritable = false;

                mANCSHandler.setService(serviceANCS, gatt);

                if (serviceANCS != null) {

                    notifycationCharacteristic = serviceANCS.getCharacteristic(WatchGattConstants.WATCH_Notifycation_UUID);
                    controlPoinCharacteristic = serviceANCS.getCharacteristic(WatchGattConstants.WATCH_Control_point_UUID);
                    dataSourceCharacteristic = serviceANCS.getCharacteristic(WatchGattConstants.WATCH_Data_Source_UUID);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.append("[中心] 发现ANCS service 和 characteristics" + "\n");
                        }
                    });

                    gatt.setCharacteristicNotification(notifycationCharacteristic, true);
                    gatt.setCharacteristicNotification(controlPoinCharacteristic, true);
                    gatt.setCharacteristicNotification(dataSourceCharacteristic, true);

                    for (BluetoothGattDescriptor dp : notifycationCharacteristic.getDescriptors()) {
                        dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(dp);
                    }

                    for (BluetoothGattDescriptor dp : controlPoinCharacteristic.getDescriptors()) {
                        dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(dp);
                    }
                    for (BluetoothGattDescriptor dp : dataSourceCharacteristic.getDescriptors()) {
                        dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(dp);
                    }

                    gatt.readCharacteristic(notifycationCharacteristic);
                    gatt.readCharacteristic(controlPoinCharacteristic);
                    gatt.readCharacteristic(dataSourceCharacteristic);

                }

            }

        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("[中心]onDescriptorRead " + "\n");
                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            Log.d(TAG, "測試::onDescriptorWrite,status[" + status + "]");
            Log.i(TAG, "onDescriptorWrite" + "status:" + status);

            if(serviceANCS == null) {
                serviceANCS = gatt.getService(WatchGattConstants.WATCH_CONTROL_SERVICE_UUID);
            }else if (!isWritable) { // set NS
                isWritable = true;
                BluetoothGattCharacteristic bluetoothGattCharacteristic = serviceANCS.getCharacteristic(WatchGattConstants.WATCH_Notifycation_UUID);
                if (bluetoothGattCharacteristic == null) {
                    Log.i(TAG, "can not find ANCS's NS cha");
                    return;
                } else {
                }
                boolean registerNS = gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                if (!registerNS) {
                    Log.i(TAG, " Enable (NS) notifications failed  ");
                    return;
                }
                BluetoothGattDescriptor desp = bluetoothGattCharacteristic.getDescriptor(WatchGattConstants.DESCRIPTOR_UUID);
                if (null != desp) {
                    boolean r = desp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    boolean rr = gatt.writeDescriptor(desp);
                    isWriteNS_DespOk = rr;
                    Log.i(TAG, "(NS)Descriptor.setValue(): " + r + ",writeDescriptor(): " + rr);
                } else {
                    Log.i(TAG, "null descriptor");
                }
            }else {
                Log.d(TAG, "onDescriptorWrite: except return:  isWritable is " + isWritable );
            }
        }



        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("[中心]onCharacteristicChanged " + "\n");
                }
            });

            final UUID uuid = characteristic.getUuid();

            byte[] data = characteristic.getValue();

            Message msg = new Message();

            msg.obj = data;

            if (uuid.equals(WatchGattConstants.WATCH_Notifycation_UUID)) {

                msg.what = NOTIFY_MSG;

                Log.i(TAG, "Notify uuid");

                /*mANCSHandler.onNotification(data);*/

            } else if (uuid.equals(WatchGattConstants.WATCH_Data_Source_UUID)) {

                msg.what = DATASOURCE_MSG;

                Log.i(TAG, "datasource uuid");

                /*mANCSHandler.onDSNotification(data);*/

            } else {

            }


            mHandler.sendMessage(msg);

        }

       @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("[中心]onCharacteristicRead ");
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.append("[中心]onCharacteristicWrite ");
                }
            });

        }
    };

    private void writeSpecificCharacter(BluetoothGatt gatt) {

        isWritable = true;

        BluetoothGattCharacteristic bluetoothGattCharacteristic = serviceANCS.getCharacteristic(WatchGattConstants.WATCH_Notifycation_UUID);
        if (bluetoothGattCharacteristic == null) {
            Log.i(TAG, "can not find ANCS's NS cha");
            return;
        } else {
        }
        boolean registerNS = gatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
        if (!registerNS) {
            Log.i(TAG, " Enable (NS) notifications failed  ");
            return;
        }
        BluetoothGattDescriptor desp = bluetoothGattCharacteristic.getDescriptor(WatchGattConstants.DESCRIPTOR_UUID);
        if (null != desp) {
            boolean r = desp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean rr = gatt.writeDescriptor(desp);
            Log.i(TAG, "(NS)Descriptor.setValue(): " + r + ",writeDescriptor(): " + rr);
        } else {
            Log.i(TAG, "null descriptor");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);
        serviceANCS = null;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOTIFY_MSG:
                    byte[] notifyData = (byte[]) msg.obj;

                    int eventId = notifyData[0];
                    String EVENTID = new String();
                    switch (eventId) {
                        case 0:
                            EVENTID = "增加; ";
                            break;
                        case 1:
                            EVENTID = "删除; ";
                            break;
                        case 2:
                            EVENTID = "修改; ";
                            break;
                        default:
                            break;
                    }

                    int eventFlags = notifyData[1];
                    String EVENTFLAGS = eventFlags == 0 ? "重要; " : "静默; ";

                    int categoryID = notifyData[2];
                    String CATEGORYID = new String();
                    switch (categoryID) {
                        case 0:
                            CATEGORYID = "other";
                            break;
                        case 1:
                            CATEGORYID = "incomming phone";
                            break;
                        case 2:
                            CATEGORYID = "missing call";
                            break;
                        case 4:
                            CATEGORYID = "social";
                            break;
                        case 7:
                            CATEGORYID = "news";
                            break;
                        default:
                            CATEGORYID = String.valueOf(categoryID);
                            break;
                    }

                    int categoryCount = notifyData[3];
                    int notificationUID = ((0xff & notifyData[4] | (0xff & notifyData[5] << 8) | (0xff & notifyData[6] << 16) | 0xff & notifyData[7] << 24));

                    Log.d(TAG, "handleMessage: Paser Response Control Point" + " EVENTID:" + EVENTID + " EVENTFLAGS:" + EVENTFLAGS + " CATEGORYID:" + CATEGORYID + " categoryCount:" + categoryCount + " notificationUID:" + notifyData[4]+notifyData[5]+notifyData[6]+notifyData[7]);

                    ByteArrayOutputStream bout = new ByteArrayOutputStream();

                    //获取通知属性命令
                    bout.write((byte) 0);
                    bout.write(notifyData[4]);
                    bout.write(notifyData[5]);
                    bout.write(notifyData[6]);
                    bout.write(notifyData[7]);

                    bout.write(NotificationAttributeIDTitle);
                    bout.write(50);
                    bout.write(0);
                    // subtitle
                    bout.write(NotificationAttributeIDSubtitle);
                    bout.write(100);
                    bout.write(0);

                    // message
                    bout.write(NotificationAttributeIDMessage);
                    bout.write(500);
                    bout.write(0);

                    // message size
                    bout.write(NotificationAttributeIDMessageSize);
                    bout.write(10);
                    bout.write(0);

                    // date
                    bout.write(NotificationAttributeIDDate);
                    bout.write(10);
                    bout.write(0);

                    byte[] control_byte = bout.toByteArray();

                    if(controlPoinCharacteristic != null && controlPoinCharacteristic.setValue(control_byte)){
                        Log.i(TAG, "write control point success!!");
                    }else{
                        Log.i(TAG, "write control point failed!!");
                    }

                    break;

                case DATASOURCE_MSG:
                    byte[] dataSourceData = (byte[]) msg.obj;
                    String result = new String ();
                    int commandIdMustBeOne = dataSourceData[0];

                    int curIdx = 5; // hard code

                    while(curIdx<100) {
                        if (dataSourceData.length < curIdx + 3) {
                            return;
                        }
                        // attributes head
                        int attrId = dataSourceData[curIdx];
                        int attrLen = ((dataSourceData[curIdx + 1]) & 0xFF) | (0xFF & (dataSourceData[curIdx + 2] << 8));
                        curIdx += 3;
                        if (dataSourceData.length < curIdx + attrLen) {
                            return;
                        }
                        String val = new String(dataSourceData, curIdx, attrLen);// utf-8 encode
                        if (attrId == NotificationAttributeIDTitle) {
                            Log.i(TAG, "noti.title:" + val + "\n");
                            result = "noti.title:" + val + "\n";
                        } else if (attrId == NotificationAttributeIDMessage) {
                            Log.i(TAG, "noti.message:" + val + "\n");
                            result = "noti.message:" + val + "\n";
                        } else if (attrId == NotificationAttributeIDDate) {
                            Log.i(TAG, "noti.date:" + val + "\n");
                            result = "noti.date:" + val + "\n";
                        } else if (attrId == NotificationAttributeIDSubtitle) {
                            Log.i(TAG, "noti.subtitle:" + val + "\n");
                            result = "noti.subtitle:" + val + "\n";
                        } else if (attrId == NotificationAttributeIDMessageSize) {
                            Log.i(TAG, "noti.messageSize:" + val + "\n");
                            result = "noti.messageSize:" + val + "\n";
                        }
                        curIdx += attrLen;
                    }
                    Log.i(TAG, "got a notification! data size = " + dataSourceData.length + "\n");

                    textView.append("message arrive : " + result);

                    break;
                default:
                    break;
            }

        }
    };


}
