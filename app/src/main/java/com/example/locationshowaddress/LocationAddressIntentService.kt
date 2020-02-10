package com.example.locationshowaddress

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.IOException
import java.lang.Exception
import java.util.*


const val LOCATION_DATA_EXTRA ="LOCATION_DATA_EXTRA"
const val BROADCAST_ACTION_LOCATION_ADDRESS = "BROADCAST_ACTION_LOCATION_ADDRESS"

class LocationAddressIntentService: IntentService("LocationAddressIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        val latLong: LatLong? = intent.getParcelableExtra(
            LOCATION_DATA_EXTRA) ?: return

        latLong ?: return

        val addressResult = AddressResult()

        val geocoder = Geocoder(this, Locale.getDefault())
        var locationAddresses: List<Address> = emptyList()

        try {
            locationAddresses = geocoder.getFromLocation(
                latLong.latitude,
                latLong.longitude,
                1)
        } catch (ioException: IOException) {
            addressResult.data = "Error occurred while fetching the address. Please try again."
        } catch (exception: Exception) {
            addressResult.data = "Unknown error: $exception"
        }

        if (locationAddresses.isNotEmpty() ) {
            val address = locationAddresses[0]

            val addressTokens = with(address) {
                (0..maxAddressLineIndex).map { getAddressLine(it) }
            }
            addressResult.data = addressTokens.joinToString (separator = "\n")
            addressResult.success = true
        }

        broadcastStatus(addressResult)
    }

    private fun broadcastStatus(addressResult: AddressResult){
        Intent().also { intent ->
            intent.action = BROADCAST_ACTION_LOCATION_ADDRESS
            intent.putExtra(LOCATION_DATA_EXTRA, addressResult)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }
}