package me.dibber.blablablapp.ext;

import me.dibber.blablablapp.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {
		
    private int mMin, mMax, mDefault;
    private NumberPicker mNumberPicker;

	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
        mMax = 500;
        mMin = 0;
        mDefault = 100;
	}
	
	@SuppressLint("InflateParams")
	@Override
    protected View onCreateDialogView() {
        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.number_picker_dialog, null);
        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        

        // Initialize state
        mNumberPicker.setMaxValue(mMax);
        mNumberPicker.setMinValue(mMin);
        mNumberPicker.setValue(getPersistedInt(mDefault));
        mNumberPicker.setWrapSelectorWheel(false);
        
        return view;
	}
	
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            persistInt(mNumberPicker.getValue());
        }
    }
}
