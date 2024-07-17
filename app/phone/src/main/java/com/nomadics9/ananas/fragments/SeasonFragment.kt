package com.nomadics9.ananas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import com.nomadics9.ananas.adapters.EpisodeListAdapter
import com.nomadics9.ananas.databinding.FragmentSeasonBinding
import com.nomadics9.ananas.dialogs.ErrorDialogFragment
import com.nomadics9.ananas.dialogs.getStorageSelectionDialog
import com.nomadics9.ananas.models.FindroidEpisode
import com.nomadics9.ananas.utils.checkIfLoginRequired
import com.nomadics9.ananas.viewmodels.SeasonEvent
import com.nomadics9.ananas.viewmodels.SeasonViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nomadics9.ananas.AppPreferences
import com.nomadics9.ananas.models.UiText
import javax.inject.Inject


@AndroidEntryPoint
class SeasonFragment : Fragment() {

    private lateinit var binding: FragmentSeasonBinding
    private val viewModel: SeasonViewModel by viewModels()
    private val args: SeasonFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment
    private lateinit var downloadPreparingDialog: AlertDialog

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(com.nomadics9.ananas.core.R.menu.season_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        com.nomadics9.ananas.core.R.id.action_download_season -> {
                            if (requireContext().getExternalFilesDirs(null).filterNotNull().size > 1) {
                                val storageDialog = getStorageSelectionDialog(
                                    requireContext(),
                                    onItemSelected = { storageIndex ->
                                        createEpisodesToDownloadDialog(storageIndex)
                                    },
                                    onCancel = {
                                    },
                                )
                                viewModel.download()
                                return true
                            }
                            createEpisodesToDownloadDialog()
                            return true
                        }
                        else -> false
                    }
                }

            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is SeasonViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is SeasonViewModel.UiState.Loading -> bindUiStateLoading()
                            is SeasonViewModel.UiState.Error -> bindUiStateError(uiState)
                        }
                    }
                }

                launch {

                    viewModel.downloadStatus.collect { (status, progress) ->
                        when (status) {
                            10 -> {
                                downloadPreparingDialog.dismiss()
                            }
                        }
                    }
                }

                launch {
                    viewModel.downloadError.collect { uiText ->
                        createErrorDialog(uiText)
                    }
                }



                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is SeasonEvent.NavigateBack -> findNavController().navigateUp()
                        }
                    }
                }
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        binding.episodesRecyclerView.adapter =
            EpisodeListAdapter { episode ->
                navigateToEpisodeBottomSheetFragment(episode)
            }
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadEpisodes(args.seriesId, args.seasonId, args.offline)
    }

    private fun bindUiStateNormal(uiState: SeasonViewModel.UiState.Normal) {
        uiState.apply {
            val adapter = binding.episodesRecyclerView.adapter as EpisodeListAdapter
            adapter.submitList(uiState.episodes)
        }
        binding.loadingIndicator.isVisible = false
        binding.episodesRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: SeasonViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.episodesRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun createDownloadPreparingDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        downloadPreparingDialog = builder
            .setTitle(com.nomadics9.ananas.core.R.string.preparing_download)
            .setView(com.nomadics9.ananas.R.layout.preparing_download_dialog)
            .setCancelable(false)
            .create()
        downloadPreparingDialog.show()
    }

    private fun createErrorDialog(uiText: UiText) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder
            .setTitle(com.nomadics9.ananas.core.R.string.downloading_error)
            .setMessage(uiText.asString(requireContext().resources))
            .setPositiveButton(getString(com.nomadics9.ananas.core.R.string.close)) { _, _ ->
            }
        builder.show()
    }

    private fun createEpisodesToDownloadDialog(storageIndex: Int = 0) {
        if (!appPreferences.downloadQualityDefault)
        createPickQualityDialog {
            showDownloadDialog(storageIndex)
        } else {
            showDownloadDialog(storageIndex)
        }
    }

    private fun showDownloadDialog(storageIndex: Int = 0) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val dialog = builder
            .setTitle(com.nomadics9.ananas.core.R.string.download_season_dialog_title)
            .setMessage(com.nomadics9.ananas.core.R.string.download_season_dialog_question)
            .setPositiveButton(com.nomadics9.ananas.core.R.string.download_season_dialog_download_all) { _, _ ->
                createDownloadPreparingDialog()
                viewModel.download(storageIndex = storageIndex, downloadWatched = true)
            }
            .setNegativeButton(com.nomadics9.ananas.core.R.string.download_season_dialog_download_unwatched) { _, _ ->
                createDownloadPreparingDialog()
                viewModel.download(storageIndex = storageIndex, downloadWatched = false)
            }
            .create()
        dialog.show()
    }

    private fun createPickQualityDialog(onQualitySelected: () -> Unit) {
        val qualityEntries = resources.getStringArray(com.nomadics9.ananas.core.R.array.quality_entries)
        val qualityValues = resources.getStringArray(com.nomadics9.ananas.core.R.array.quality_values)
        val quality = appPreferences.downloadQuality
        val currentQualityIndex = qualityValues.indexOf(quality)

        var selectedQuality = quality

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Download Quality")
        builder.setSingleChoiceItems(qualityEntries, currentQualityIndex) { _, which ->
            selectedQuality = qualityValues[which]
        }
        builder.setPositiveButton("Download") { dialog, _ ->
            appPreferences.downloadQuality = selectedQuality
            dialog.dismiss()
            onQualitySelected()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }






    private fun navigateToEpisodeBottomSheetFragment(episode: FindroidEpisode) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                episode.id,
            ),
        )
    }
}
