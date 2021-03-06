package com.echdr.android.echdrapp.ui.enrollment_form;

import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.service.forms.FormField;

import org.hisp.dhis.android.core.common.ValueType;

class BooleanFieldHolder extends FieldHolder {

    private final RadioGroup radioGroup;
    private FormField fieldItem;

    BooleanFieldHolder(@NonNull View itemView, FormAdapter.OnValueSaved valueSavedListener) {
        super(itemView, valueSavedListener);
        this.radioGroup = itemView.findViewById(R.id.radioGroup);

    }

    void bind(FormField fieldItem) {
        super.bind(fieldItem);
        this.fieldItem = fieldItem;
        if (fieldItem.getValueType() == ValueType.TRUE_ONLY) {
            itemView.findViewById(R.id.optionNo).setVisibility(View.GONE);
        } else {
            itemView.findViewById(R.id.optionNo).setVisibility(View.VISIBLE);
        }

        if (fieldItem.getValue() != null && fieldItem.getValue().equals("true")) {
            radioGroup.check(R.id.optionYes);
        } else if (fieldItem.getValue() != null && fieldItem.getValue().equals("false")) {
            radioGroup.check(R.id.optionNo);
        } else {
            radioGroup.clearCheck();
        }


        radioGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            String value;
            switch (i) {
                case R.id.optionYes:
                    value = "true";
                    break;
                case R.id.optionNo:
                    value = "false";
                    break;
                    default:
                        value = null;
                        break;
            }
            System.out.println("Field item is " + fieldItem.getUid());
            valueSavedListener.onValueSaved(fieldItem.getUid(), value);
        });

    }
}
