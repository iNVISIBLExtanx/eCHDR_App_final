package com.echdr.android.echdrapp.ui.enrollment_form;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.echdr.android.echdrapp.R;
import com.echdr.android.echdrapp.data.service.forms.FormField;

import org.hisp.dhis.android.core.common.ValueType;

import java.util.ArrayList;
import java.util.List;

public class FormAdapter extends RecyclerView.Adapter<FieldHolder> {

    private int height = -1;
    private int weight = -1;
    private int age = -1;

    private final int OPTIONSET = 98;
    private final int OPTIONSETIMAGE = 99;
    private final OnValueSaved valueSavedListener;
    private final OnImageSelectionClick imageSelectionListener;
    private boolean isListingRendering = true;
    private String sex;

    private List<FormField> fields;

    public FormAdapter(OnValueSaved valueSavedListener) {
        this.fields = new ArrayList<>();
        this.valueSavedListener = valueSavedListener;
        this.imageSelectionListener = null;
        setHasStableIds(true);
    }

    public FormAdapter(OnValueSaved valueSavedListener,
                       OnImageSelectionClick imageSelectionListener) {
        this.fields = new ArrayList<>();
        this.valueSavedListener = valueSavedListener;
        this.imageSelectionListener = imageSelectionListener;
        setHasStableIds(true);
    }

    public FormAdapter(OnValueSaved valueSavedListener,
                       OnImageSelectionClick imageSelectionListener,
                       String sex) {
        this.fields = new ArrayList<>();
        this.valueSavedListener = valueSavedListener;
        this.imageSelectionListener = imageSelectionListener;
        this.sex = sex;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public FieldHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == OPTIONSET) {
            return new OptionSetFieldHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_field_optionset, parent, false), valueSavedListener);
        } else if (viewType == OPTIONSETIMAGE) {
            return new OptionSetImageFieldHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_field_optionset_image, parent, false), valueSavedListener);
        } else
            switch (ValueType.values()[viewType]) {
                case DATE:
                    return new DateFieldHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_date_field, parent, false), valueSavedListener);
                case BOOLEAN:
                case TRUE_ONLY:
                    return new BooleanFieldHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_boolean_field, parent, false), valueSavedListener);
                case IMAGE:
                    return new ImageFieldHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_image_field, parent, false), valueSavedListener,
                            imageSelectionListener);
                default:
                    return new TextFieldHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_field, parent, false), valueSavedListener);
            }
    }

    @Override
    public void onBindViewHolder(@NonNull FieldHolder holder, int position) {
        holder.bind(fields.get(position));
        System.out.println(fields.get(position).getFormLabel());
        System.out.println(fields.get(position).getValue());


        if(fields.get(position).getFormLabel().equals("MSGP|Weight"))
        {
            try {
                weight = Integer.parseInt(fields.get(position).getValue());
            }
            catch(Exception e)
            {
                System.out.println("Value is null");
            }
        }

        if(fields.get(position).getFormLabel().equals("MSGP|Length/Height"))
        {
            try {
                height = Integer.parseInt(fields.get(position).getValue());
            }
            catch(Exception e)
            {
                System.out.println("Value is null");
            }
        }

        if(fields.get(position).getFormLabel().equals("MSGP|Age in months"))
        {
            try {
                age = Integer.parseInt(fields.get(position).getValue());
            }
            catch(Exception e)
            {
                System.out.println("Value is null");
            }
        }

        if(height != -1 && weight != -1 && age != -1)
        {
            holder.changeColor(age, weight, height, sex);
        }
    }

    @Override
    public int getItemCount() {
        return fields.size();
    }

    @Override
    public long getItemId(int position) {
        return fields.get(position).hashCode();
    }

    public void updateData(List<FormField> updates) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return fields.size();
            }

            @Override
            public int getNewListSize() {
                return updates.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return fields.get(oldItemPosition).getUid().equals(updates.get(newItemPosition).getUid());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return fields.get(oldItemPosition) == updates.get(newItemPosition);
            }
        });

        fields.clear();
        fields.addAll(updates);

        //System.out.println("Printing Each time");

        for(int i=0; i<updates.size();i++)
        {
            if(updates.get(i).getFormLabel().equals("MSGP|Weight"))
            {
                try {
                    weight = Integer.parseInt(updates.get(i).getValue());
                }
                catch(Exception e)
                {
                    System.out.println("Value is null");
                }
            }

            if(updates.get(i).getFormLabel().equals("MSGP|Length/Height"))
            {
                try {
                    height = Integer.parseInt(updates.get(i).getValue());
                }
                catch(Exception e)
                {
                    System.out.println("Value is null");
                }
            }

            if(updates.get(i).getFormLabel().equals("MSGP|Age in months"))
            {
                try {
                    age = Integer.parseInt(updates.get(i).getValue());
                }
                catch(Exception e)
                {
                    System.out.println("Value is null");
                }
            }

            //holder.changeColor(age, weight, height, sex);
            System.out.println("Update after every entry " + age
            + " weight " + weight + " Height " + height);

        }

        diffResult.dispatchUpdatesTo(this);
    }


    @Override
    public int getItemViewType(int position) {
        if (fields.get(position).getOptionSetUid() != null && fields.get(position).getOptionSetUid() != null)
            if (isListingRendering)
                return OPTIONSET;
            else
                return OPTIONSETIMAGE;
        else
            return fields.get(position).getValueType().ordinal();


    }

    public void setListingRendering(boolean isListingRendering) {
        this.isListingRendering = isListingRendering;
    }

    public interface OnValueSaved {
        void onValueSaved(String fieldUid, String value);
    }

    public interface OnImageSelectionClick {
        void onImageSelectionClick(String fieldUid);
    }
}
