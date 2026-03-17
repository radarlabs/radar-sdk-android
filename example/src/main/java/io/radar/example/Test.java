package io.radar.example;

import android.content.Context;

import io.radar.sdk.Radar;
import io.radar.sdk.RadarInitializeOptions;

public class Test {

    public static void test(Context context) {
        RadarInitializeOptions options = RadarInitializeOptions.builder().authToken("kjflsd").build();
        Radar.initialize(context, options);
    }
}
