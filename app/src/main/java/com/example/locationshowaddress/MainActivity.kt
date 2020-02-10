package com.example.locationshowaddress

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

const val PERMISSION_REQUEST_COARSE_LOCATION = 0
class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                BROADCAST_ACTION_LOCATION_ADDRESS -> {
                    val addressResult: AddressResult? = intent.getParcelableExtra(LOCATION_DATA_EXTRA)
                    addressResult?.let{
                        if(it.success){
                            last_location_address.append("\n"+ it.data ?: getString(R.string.address_not_found))
                        } else{
                            last_location_address.append("\n"+ addressResult.data)
                        }
                    }

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(BROADCAST_ACTION_LOCATION_ADDRESS))

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        get_last_location_address.setOnClickListener {
            last_location_address.text = getString(R.string.fetching_location)
            getLastLocationAddress()
        }

    }

    private fun getLastLocationAddress(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            requestLastLocation()
        } else {
            requestLocationPermission()
        }
    }


    private fun requestLastLocation(){
        fusedLocationProviderClient.lastLocation
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    task.result?.let {
                        val pos = LatLong(it.latitude, it.longitude)
                        last_location_address.text = getString(R.string.location_info,
                            pos.latitude, pos.longitude)
                        last_location_address.append(getString(R.string.fetching_address))
                        fetchAddressFromLocation(pos)

                    }
                } else {
                    last_location_address.text = getString(R.string.no_location_found)
                }
            }
    }

    private fun fetchAddressFromLocation(pos: LatLong){
        val intent = Intent(this, LocationAddressIntentService::class.java).apply {
            putExtra(LOCATION_DATA_EXTRA, pos)
        }
        startService(intent)
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            val snack = Snackbar.make(container, R.string.location_permission_rationale,
                Snackbar.LENGTH_INDEFINITE)
            snack.setAction(getString(R.string.ok)) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION)
            }
            snack.show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_COARSE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLastLocation()
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied),
                    Toast.LENGTH_SHORT). show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(broadcastReceiver)
    }
}
