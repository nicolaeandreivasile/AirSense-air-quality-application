package com.test.bluetoothlowenergyapplication.control.ui.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.model.Data;

import java.util.LinkedList;
import java.util.List;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> implements Parcelable {
    private final static String DATA_ADAPTER_TAG = DataAdapter.class.getSimpleName();

    private final List<Data> localDataSet;

    public DataAdapter(List<Data> localDataSet) {
        this.localDataSet = localDataSet;
    }

    @Override
    public DataAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.control_fragment_item_layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DataAdapter.ViewHolder holder, int position) {
        holder.bind(localDataSet.get(position));
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView dataTypeView;
        private TextView dataValueView;

        public ViewHolder(View view) {
            super(view);
            dataTypeView = (TextView) view.findViewById(R.id.dataItemName);
            dataValueView = (TextView) view.findViewById(R.id.dataItemValue);
        }

        public void bind(Data data) {
            dataTypeView.setText(data.getDataName());
            dataValueView.setText(data.getDataValue());
        }
    }

    protected DataAdapter(Parcel in) {
        localDataSet = new LinkedList<>();
        in.readParcelableList(localDataSet, ClassLoader.getSystemClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelableList(localDataSet, flags);
    }

    public static final Creator<DataAdapter> CREATOR = new Creator<DataAdapter>() {
        @Override
        public DataAdapter createFromParcel(Parcel in) {
            return new DataAdapter(in);
        }

        @Override
        public DataAdapter[] newArray(int size) {
            return new DataAdapter[size];
        }
    };
}
