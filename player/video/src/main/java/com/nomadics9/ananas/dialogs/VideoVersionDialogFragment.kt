package com.nomadics9.ananas.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nomadics9.ananas.models.FindroidItem
import com.nomadics9.ananas.player.video.R

fun getVideoVersionDialog(
    context: Context,
    item: FindroidItem,
    onItemSelected: (which: Int) -> Unit,
    onCancel: () -> Unit,
): AlertDialog {
    val items = item.sources.map { "${it.name} - ${it.type}" }.toTypedArray()
    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.select_a_version)
        .setItems(items) { _, which ->
            onItemSelected(which)
        }
        .setOnCancelListener {
            onCancel()
        }
        .create()
    return dialog
}
