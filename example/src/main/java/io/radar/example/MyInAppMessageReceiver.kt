package io.radar.example

import io.radar.example.store.ConsoleKind
import io.radar.example.store.LogStore
import io.radar.sdk.Radar
import io.radar.sdk.RadarInAppMessageReceiver
import io.radar.sdk.model.RadarInAppMessage

/** Routes in-app message callbacks into the unified [LogStore] console. */
class MyInAppMessageReceiver(private val logStore: LogStore) : RadarInAppMessageReceiver {

    override fun onNewInAppMessage(inAppMessage: RadarInAppMessage) {
        logStore.write(ConsoleKind.EVENT, "in-app message: ${inAppMessage.title.text}")
        Radar.showInAppMessage(inAppMessage)
    }

    override fun onInAppMessageDismissed(inAppMessage: RadarInAppMessage) {
        logStore.write(ConsoleKind.LOG, "in-app message dismissed: ${inAppMessage.title.text}")
    }

    override fun onInAppMessageButtonClicked(inAppMessage: RadarInAppMessage) {
        logStore.write(ConsoleKind.LOG, "in-app message button clicked: ${inAppMessage.title.text}")
    }
}
