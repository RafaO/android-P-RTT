package com.rortega.androidprtt

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    fun <T> MutableList<T>.takeMax(max: Int) = this.subList(0, minOf(size, max))

    companion object {
        const val REQUEST_CODE_ACCESS_COARSE_LOCATION = 1
    }

    private val mWifiManager by lazy { applicationContext.getSystemService(WifiManager::class.java) as WifiManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rttManager = getSystemService(WifiRttManager::class.java) as WifiRttManager

        button_scan.setOnClickListener {
            showLoading()
            textview_results.text = ""
            mWifiManager.startScan()
        }

        // REQUEST PERMISSION
        when {
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED -> // permission is not granted
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_CODE_ACCESS_COARSE_LOCATION)
            rttManager.isAvailable -> showButton()
            else -> showErrorView()
        }

        // REGISTER BROADCAST RECEIVER
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                with(mWifiManager.scanResults) {
                    if (size > 0) {
                        val rangingRequest = RangingRequest.Builder()
                                .addAccessPoints(takeMax(RangingRequest.getMaxPeers()))
                                .build()

                        rttManager.startRanging(rangingRequest, object : RangingResultCallback() {
                            override fun onRangingResults(results: MutableList<RangingResult>) {
                                showButton()
                                results
                                        .filter { it.status == RangingResult.STATUS_SUCCESS }
                                        .forEach {
                                            textview_results.append("\nAccess point ${it.macAddress} is at ${it.distanceMm.div(1000)} +/- ${it.distanceStdDevMm.div(1000)} meters.")
                                        }
                            }

                            override fun onRangingFailure(p0: Int) {
                                textview_results.text = "error in ranging request, code: ${p0}"
                                showButton()
                            }
                        }, null)
                    }
                }
            }

        }, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_ACCESS_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    showButton()
                } else {
                    // permission denied, boo!
                    showErrorView("we need your permission")
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun showErrorView(errorMessage: String? = null) {
        textview_error.visibility = View.VISIBLE
        errorMessage?.let { textview_error.text = it }
        button_scan.visibility = View.GONE
        progressbar.visibility = View.GONE
    }

    private fun showButton() {
        textview_error.visibility = View.GONE
        button_scan.visibility = View.VISIBLE
        progressbar.visibility = View.GONE
    }

    private fun showLoading() {
        textview_error.visibility = View.GONE
        button_scan.visibility = View.GONE
        progressbar.visibility = View.VISIBLE
    }
}
