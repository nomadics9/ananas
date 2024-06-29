package org.askartv.phone.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.askartv.phone.AppNavigationDirections
import timber.log.Timber

fun Fragment.checkIfLoginRequired(error: String?) {
    if (error != null) {
        if (error.contains("401")) {
            Timber.d("Login required!")
            findNavController().navigate(AppNavigationDirections.actionGlobalLoginFragment(reLogin = true))
        }
    }
}
