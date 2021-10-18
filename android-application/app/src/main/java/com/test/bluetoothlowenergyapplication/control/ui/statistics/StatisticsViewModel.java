package com.test.bluetoothlowenergyapplication.control.ui.statistics;

import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class StatisticsViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Intent> selectedIntent;

    public StatisticsViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        this.selectedIntent = new MutableLiveData<Intent>();
    }

    public void selectIntent(Intent intent) {
        selectedIntent.setValue(intent);
    }

    public LiveData<Intent> getSelectedIntent() {
        return selectedIntent;
    }

    public ArrayList<Entry> getPersistentArrayField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentArrayField(String fieldTag, ArrayList<Entry> field) {
        savedStateHandle.set(fieldTag, field);
    }
}
