package com.echdr.android.echdrapp.ui.tracked_entity_instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.data.service.ActivityStarter;
import com.echdr.android.echdrapp.ui.base.ListActivity;
import com.echdr.android.echdrapp.ui.enrollment_form.EnrollmentFormActivity;

import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceCollectionRepository;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceCreateProjection;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static android.text.TextUtils.isEmpty;

public class TrackedEntityInstancesActivity extends ListActivity {

    private CompositeDisposable compositeDisposable;
    private String selectedProgram;
    private final int ENROLLMENT_RQ = 1210;
    private TrackedEntityInstanceAdapter adapter;


    private enum IntentExtra {
        TRACKED_ENTITY_INSTANCE
    }

    public static Intent getTrackedEntityInstancesActivityIntent(Context context, String trackedEntityInstanceUid) {
        Intent intent = new Intent(context, TrackedEntityInstancesActivity.class);
        intent.putExtra(IntentExtra.TRACKED_ENTITY_INSTANCE.name(), trackedEntityInstanceUid);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp(R.layout.activity_tracked_entity_instances, R.id.trackedEntityInstancesRecyclerView);
        compositeDisposable = new CompositeDisposable();
        observeTrackedEntityInstances();
/*
        if (isEmpty(selectedProgram))
            findViewById(R.id.enrollmentButton).setVisibility(View.GONE);



        findViewById(R.id.enrollmentButton).setOnClickListener(view -> compositeDisposable.add(
                Sdk.d2().programModule().programs().uid(selectedProgram).get()
                        .map(program -> Sdk.d2().trackedEntityModule().trackedEntityInstances()
                                .blockingAdd(
                                        TrackedEntityInstanceCreateProjection.builder()
                                                .organisationUnit(Sdk.d2().organisationUnitModule().organisationUnits()
                                                        .one().blockingGet().uid())
                                                .trackedEntityType(program.trackedEntityType().uid())
                                                .build()
                                ))
                        .map(teiUid -> EnrollmentFormActivity.getFormActivityIntent(
                                TrackedEntityInstancesActivity.this,
                                teiUid,
                                selectedProgram,
                                Sdk.d2().organisationUnitModule().organisationUnits().one().blockingGet().uid()
                        ))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                activityIntent ->
                                        ActivityStarter.startActivityForResult(
                                                TrackedEntityInstancesActivity.this, activityIntent,ENROLLMENT_RQ),
                                Throwable::printStackTrace
                        )
        ));
*/

    }



    private void observeTrackedEntityInstances() {
        adapter = new TrackedEntityInstanceAdapter(this);
        recyclerView.setAdapter(adapter);

        try {
            getTeiRepository().getPaged(20).observe(this, trackedEntityInstancePagedList -> {
                adapter.setSource(trackedEntityInstancePagedList.getDataSource());
                adapter.submitList(trackedEntityInstancePagedList);
            });
        }
        catch(Exception e){
            setUp(R.layout.activity_tracked_entity_instances, R.id.trackedEntityInstancesRecyclerView);
        }
        //findViewById(R.id.trackedEntityInstancesNotificator).setVisibility(
        //      trackedEntityInstancePagedList.isEmpty() ? View.VISIBLE : View.GONE);

    }

    private TrackedEntityInstanceCollectionRepository getTeiRepository() {
        TrackedEntityInstanceCollectionRepository teiRepository = null;
        try{
            teiRepository = Sdk.d2().trackedEntityModule().trackedEntityInstances().withTrackedEntityAttributeValues();
        }
        catch (Exception e){
            Toast.makeText(this, "This data is partially filled", Toast.LENGTH_LONG).show();
        }

        return teiRepository;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == ENROLLMENT_RQ && resultCode == RESULT_OK){
            adapter.invalidateSource();
        }
        super.onActivityResult(requestCode,resultCode,data);
    }


}
