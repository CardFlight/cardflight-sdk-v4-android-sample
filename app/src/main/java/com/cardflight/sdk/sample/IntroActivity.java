package com.cardflight.sdk.sample;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.Nullable;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

/**
 * Created by radhikadayal on 11/6/17.
 */

public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.background_color)
                .buttonsColor(R.color.button_color)
                .neededPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET})
                .image(R.drawable.permissions_icon)
                .title("Need Required Permissions")
                .description("You must enable these permissions to use the app without any issues.")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.background_color)
                .buttonsColor(R.color.button_color)
                .image(R.drawable.cf_logo_light)
                .title("Hooray! You're ready to use the CardFlight SDK sample app!")
                .build());
    }
}
