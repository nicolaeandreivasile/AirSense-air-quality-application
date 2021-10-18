package com.test.bluetoothlowenergyapplication.control.ui.data;

import android.content.ClipData;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.test.bluetoothlowenergyapplication.control.model.Data;

import java.util.ArrayList;

public class DataViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Intent> selectedIntent;

    public DataViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        this.selectedIntent = new MutableLiveData<Intent>();
    }

    public void selectIntent(Intent intent) {
        selectedIntent.setValue(intent);
    }

    public LiveData<Intent> getSelectedIntent() {
        return selectedIntent;
    }

    public Data getPersistentField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentField(String fieldTag, Data field) {
        savedStateHandle.set(fieldTag, field);
    }

    public ArrayList<Data> getPersistentArrayField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentArrayField(String fieldTag, ArrayList<Data> field) {
        savedStateHandle.set(fieldTag, field);
    }
}
