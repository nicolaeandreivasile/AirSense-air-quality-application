package com.test.bluetoothlowenergyapplication.control.ui.map;

import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.MarkerOptions;
import com.test.bluetoothlowenergyapplication.control.model.Measurement;

import java.util.ArrayList;

public class MapViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Intent> selectedIntent;

    public MapViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        this.selectedIntent = new MutableLiveData<Intent>();
    }

    public void selectIntent(Intent intent) {
        selectedIntent.setValue(intent);
    }

    public LiveData<Intent> getSelectedIntent() {
        return selectedIntent;
    }

    public String getPersistentField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentField(String fieldTag, String field) {
        savedStateHandle.set(fieldTag, field);
    }

    public ArrayList<Measurement> getPersistentArrayMeasurements(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentArrayMeasurements(String fieldTag, ArrayList<Measurement> field) {
        savedStateHandle.set(fieldTag, field);
    }


    public ArrayList<MarkerOptions> getPersistentArrayMarkerOptions(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentArrayMarkerOptions(String fieldTag, ArrayList<MarkerOptions> field) {
        savedStateHandle.set(fieldTag, field);
    }
}
