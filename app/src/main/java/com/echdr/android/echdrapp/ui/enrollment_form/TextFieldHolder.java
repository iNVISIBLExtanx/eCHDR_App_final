package com.echdr.android.echdrapp.ui.enrollment_form;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.service.forms.FormField;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

class TextFieldHolder extends FieldHolder {

    private final TextInputEditText editText;

    TextFieldHolder(@NonNull View itemView, FormAdapter.OnValueSaved valueSavedListener) {
        super(itemView, valueSavedListener);
        this.editText = itemView.findViewById(R.id.inputEditText);
    }

    void bind(FormField fieldItem) {
        super.bind(fieldItem);

        editText.setFilters(new InputFilter[]{});
        editText.setMaxLines(1);

        switch (fieldItem.getValueType()) {
            case NUMBER:
            case DATE:
            case INTEGER:
            case INTEGER_NEGATIVE:
            case INTEGER_ZERO_OR_POSITIVE:
            case INTEGER_POSITIVE:
            case PERCENTAGE:
            case UNIT_INTERVAL:
                editText.setInputType(InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL |
                        InputType.TYPE_NUMBER_FLAG_SIGNED);
                break;
            case PHONE_NUMBER:
                editText.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
                default:
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    break;
        }

        editText.setText(fieldItem.getValue());

        editText.setEnabled(fieldItem.isEditable());

        /*
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                if (!Objects.equals(fieldItem.getValue(), editText.getText().toString()))
                    valueSavedListener.onValueSaved(fieldItem.getUid(), editText.getText().toString());
            }
        });
         */

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!Objects.equals(fieldItem.getValue(), editText.getText().toString()))
                    valueSavedListener.onValueSaved(fieldItem.getUid(), editText.getText().toString());
            }

        });



    }


}

