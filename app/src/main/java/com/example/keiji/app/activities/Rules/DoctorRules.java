package com.example.keiji.app.activities.Rules;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.keiji.app.activities.R;

public class DoctorRules extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_doctor_rules, container, false);
        TextView rulesText = result.findViewById(R.id.doctor_view);
        rulesText.setMovementMethod(new ScrollingMovementMethod());
        return result;
    }
}