package com.example.usbhost

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.text.ParseException

private const val TAG = "UsbEnumerator"

class MainActivity : AppCompatActivity() {
    private lateinit var usbManager: UsbManager

    private lateinit var statusView: TextView
    private lateinit var resultView: TextView

    private var usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            device?.let {
                printStatus(getString(R.string.status_removed))
                printDeviceDescription(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusView = findViewById(R.id.text_status)
        resultView = findViewById(R.id.text_result)

        usbManager = getSystemService(UsbManager::class.java)

        val filter = IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        registerReceiver(usbReceiver,filter)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    private fun handleIntent(intent: Intent) {
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (device != null) {
            printStatus(getString(R.string.status_added))
            printDeviceDetails(device)
        } else {
            // List all devices connected to USB host on startup
            printStatus(getString(R.string.status_list))
            printDeviceList()
        }
    }

    private fun printDeviceList() {
        val connectedDevices = usbManager.deviceList

        if (connectedDevices.isEmpty()) {
            printResult("No Devices Currently Connected")
        } else {
            val builder = buildString {
                append("Connected Device Count: ")
                append(connectedDevices.size)
                append("\n\n")
                for (device in connectedDevices.values) {
                    //Use the last device detected (if multiple) to open
                    append(device.getDescription())
                    append("\n\n")
                }
            }

            printResult(builder)
        }
    }

    private fun printDeviceDescription(device: UsbDevice) {
        val result = device.getDescription() + "\n\n"
        printResult(result)
    }

    private fun printDeviceDetails(device: UsbDevice) {
        val connection = usbManager.openDevice(device)

        val deviceDescriptor = try {
            //Parse the raw device descriptor
            connection.readDeviceDescriptor()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid device descriptor", e)
            null
        }

        val configDescriptor = try {
            //Parse the raw configuration descriptor
            connection.readConfigurationDescriptor()
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Invalid config descriptor", e)
            null
        } catch (e: ParseException) {
            Log.w(TAG, "Unable to parse config descriptor", e)
            null
        }

        printResult("$deviceDescriptor\n\n$configDescriptor")
        connection.close()
    }

    private fun printStatus(status: String) {
        statusView.text = status
        Log.i(TAG, status)
    }

    private fun printResult(result: String) {
        resultView.text = result
        Log.i(TAG, result)
    }
}