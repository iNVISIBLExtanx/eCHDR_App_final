package com.echdr.android.echdrapp.ui.enrollment_form;

import android.graphics.Color;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.data.service.forms.FormField;
import com.echdr.android.echdrapp.ui.event_form.dataValuesWHO;

import org.hisp.dhis.android.core.option.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class OptionSetFieldHolder extends FieldHolder {

    private final Spinner spinner;
    private List<Option> optionList;
    private String fieldUid;
    private String fieldCurrentValue;

    OptionSetFieldHolder(@NonNull View itemView, FormAdapter.OnValueSaved valueSavedListener) {
        super(itemView, valueSavedListener);
        this.spinner = itemView.findViewById(R.id.spinner);
    }

    void bind(FormField fieldItem) {
        super.bind(fieldItem);
        fieldUid = fieldItem.getUid();
        fieldCurrentValue = fieldItem.getValue();

        setUpSpinner(fieldItem.getOptionSetUid());

        if (fieldCurrentValue != null)
            setInitialValue(fieldCurrentValue);
    }

    private void setUpSpinner(String optionSetUid) {
        optionList = Sdk.d2().optionModule().options().byOptionSetUid().eq(optionSetUid).blockingGet();
        List<String> optionListNames = new ArrayList<>();
        optionListNames.add(label.getText().toString());
        for (Option option : optionList) optionListNames.add(option.displayName());
        spinner.setAdapter(new ArrayAdapter<>(itemView.getContext(),
                android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, optionListNames));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i > 0) {
                    if (fieldCurrentValue == null || !Objects.equals(fieldCurrentValue, optionList.get(i - 1).code()))
                        valueSavedListener.onValueSaved(fieldUid, optionList.get(i - 1).code());
                } else if (fieldCurrentValue != null)
                    valueSavedListener.onValueSaved(fieldUid, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }

    private void setInitialValue(String selectedCode) {
        for (int i = 0; i < optionList.size(); i++) {
            if (Objects.equals(optionList.get(i).code(), selectedCode))
                spinner.setSelection(i + 1);
        }
    }

    public void changeColor(int age, int weight, int height, String sex){

        dataValuesWHO d = new dataValuesWHO();
        Map<Integer, double[]> heightData;
        Map<Integer, double[]> weightData;
        if(sex == "Male")
        {
            d.initializeweightForAgeBoys();
            d.initializeheightForAgeBoys();
            heightData = d.getHeightForAgeBoys();
            weightData = d.getWeightForAgeBoys();

        }else{
            d.initializeheightForAgeGirls();
            d.initializeweightForAgeGirls();
            heightData = d.getHeightForAgeGirls();
            weightData = d.getWeightForAgeGirls();
        }


        if(label.getText().toString().equals("MSGP|Weight for Age"))
        {
            System.out.println("Age " + age + " Weight " + weight);

        }

        if(label.getText().toString().equals("MSGP|Length/Height for Age"))
        {
            int selection = 0;
            int i = 0;
            System.out.println("Height for age | Age " + age + " Height " + height);
            try{
                double [] array = heightData.get(age);
                //int i = 0;
                for(i=0; i < 7;)
                {
                    if (array[i] < height)
                    {
                        i++;
                        //System.out.println("Compared to " + array[i] );
                    }else
                    {
                        break;
                    }
                }
                i = i-1;
                System.out.println("Selected category is " + i );
            }catch (Exception e)
            {
                e.printStackTrace();
            }
            //spinner.setBackgroundColor(Color.RED);
            //0xFFFF0000
            switch (i){
                case -1:
                    selection = 6;
                    spinner.setBackgroundColor(Color.RED);
                    break;
                case 0: selection = 5;
                    spinner.setBackgroundColor(Color.rgb(255, 165, 0));
                        break;
                case 1: selection = 4;
                    spinner.setBackgroundColor(Color.YELLOW);
                        break;
                case 2: selection = 2;
                    spinner.setBackgroundColor(Color.GREEN);
                        break;
                case 3: selection = 1;
                    spinner.setBackgroundColor(Color.YELLOW);
                        break;
                case 4: selection = 3;
                    spinner.setBackgroundColor(Color.rgb(255, 165, 0));
                        break;
                case 5: selection = 7;
                    spinner.setBackgroundColor(Color.RED);
                        break;

            }
            spinner.setSelection(selection);

        }


    }


}
