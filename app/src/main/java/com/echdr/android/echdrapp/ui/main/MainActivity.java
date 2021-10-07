package com.echdr.android.echdrapp.ui.main;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.data.service.ActivityStarter;
import com.echdr.android.echdrapp.data.service.SyncStatusHelper;
import com.echdr.android.echdrapp.ui.code_executor.CodeExecutorActivity;
import com.echdr.android.echdrapp.ui.d2_errors.D2ErrorActivity;
import com.echdr.android.echdrapp.ui.data_sets.DataSetsActivity;
import com.echdr.android.echdrapp.ui.data_sets.instances.DataSetInstancesActivity;
import com.echdr.android.echdrapp.ui.events.EventsActivity;
import com.echdr.android.echdrapp.ui.foreign_key_violations.ForeignKeyViolationsActivity;
import com.echdr.android.echdrapp.ui.programs.ProgramsActivity;
import com.echdr.android.echdrapp.ui.tracked_entity_instances.TrackedEntityInstancesActivity;
import com.echdr.android.echdrapp.ui.tracked_entity_instances.search.TrackedEntityInstanceSearchActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.hisp.dhis.android.core.arch.call.D2Progress;
import org.hisp.dhis.android.core.user.User;

import java.text.MessageFormat;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity{

    private CompositeDisposable compositeDisposable;

    private Context context;
    private TextView numAllregistrations;
    private TextView numSupplementary;
    private TextView numTherapeutic;
    private TextView numOverweight;
    private TextView numStunting;
    private TextView numOtherHealth;
    private Button syncBtn;
    private Button UploadBtn;
    private LinearLayout myAreaDetailsBtn;
    private LinearLayout createNewChild;
    private boolean isSyncing = false;

    public static Intent getMainActivityIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_yash);

        compositeDisposable = new CompositeDisposable();
        context = this;

        User user = getUser();
        TextView greeting = findViewById(R.id.greetingYash);
        greeting.setText(String.format("%s!", user.displayName()));


        inflateMainView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    private void inflateMainView() {

        syncBtn = findViewById(R.id.syncBtn);
        UploadBtn = findViewById(R.id.uploadBtn);
        myAreaDetailsBtn = findViewById(R.id.area);
        createNewChild = findViewById(R.id.new_child);

        numAllregistrations = findViewById(R.id.numAll);
        numTherapeutic = findViewById(R.id.numTherapeutical);
        numOtherHealth = findViewById(R.id.numOther);
        numOverweight = findViewById(R.id.numOverweight);
        numStunting = findViewById(R.id.numStunting);
        numSupplementary = findViewById(R.id.numSup);

        numAllregistrations.setText(MessageFormat.format("{0}",
                SyncStatusHelper.trackedEntityInstanceCount()));
        numSupplementary.setText(MessageFormat.format("{0}",
                SyncStatusHelper.supplementaryCount()));
        numOtherHealth.setText(MessageFormat.format("{0}",
                SyncStatusHelper.otherCount()));
        numOverweight.setText(MessageFormat.format("{0}",
                SyncStatusHelper.overweightCount()));
        numStunting.setText(MessageFormat.format("{0}",
                SyncStatusHelper.stuntingCount()));
        numTherapeutic.setText(MessageFormat.format("{0}",
                SyncStatusHelper.therapeuticalCount()));

        myAreaDetailsBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent searchAreaDetails = new Intent(getApplicationContext(), TrackedEntityInstancesActivity.class);
                startActivity(searchAreaDetails);

            }
        });

        createNewChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast t = Toast.makeText(context, "Clicked add new child", Toast.LENGTH_LONG);
                t.show();

                /*
                Intent intent = EventsActivity.getIntent(context, "hM6Yt9FQL0n", "qj5r3gSwIww");
                startActivity(intent);
                tharaka's line
                 */


            }
        });

        syncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncMetadata();
                // Downloading data happens after this automatically
            }
        });

        UploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadData();
            }
        });
    }


    private void syncMetadata() {
        compositeDisposable.add(downloadMetadata()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnComplete(this::downloadData)
                .subscribe());
    }

    private Observable<D2Progress> downloadMetadata() {
        return Sdk.d2().metadataModule().download();
    }

    private void downloadData() {

        compositeDisposable.add(
                Observable.merge(
                        downloadTrackedEntityInstances(),
                        downloadSingleEvents(),
                        downloadAggregatedData()
                )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(() -> {
                            ActivityStarter.startActivity(this, MainActivity.getMainActivityIntent(this),
                                    true);
                        })
                        .doOnError(Throwable::printStackTrace)
                        .subscribe());
    }

    private Observable<D2Progress> downloadTrackedEntityInstances() {
        return Sdk.d2().trackedEntityModule().trackedEntityInstanceDownloader()
                .limitByOrgunit(true).limitByProgram(false).download();
    }

    private Observable<D2Progress> downloadSingleEvents() {
        return Sdk.d2().eventModule().eventDownloader()
                .limitByOrgunit(true).limitByProgram(false).download();
    }

    private Observable<D2Progress> downloadAggregatedData() {
        return Sdk.d2().aggregatedModule().data().download();
    }

    private User getUser() {
        return Sdk.d2().userModule().user().blockingGet();
    }

    private void uploadData() {
        compositeDisposable.add(
                Sdk.d2().fileResourceModule().fileResources().upload()
                        .concatWith(Sdk.d2().trackedEntityModule().trackedEntityInstances().upload())
                        .concatWith(Sdk.d2().dataValueModule().dataValues().upload())
                        .concatWith(Sdk.d2().eventModule().events().upload())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(() -> {
                            Toast t = Toast.makeText(context,
                                    "Syncing data complete", Toast.LENGTH_LONG);
                        })
                        .doOnError(Throwable::printStackTrace)
                        .subscribe());
    }


}