package io.radar.example

import io.radar.sdk.Radar
import io.radar.sdk.model.RadarEvent

class Utils {

    companion object {

        fun stringForRadarStatus(status: Radar.RadarStatus): String {
            return when (status) {
                Radar.RadarStatus.SUCCESS -> "Success"
                Radar.RadarStatus.ERROR_PUBLISHABLE_KEY -> "Publishable key error"
                Radar.RadarStatus.ERROR_PERMISSIONS -> "Permissions error"
                Radar.RadarStatus.ERROR_LOCATION -> "Location error"
                Radar.RadarStatus.ERROR_NETWORK -> "Network error"
                Radar.RadarStatus.ERROR_UNAUTHORIZED -> "Unauthorized error"
                Radar.RadarStatus.ERROR_RATE_LIMIT -> "Rate limit error"
                Radar.RadarStatus.ERROR_SERVER -> "Server error"
                else -> "Unknown error"
            }
        }

        fun stringForRadarEvent(event: RadarEvent): String {
            val confidenceStr = stringForRadarEventConfidence(event.confidence)

            return when (event.type) {
                RadarEvent.RadarEventType.USER_ENTERED_GEOFENCE -> "Entered geofence ${event.geofence?.description} with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_GEOFENCE -> "Exited geofence ${event.geofence?.description} with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_HOME -> "Entered home with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_HOME -> "Exited home with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_OFFICE -> "Entered office with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_OFFICE -> "Exited office with $confidenceStr"
                RadarEvent.RadarEventType.USER_STARTED_TRAVELING -> "Started traveling with $confidenceStr"
                RadarEvent.RadarEventType.USER_STOPPED_TRAVELING -> "Stopped traveling with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_PLACE -> "Entered place ${event.place?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_PLACE -> "Exited place ${event.place?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_NEARBY_PLACE_CHAIN -> "Nearby chain ${event.place?.chain?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_REGION_COUNTRY -> "Entered country ${event.region?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_REGION_COUNTRY -> "Exited country ${event.region?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_REGION_STATE -> "Entered state ${event.region?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_REGION_STATE -> "Exited state ${event.region?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_ENTERED_REGION_DMA -> "Entered DMA ${event.region?.name} with $confidenceStr"
                RadarEvent.RadarEventType.USER_EXITED_REGION_DMA -> "Exited DMA ${event.region?.name} with $confidenceStr"
                else -> "Unknown"
            }
        }

        fun stringForRadarEventConfidence(confidence: RadarEvent.RadarEventConfidence): String {
            return when (confidence) {
                RadarEvent.RadarEventConfidence.LOW -> "low confidence"
                RadarEvent.RadarEventConfidence.MEDIUM -> "medium confidence"
                RadarEvent.RadarEventConfidence.HIGH -> "high confidence"
                else -> "unknown confidence"
            }
        }

        fun stringForRadarLocationSource(source: Radar.RadarLocationSource): String {
            return when (source) {
                Radar.RadarLocationSource.FOREGROUND_LOCATION -> "foreground location"
                Radar.RadarLocationSource.BACKGROUND_LOCATION -> "background location"
                Radar.RadarLocationSource.MANUAL_LOCATION -> "manual location"
                Radar.RadarLocationSource.GEOFENCE_ENTER -> "geofence enter"
                Radar.RadarLocationSource.GEOFENCE_DWELL -> "geofence dwell"
                Radar.RadarLocationSource.GEOFENCE_EXIT -> "geofence exit"
                else -> "unknown"
            }
        }

    }

}