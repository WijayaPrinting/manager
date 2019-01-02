package com.hendraanggrian.openpss

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.Toast

/** View is the root layout for snackbar and errorbar. */
interface AndroidComponent :
    Component<View, AndroidSetting, SharedPreferences.Editor>,
    StringResources {

    /** To be overriden with dialog, this has to be function instead of type. */
    fun getContext(): Context?

    fun toast(stringId: String) =
        Toast.makeText(getContext(), getString(stringId), Toast.LENGTH_SHORT).show()

    fun longToast(stringId: String) =
        Toast.makeText(getContext(), getString(stringId), Toast.LENGTH_LONG).show()
}