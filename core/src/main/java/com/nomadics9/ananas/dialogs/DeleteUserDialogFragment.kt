package com.nomadics9.ananas.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nomadics9.ananas.core.R
import com.nomadics9.ananas.models.User
import com.nomadics9.ananas.viewmodels.UsersViewModel
import java.lang.IllegalStateException

class DeleteUserDialogFragment(private val viewModel: UsersViewModel, val user: User) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            builder.setTitle(getString(R.string.remove_user))
                .setMessage(getString(R.string.remove_user_dialog_text, user.name))
                .setPositiveButton(getString(R.string.remove)) { _, _ ->
                    viewModel.deleteUser(user)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
