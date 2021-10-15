package com.echdr.android.echdrapp.ui.event_form;

import static android.text.TextUtils.isEmpty;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.data.service.forms.EventFormService;
import com.echdr.android.echdrapp.data.service.forms.RuleEngineService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueObjectRepository;
import org.hisp.dhis.rules.RuleEngine;

import java.util.Map;
import java.util.Objects;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

public class AnthropometryActivity extends AppCompatActivity {

    private String eventUid;
    private String programUid;
    private Context context;
    private String selectedChild;
    private PublishProcessor<Boolean> engineInitialization;
    private RuleEngineService engineService;
    private CompositeDisposable disposable;
    private RuleEngine ruleEngine;
    private FormType formType;
    private GraphView heightGraph;
    private GraphView weightGraph;
    private String sex;

    private enum IntentExtra {
        EVENT_UID, PROGRAM_UID, OU_UID, TYPE, TEI_ID
    }

    public enum FormType {
        CREATE, CHECK
    }

    public static Intent getFormActivityIntent(Context context, String eventUid,
                                               String programUid, String orgUnitUid,
                                               FormType type, String teiID) {
        Intent intent = new Intent(context, AnthropometryActivity.class);
        intent.putExtra(IntentExtra.EVENT_UID.name(), eventUid);
        intent.putExtra(IntentExtra.PROGRAM_UID.name(), programUid);
        intent.putExtra(IntentExtra.OU_UID.name(), orgUnitUid);
        intent.putExtra(IntentExtra.TYPE.name(), type.name());
        intent.putExtra(IntentExtra.TEI_ID.name(), teiID);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anthropometry);

        EditText heightTxt = findViewById(R.id.anthropometryHeight);
        EditText weightTxt = findViewById(R.id.anthropometryWeight);
        FloatingActionButton saveButton = findViewById(R.id.anthropometrySave);
        heightGraph = findViewById(R.id.heightforageAnthropometry);
        weightGraph = findViewById(R.id.weightforageAnthropometry);
        int currentAge = 24;

        eventUid = getIntent().getStringExtra(AnthropometryActivity.IntentExtra.EVENT_UID.name());
        programUid = getIntent().getStringExtra(AnthropometryActivity.IntentExtra.PROGRAM_UID.name());
        selectedChild = getIntent().getStringExtra(AnthropometryActivity.IntentExtra.TEI_ID.name());
        formType = FormType.valueOf(getIntent().getStringExtra(AnthropometryActivity.IntentExtra.TYPE.name()));

        sex = "Male";

        if(formType == FormType.CHECK)
        {
            heightTxt.setText(getDataElement("cDXlUgg1WiZ"));
            weightTxt.setText(getDataElement("rBRI27lvfY5"));
        }

        engineInitialization = PublishProcessor.create();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String heightTextValue = heightTxt.getText().toString();
                String weightTextValue = weightTxt.getText().toString();

                saveDataElement("cDXlUgg1WiZ", heightTextValue); // save height value
                saveDataElement("rBRI27lvfY5", weightTextValue); // save weight value

            }
        });

        setupCharts(sex);

        dataValuesWHO d = new dataValuesWHO();
        Map<Integer, double[]> heightDataWHO;
        Map<Integer, double[]> weightDataWHO;

        if(sex == "Male")
        {
            d.initializeweightForAgeBoys();
            d.initializeheightForAgeBoys();
            heightDataWHO = d.getHeightForAgeBoys();
            weightDataWHO = d.getWeightForAgeBoys();

        }else{
            d.initializeheightForAgeGirls();
            d.initializeweightForAgeGirls();
            heightDataWHO = d.getHeightForAgeGirls();
            weightDataWHO = d.getWeightForAgeGirls();
        }

        heightTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ChangeColor(heightTxt, s, currentAge, heightDataWHO);
            }
        });

        weightTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                ChangeColor(weightTxt, s, currentAge, weightDataWHO);
            }
        });


    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //disposable.clear();
    }

    @Override
    protected void onDestroy() {
        EventFormService.clear();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //if (formType == FormType.CREATE)
        //EventFormService.getInstance().delete();
        EventFormService.clear();
        setResult(RESULT_OK);
        finish();
    }

    private String getDataElement(String dataElement){
        TrackedEntityDataValueObjectRepository valueRepository =
                Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                        .value(
                                eventUid,
                                dataElement
                        );

        String currentValue = valueRepository.blockingExists() ?
                valueRepository.blockingGet().value() : "";

        return currentValue;
    }

    private void saveDataElement(String dataElement, String value){
        TrackedEntityDataValueObjectRepository valueRepository;
        try {
            valueRepository = Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                            .value(
                                    EventFormService.getInstance().getEventUid(),
                                    dataElement
                            );
        }catch (Exception e)
        {
            EventFormService.getInstance().init(
                    Sdk.d2(),
                    eventUid,
                    programUid,
                    getIntent().getStringExtra(AnthropometryActivity.IntentExtra.OU_UID.name()));
            valueRepository = Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                            .value(
                                    EventFormService.getInstance().getEventUid(),
                                    dataElement
                            );
        }

        String currentValue = valueRepository.blockingExists() ?
                valueRepository.blockingGet().value() : "";

        if (currentValue == null)
            currentValue = "";

        try{
            if(!isEmpty(value))
            {
                valueRepository.blockingSet(value);
            }else
            {
                valueRepository.blockingDeleteIfExist();
            }
        } catch (D2Error d2Error) {
            d2Error.printStackTrace();
        }finally {
            if (!value.equals(currentValue)) {
                engineInitialization.onNext(true);
            }
        }
    }

    private void setupCharts(String sex)
    {
        dataValuesWHO e = new dataValuesWHO();
        if(sex.equals("Male"))
        {
            e.initializeheightForAgeBoys();
            e.initializeweightForAgeBoys();

            for(int i=6; i > -1; i--)
            {
                heightGraph.addSeries(e.heightForAgeBoysValues(i, 60));
                weightGraph.addSeries(e.weightForAgeBoys(i, 60));
            }

        }else {
            e.initializeheightForAgeGirls();
            e.initializeweightForAgeGirls();

            for(int i=6; i > -1; i--)
            {
                heightGraph.addSeries(e.heightForAgeGirlsValues(i, 60));
                weightGraph.addSeries(e.weightForAgeGirlsValues(i, 60));
            }

        }
    }

    private void ChangeColor(EditText text, Editable s, int currentAge,
                             Map<Integer, double[]> data )
    {
        int currentValue;
        if(s.toString().isEmpty())
        {
            currentValue = 0;
        }else{
            currentValue = Integer.parseInt(s.toString());
        }

        int category = 0;
        try{
            double [] array = data.get(currentAge);
            //int i = 0;
            for(category=0; category< 7;)
            {

                if (array[category] < currentValue)
                {
                    category++;
                }else
                {
                    break;
                }
            }
            category = category-1;
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        switch (category){
            case -1:
            case 5:
                text.setBackgroundColor(Color.RED);
                break;
            case 0:
            case 4:
                text.setBackgroundColor(Color.rgb(255, 165, 0));
                break;
            case 1:
            case 3:
                text.setBackgroundColor(Color.YELLOW);
                break;
            case 2:
                text.setBackgroundColor(Color.GREEN);
                break;

        }

    }


}