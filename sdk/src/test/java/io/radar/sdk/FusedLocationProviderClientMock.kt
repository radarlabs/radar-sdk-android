package io.radar.sdk

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource

internal class FusedLocationProviderClientMock(
    context: Context
) : FusedLocationProviderClient(context) {

    internal var mockLocation: Location? = null

    override fun getCurrentLocation(priority: Int, token: CancellationToken?): Task<Location> {
        val source = TaskCompletionSource<Location>()
        source.setResult(mockLocation)
        return source.task
    }

    override fun requestLocationUpdates(
        request: LocationRequest?,
        callback: LocationCallback?,
        looper: Looper?
    ): Task<Void> {
        if (mockLocation != null) {
            callback?.onLocationResult(LocationResult.create(listOf(mockLocation)))
        }
        return super.requestLocationUpdates(request, callback, looper)
    }

}