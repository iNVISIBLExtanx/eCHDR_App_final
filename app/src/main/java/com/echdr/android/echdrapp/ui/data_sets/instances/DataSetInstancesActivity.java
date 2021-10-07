package com.echdr.android.echdrapp.ui.data_sets.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.Sdk;
import com.echdr.android.echdrapp.ui.base.ListActivity;

import org.hisp.dhis.android.core.dataset.DataSetInstance;

public class DataSetInstancesActivity extends ListActivity {

    public static Intent getIntent(Context context) {
        return new Intent(context,DataSetInstancesActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUp(R.layout.activity_data_set_instances, R.id.dataSetInstancesRecyclerView);
        observeDataSetInstances();
    }

    private void observeDataSetInstances() {
        DataSetInstancesAdapter adapter = new DataSetInstancesAdapter(this);
        recyclerView.setAdapter(adapter);

        LiveData<PagedList<DataSetInstance>> liveData = Sdk.d2().dataSetModule().dataSetInstances()
                .getPaged(20);

        liveData.observe(this, dataSetInstancePagedList -> {
            adapter.submitList(dataSetInstancePagedList);
            findViewById(R.id.dataSetInstancesNotificator).setVisibility(
                    dataSetInstancePagedList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
