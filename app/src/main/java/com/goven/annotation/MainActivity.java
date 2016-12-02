package com.goven.annotation;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.goven.api.AfterViews;
import com.goven.api.EActivity;
import com.goven.api.ViewById;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    @ViewById TextView tvAnnotation;
    @ViewById(R.id.tvOther) TextView textView;

    @AfterViews void initView() {

    }

}
