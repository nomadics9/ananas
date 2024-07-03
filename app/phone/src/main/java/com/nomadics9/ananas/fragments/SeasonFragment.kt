package com.nomadics9.ananas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.nomadics9.ananas.models.FindroidEpisode
import com.nomadics9.ananas.utils.checkIfLoginRequired
import com.nomadics9.ananas.viewmodels.SeasonEvent
import com.nomadics9.ananas.viewmodels.SeasonViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class SeasonFragment : Fragment() {

    private lateinit var binding: FragmentSeasonBinding
    private val viewModel: SeasonViewModel by viewModels()
    private val args: SeasonFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSeasonBinding.inflate(inflater, container, false)
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

    private fun navigateToEpisodeBottomSheetFragment(episode: FindroidEpisode) {
        findNavController().navigate(
            SeasonFragmentDirections.actionSeasonFragmentToEpisodeBottomSheetFragment(
                episode.id,
            ),
        )
    }
}
