package com.echdr.android.echdrapp.ui.event_form;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;

import com.echdr.android.echdrapp.BuildConfig;
import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.data.service.forms.EventFormService;
import com.echdr.android.echdrapp.data.service.forms.FormField;
import com.echdr.android.echdrapp.data.service.forms.RuleEngineService;
import com.echdr.android.echdrapp.databinding.ActivityEnrollmentFormTwoBinding;
//import com.example.android.androidskeletonapp.databinding.ActivityEnrollmentFormTwoBinding;
import com.echdr.android.echdrapp.ui.enrollment_form.FormAdapter;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.android.core.arch.helpers.FileResizerHelper;
import org.hisp.dhis.android.core.arch.helpers.FileResourceDirectoryHelper;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.program.ProgramIndicator;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueObjectRepository;
import org.hisp.dhis.rules.RuleEngine;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionHideField;
import org.hisp.dhis.rules.models.RuleEffect;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

import static android.text.TextUtils.isEmpty;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import java.util.Set;
import java.util.concurrent.TimeUnit;

public class EventFormActivity extends AppCompatActivity {

    private final int CAMERA_RQ = 0;
    private final int CAMERA_PERMISSION = 0;

    private ActivityEnrollmentFormTwoBinding binding;
    private FormAdapter adapter;
    private CompositeDisposable disposable;
    private PublishProcessor<Boolean> engineInitialization;
    private RuleEngineService engineService;
    private RuleEngine ruleEngine;
    private FormType formType;
    private String fieldWaitingImage;
    private String eventUid;
    private String programUid;
    private Context context;
    private String selectedChild;
    private GraphView weightforage;
    private GraphView heightforage;
    private GraphView weightforheight;
    private TextView weightforageLable;
    private TextView heightforageLable;
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
        Intent intent = new Intent(context, EventFormActivity.class);
        intent.putExtra(IntentExtra.EVENT_UID.name(), eventUid);
        intent.putExtra(IntentExtra.PROGRAM_UID.name(), programUid);
        intent.putExtra(IntentExtra.OU_UID.name(), orgUnitUid);
        intent.putExtra(IntentExtra.TYPE.name(), type.name());
        intent.putExtra(IntentExtra.TEI_ID.name(), teiID);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_enrollment_form_two);

        context = this;

        eventUid = getIntent().getStringExtra(IntentExtra.EVENT_UID.name());
        programUid = getIntent().getStringExtra(IntentExtra.PROGRAM_UID.name());
        selectedChild = getIntent().getStringExtra(IntentExtra.TEI_ID.name());
        sex = Sdk.d2().trackedEntityModule().trackedEntityAttributeValues()
                .byTrackedEntityInstance().eq(selectedChild)
                .byTrackedEntityAttribute().eq("lmtzQrlHMYF")
                .one().blockingGet().value();


        formType = FormType.valueOf(getIntent().getStringExtra(IntentExtra.TYPE.name()));

        adapter = new FormAdapter(getValueListener(), getImageListener());

        binding.buttonEndTwo.setOnClickListener(this::finishEnrollment);
        binding.buttonValidateTwo.setOnClickListener(this::evaluateProgramIndicators);
        binding.formRecycler.setAdapter(adapter);

        String Anthropometry = Sdk.d2().programModule().programs()
                .byUid().eq(programUid)
                .one().blockingGet().displayName();
        if(Anthropometry.equals("Anthropometry Programme"))
        {
            System.out.println("Anthropometry Visibility true");
            binding.charts.setVisibility(View.VISIBLE);

            binding.charts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast t = Toast.makeText(context, "Hello world", Toast.LENGTH_LONG);
                    t.show();

                    showCharts();

                }
            });


        }


        binding.title.setText(
                Sdk.d2().programModule().programs()
                        .byUid().eq(programUid)
                        .one().blockingGet().name()
        );


        engineInitialization = PublishProcessor.create();

        if (EventFormService.getInstance().init(
                Sdk.d2(),
                eventUid,
                programUid,
                getIntent().getStringExtra(IntentExtra.OU_UID.name())))
            this.engineService = new RuleEngineService();

    }

    private FormAdapter.OnValueSaved getValueListener() {
        return (fieldUid, value) -> {
            TrackedEntityDataValueObjectRepository valueRepository =
                    Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                            .value(
                                    EventFormService.getInstance().getEventUid(),
                                    fieldUid
                            );
            String currentValue = valueRepository.blockingExists() ?
                    valueRepository.blockingGet().value() : "";
            if (currentValue == null)
                currentValue = "";

            try {
                if (!isEmpty(value)) {
                    valueRepository.blockingSet(value);
                } else {
                    valueRepository.blockingDeleteIfExist();
                }
            } catch (D2Error d2Error) {
                d2Error.printStackTrace();
            } finally {
                if (!value.equals(currentValue)) {
                    engineInitialization.onNext(true);
                }
            }
        };
    }

    private FormAdapter.OnImageSelectionClick getImageListener() {
        return fieldUid -> {
            fieldWaitingImage = fieldUid;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            } else {
                requestCamera();
            }
        };
    }

    private void requestCamera() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePicture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri photoUri = FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                new File(FileResourceDirectoryHelper.getFileResourceDirectory(this), "tempFile.png"));
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePicture, CAMERA_RQ);
    }

    @Override
    protected void onResume() {
        super.onResume();
        disposable = new CompositeDisposable();

        disposable.add(
                Flowable.zip(
                        engineService.configure(Sdk.d2(), programUid, eventUid),
                        EventFormService.getInstance().isListingRendering(),
                        Pair::of
                )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                ruleEngineANDrendering -> {
                                    this.ruleEngine = ruleEngineANDrendering.getLeft();
                                    this.adapter.setListingRendering(ruleEngineANDrendering.getRight());
                                    engineInitialization.onNext(true);
                                },
                                Throwable::printStackTrace
                        )
        );

        disposable.add(
                engineInitialization
                        .flatMap(next ->
                                Flowable.zip(
                                        EventFormService.getInstance().getEventFormFields()
                                                .subscribeOn(Schedulers.io()),
                                        engineService.ruleEvent().flatMap(ruleEvent ->
                                                Flowable.fromCallable(() -> ruleEngine.evaluate(ruleEvent).call()))
                                                .subscribeOn(Schedulers.io()),
                                        this::applyEffects
                                ))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                fieldData -> adapter.updateData(fieldData),
                                Throwable::printStackTrace
                        )
        );
    }

    private List<FormField> applyEffects(Map<String, FormField> fields,
                                         List<RuleEffect> ruleEffects) {

        for (RuleEffect ruleEffect : ruleEffects) {
            RuleAction ruleAction = ruleEffect.ruleAction();
            if (ruleEffect.ruleAction() instanceof RuleActionHideField) {
                fields.remove(((RuleActionHideField) ruleAction).field());
                for (String key : fields.keySet()) //For image options
                    if (key.contains(((RuleActionHideField) ruleAction).field()))
                        fields.remove(key);
            }

        }

        return new ArrayList<>(fields.values());
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposable.clear();
    }

    @Override
    protected void onDestroy() {
        EventFormService.clear();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void finishEnrollment(View view) {
        setResult(RESULT_OK);
        finish();
    }

    private void evaluateProgramIndicators(View view) {
        List<ProgramIndicator> programIndicators = Sdk.d2().programModule()
                .programIndicators()
                .byProgramUid().eq(programUid)
                .byDisplayInForm().isTrue()
                .blockingGet();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                .setTitle("Program indicators");

        if (programIndicators.size() > 0) {
            StringBuilder message = new StringBuilder();

            for (ProgramIndicator pi : programIndicators) {
                String value = Sdk.d2().programModule().programIndicatorEngine()
                        .getEventProgramIndicatorValue(eventUid, pi.uid());

                message.append(pi.displayName()).append(": ").append(value).append("\n");
            }

            dialog.setMessage(message);
        } else {
            dialog.setMessage("There are no program indicators for this program");
        }

        dialog.show();
    }

    @Override
    public void onBackPressed() {
        if (formType == FormType.CREATE)
            EventFormService.getInstance().delete();
        setResult(RESULT_CANCELED);
        finish();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case CAMERA_RQ:
                if (resultCode == RESULT_OK) {
                    File file = new File(
                            FileResourceDirectoryHelper.getFileResourceDirectory(this),
                            "tempFile.png"
                    );
                    if (file.exists()) {
                        try {
                            String fileResourceUid =
                                    Sdk.d2().fileResourceModule().fileResources()
                                            .blockingAdd(FileResizerHelper.resizeFile(file, FileResizerHelper.Dimension.MEDIUM));
                            Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                                    .value(eventUid, fieldWaitingImage).blockingSet(fileResourceUid);
                            engineInitialization.onNext(true);
                        } catch (D2Error d2Error) {
                            d2Error.printStackTrace();
                        } finally {
                            fieldWaitingImage = null;
                        }
                    }
                }
        }
    }


    private void showCharts()
    {
        int currentHeight = 0;
        int currentWeight = 0;
        int currentAge = 0;
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        weightforage = (GraphView) dialog.findViewById(R.id.weightforage);
        heightforage = dialog.findViewById(R.id.heightforage);
        weightforheight = dialog.findViewById(R.id.weightforheight);
        weightforageLable = dialog.findViewById(R.id.weightforageLabel);
        heightforageLable = dialog.findViewById(R.id.heightforageLabel);



        List<TrackedEntityDataValue> t = Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                .byEvent().eq(eventUid)
                .blockingGet();

        for(int i=0; i < t.size();i++)
        {
            try {
                if (t.get(i).dataElement().equals("b4Gpl5ayBe3")) {
                    currentAge = Integer.parseInt(t.get(i).value());
                } else if (t.get(i).dataElement().equals("cDXlUgg1WiZ")) {
                    currentHeight = Integer.parseInt(t.get(i).value());
                } else if (t.get(i).dataElement().equals("rBRI27lvfY5")) {
                    currentWeight = Integer.parseInt(t.get(i).value());
                }
            }catch (Exception e)
            {
                Toast.makeText(getApplicationContext(),
                        "Missing data values",
                        Toast.LENGTH_LONG).show();
            }
        }

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

        int heightCategory = 0;
        try{
            double [] array = heightDataWHO.get(currentAge);
            //int i = 0;
            for(heightCategory=0; heightCategory< 7;)
            {
                if (array[heightCategory] < currentHeight)
                {
                    heightCategory++;
                }else
                {
                    break;
                }
            }
            heightCategory = heightCategory-1;
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        int weightCategory = 0;
        try{
            double [] array = weightDataWHO.get(currentAge);
            //int i = 0;
            for(weightCategory=0; weightCategory< 7;)
            {
                if (array[weightCategory] < currentWeight)
                {
                    weightCategory++;
                }else
                {
                    break;
                }
            }
            weightCategory = weightCategory-1;
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        switch (weightCategory){
            case -1:
                weightforageLable.setText("Less than -3SD");
                weightforageLable.setBackgroundColor(Color.RED);
                break;
            case 0:
                weightforageLable.setText("-2SD to -3SD");
                weightforageLable.setBackgroundColor(Color.rgb(255, 165, 0));
                break;
            case 1:
                weightforageLable.setText("-1SD to -2SD");
                weightforageLable.setBackgroundColor(Color.YELLOW);
                break;
            case 2:
                weightforageLable.setText("+1SD to -1SD");
                weightforageLable.setBackgroundColor(Color.GREEN);
                break;
            case 3:
                weightforageLable.setText("+1SD to +2SD");
                weightforageLable.setBackgroundColor(Color.YELLOW);
                break;
            case 4:
                weightforageLable.setText("+2SD to +3SD");
                weightforageLable.setBackgroundColor(Color.rgb(255, 165, 0));
                break;
            case 5:
                weightforageLable.setText("More than +3SD");
                weightforageLable.setBackgroundColor(Color.RED);
                break;

        }

        switch (heightCategory){
            case -1:
                heightforageLable.setText("Less than -3SD");
                heightforageLable.setBackgroundColor(Color.RED);
                break;
            case 0:
                heightforageLable.setText("-2SD to -3SD");
                heightforageLable.setBackgroundColor(Color.rgb(255, 165, 0));
                break;
            case 1:
                heightforageLable.setText("-1SD to -2SD");
                heightforageLable.setBackgroundColor(Color.YELLOW);
                break;
            case 2:
                heightforageLable.setText("+1SD to -1SD");
                heightforageLable.setBackgroundColor(Color.GREEN);
                break;
            case 3:
                heightforageLable.setText("+1SD to +2SD");
                heightforageLable.setBackgroundColor(Color.YELLOW);
                break;
            case 4:
                heightforageLable.setText("+2SD to +3SD");
                heightforageLable.setBackgroundColor(Color.rgb(255, 165, 0));
                break;
            case 5:
                heightforageLable.setText("More than +3SD");
                heightforageLable.setBackgroundColor(Color.RED);
                break;

        }

        LineGraphSeries<DataPoint> weightforage_series = new LineGraphSeries<DataPoint>();

        LineGraphSeries<DataPoint> weightforage_series_double = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint> heightforage_series  = new LineGraphSeries<DataPoint>();
        LineGraphSeries<DataPoint>  weightforheight_series  = new LineGraphSeries<DataPoint>();

        float[] height_list = new float[60];
        float[] weight_list = new float[60];

        // height repository
        List<TrackedEntityDataValue> height = Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                .byDataElement().eq("cDXlUgg1WiZ")
                .blockingGet();

        // weight repository
        List<TrackedEntityDataValue> weight = Sdk.d2().trackedEntityModule().trackedEntityDataValues()
                .byDataElement().eq("rBRI27lvfY5")
                .blockingGet();

        // Get birthday of the child
        TrackedEntityAttributeValue data = Sdk.d2().trackedEntityModule().trackedEntityAttributeValues()
                .byTrackedEntityInstance().eq(selectedChild)
                .byTrackedEntityAttribute().eq("qNH202ChkV3")
                .one().blockingGet();

        System.out.println("Date of birth is " + data.value());
        SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd");


        try {
            Date dob = formatter2.parse(data.value());
            System.out.println("Date of birth is " + dob);

            for(int i = 0; i < height.size(); i++) {

                //int k = s.compareTo(height.get(i).created());
                long diffInMillies = Math.abs(height.get(i).created().getTime()
                        - dob.getTime());
                // convert to months by dividing days
                int diff = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)/30;

                height_list[diff] = Float.parseFloat(height.get(i).value());
            }

            for (int i = 0; i < weight.size(); i++) {
                //int k = s.compareTo(height.get(i).created());
                long diffInMillies = Math.abs(height.get(i).created().getTime()
                        - dob.getTime());

                // convert to months by dividing days
                int diff = (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS)/30;

                weight_list[diff] = Float.parseFloat(weight.get(i).value());
            }

            // reconstruct weight for age
            for(int i=0; i< weight.size(); i++)
            {
                weightforage_series.appendData(
                        new DataPoint(i, weight_list[i] ), true, 60);
            }

            // reconstruct height for age
            for(int i=0; i< height.size(); i++)
            {
                heightforage_series.appendData(
                        new DataPoint(i, height_list[i] ), true, 60);
            }

            // reconstruct weight for height
            /*
            for(int i=0; i< 60; i++)
            {
                heightforage_series.appendData(
                        new DataPoint(height_list[i] ,
                                weight_list[i] ), false, 60);
            }
             */

            dataValuesWHO e = new dataValuesWHO();
            e.initializeheightForAgeGirls();

            heightforage.addSeries(heightforage_series);

            heightforage.addSeries(e.heightForAgeGirlsValues(0, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(1, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(2, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(3, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(4, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(5, height.size()));
            heightforage.addSeries(e.heightForAgeGirlsValues(6, height.size()));
            heightforage.getViewport().setScrollable(true);
            heightforage.getViewport().setScrollableY(true);
            heightforage.getViewport().setScalable(true);
            heightforage.getViewport().setScalableY(true);
            heightforage.getViewport().setYAxisBoundsManual(true);
            heightforage.getViewport().setMinY(40);
            heightforage.getViewport().setMaxY(130);
            //heightforage.getViewport().setMaxY(130);
            //weightforheight.addSeries(weightforheight_series);


            e.initializeweightForAgeGirls();

            weightforage.addSeries(weightforage_series);

            weightforage.addSeries(e.weightForAgeGirlsValues(0, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(1, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(2, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(3, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(4, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(5, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(6, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(7, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(8, height.size()));
            weightforage.addSeries(e.weightForAgeGirlsValues(9, height.size()));
            weightforage.getViewport().setScrollable(true);
            weightforage.getViewport().setScrollableY(true);
            weightforage.getViewport().setScalable(true);
            weightforage.getViewport().setScalableY(true);
            weightforage.getViewport().setYAxisBoundsManual(true);
            weightforage.getViewport().setMinY(40);
            weightforage.getViewport().setMaxY(130);
            //heightforage.getViewport().setMaxY(130);
            //weightforheight.addSeries(weightforheight_series);


            dialog.show();
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.getWindow().setGravity(Gravity.BOTTOM);

        }
        catch (Exception error)
        {
            System.out.print( "Error in parsing date field: " +  error.toString());
        }

    }


}
