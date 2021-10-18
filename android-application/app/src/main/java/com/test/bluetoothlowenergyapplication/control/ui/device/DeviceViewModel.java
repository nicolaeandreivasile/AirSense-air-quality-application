package com.test.bluetoothlowenergyapplication.control.ui.device;

import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.test.bluetoothlowenergyapplication.control.model.Data;

public class DeviceViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Intent> selectedIntent;

    public DeviceViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        this.selectedIntent = new MutableLiveData<>();
    }

    public void selectItem(Intent intent) {
        selectedIntent.setValue(intent);
    }

    public LiveData<Intent> getSelectedItem() {
        return selectedIntent;
    }

    public String getPersistentField(String fieldTag) {
        return savedStateHandle.get(fieldTag);
    }

    public void setPersistentField(String fieldTag, String field) {
        savedStateHandle.set(fieldTag, field);
    }
}
