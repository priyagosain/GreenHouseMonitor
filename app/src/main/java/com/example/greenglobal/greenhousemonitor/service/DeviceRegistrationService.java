package com.example.greenglobal.greenhousemonitor.service;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.example.greenglobal.greenhousemonitor.R;
//import com.example.greenglobal.greenhousemonitor.activity.ReadingsActivity_;
import com.example.greenglobal.greenhousemonitor.model.HexiwearDevice;
import com.example.greenglobal.greenhousemonitor.util.HexiwearDevices;
//import com.example.greenglobal.greenhousemonitor.view.DeviceActivation;
//import com.example.greenglobal.greenhousemonitor.view.DeviceActivation_;
import com.wolkabout.wolkrestandroid.Credentials_;
import com.wolkabout.wolkrestandroid.dto.CreatePointBodyDTO;
import com.wolkabout.wolkrestandroid.dto.CreatedPointDto;
import com.wolkabout.wolkrestandroid.dto.PointWithFeedsResponse;
import com.wolkabout.wolkrestandroid.dto.SerialDto;
import com.wolkabout.wolkrestandroid.enumeration.SensorType;
import com.wolkabout.wolkrestandroid.service.DeviceService;
import com.wolkabout.wolkrestandroid.service.PointService;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.rest.spring.annotations.RestService;

import java.util.ArrayList;
import java.util.List;

@EBean
public class DeviceRegistrationService {

    private static final String TAG = DeviceRegistrationService.class.getSimpleName();

    @RootContext
    Context context;

    @Pref
    Credentials_ credentials;

    @Bean
    HexiwearDevices devicesStore;

    @RestService
    DeviceService deviceService;

    @RestService
    PointService pointService;

    @Background
    public void registerHexiwearDevice(final BluetoothDevice device) {
        if (credentials.username().get().equals("Demo")) {
            //final int demoNumber = devicesStore.getDevices().size() + 1;
            final int demoNumber = 0;
            //final HexiwearDevice hexiwearDevice = new HexiwearDevice(device.getName(), "", device.getAddress(), "", "Demo device " + demoNumber);
            final HexiwearDevice hexiwearDevice = new HexiwearDevice("Device", "", device.getAddress(), "", "Demo device " + demoNumber);
            devicesStore.storeDevice(hexiwearDevice);
            //ReadingsActivity_.intent(context).device(device).start();
        }

        final SerialDto serialDto = deviceService.getRandomSerial(SensorType.HEXIWEAR);
        final String serial = serialDto.getSerial();
        getDevices(device, serial);
    }

    @Background
    void getDevices(final BluetoothDevice device, final String serial) {
        final List<PointWithFeedsResponse> points = pointService.getPoints();
        final List<HexiwearDevice> hexiwearDevices = HexiwearDevices.getDevices(points);
        displayActivationDialog(device, serial, hexiwearDevices);
    }

    @UiThread
    void displayActivationDialog(final BluetoothDevice device, final String serial, final List<HexiwearDevice> hexiwearDevices) {
        //final DeviceActivation view = DeviceActivation_.build(context);
        //view.setDevices(hexiwearDevices);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        /*]
        builder.setView(view);
        builder.setMessage(R.string.activation_dialog_title);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //dialog.cancel();
            }
        });
        builder.setPositiveButton(R.string.activation_dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String wolkName = view.getWolkName();
                if (view.isNewDeviceSelected()) {
                    Log.d(TAG, "New device selected. Activating...");
                    registerHexiwearDevice(device, serial, wolkName);
                } else {
                    deactivateDeviceAndRegister(device, serial, wolkName, view.getDeviceSerial());
                }
            }
        });
         */
        builder.show();
    }

    @Background
    void deactivateDeviceAndRegister(final BluetoothDevice device, final String serial, final String wolkName, String existingDeviceSerial) {
        try {
            Log.d(TAG, "Deactivating existing device...");
            deviceService.deactivateDevice(existingDeviceSerial);
            Log.d(TAG, "Deactivated.");
            registerHexiwearDevice(device, serial, wolkName);
        } catch (Exception e) {
            onDeactivateError();
        }
    }

    @UiThread
    void onDeactivateError() {
        Toast.makeText(context, "Failed to activate as exiting device", Toast.LENGTH_LONG).show();
    }

    @Background
    void registerHexiwearDevice(final BluetoothDevice device, final String serial, final String wolkName) {
        Log.d(TAG, "Registering hexiwear device.");
        final CreatePointBodyDTO bodyDto = new CreatePointBodyDTO(wolkName, "", "T:ON,P:ON,H:ON");
        final ArrayList<CreatePointBodyDTO> bodyDtos = new ArrayList<>();
        bodyDtos.add(bodyDto);
        final List<CreatedPointDto> response = deviceService.createPointWithThings(serial, bodyDtos);
        if (!response.isEmpty()) {
            final String name = "Device";
            //final String name = device.getName();
            final String address = device.getAddress();
            final String password = response.get(0).getPassword();
            final HexiwearDevice hexiwearDevice = new HexiwearDevice(name, serial, address, password, wolkName);
            devicesStore.storeDevice(hexiwearDevice);
            Log.d(TAG, "Hexiwear registered." + hexiwearDevice);

            //ReadingsActivity_.intent(context).device(device).start();
        } else {
            onRegistrationError();
        }
    }

    @UiThread
    void onRegistrationError() {
        Toast.makeText(context, "Failed to activate device.", Toast.LENGTH_LONG).show();
        Log.e(TAG, "Registration failed");
    }
}