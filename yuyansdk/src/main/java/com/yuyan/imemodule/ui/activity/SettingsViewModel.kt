package com.yuyan.imemodule.ui.activity

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    val toolbarTitle = MutableLiveData("语燕输入法K")

    val toolbarShadow = MutableLiveData(true)

    fun enableToolbarShadow() {
        toolbarShadow.value = true
    }

    fun disableToolbarShadow() {
        toolbarShadow.value = false
    }

    override fun onCleared() {
    }
}
