package org.askartv.phone.fragments

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
import org.askartv.phone.adapters.FavoritesListAdapter
import org.askartv.phone.databinding.FragmentFavoriteBinding
import org.askartv.phone.dialogs.ErrorDialogFragment
import org.askartv.phone.models.FindroidEpisode
import org.askartv.phone.models.FindroidItem
import org.askartv.phone.models.FindroidMovie
import org.askartv.phone.models.FindroidShow
import org.askartv.phone.utils.checkIfLoginRequired
import org.askartv.phone.viewmodels.CollectionViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import org.askartv.phone.core.R as CoreR

@AndroidEntryPoint
class CollectionFragment : Fragment() {
    private lateinit var binding: FragmentFavoriteBinding
    private val viewModel: CollectionViewModel by viewModels()
    private val args: CollectionFragmentArgs by navArgs()

    private lateinit var errorDialog: ErrorDialogFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater, container, false)

        binding.noFavoritesText.text = getString(CoreR.string.collection_no_media)

        binding.favoritesRecyclerView.adapter = FavoritesListAdapter { item ->
            navigateToMediaItem(item)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is CollectionViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is CollectionViewModel.UiState.Loading -> bindUiStateLoading()
                        is CollectionViewModel.UiState.Error -> bindUiStateError(uiState)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadItems(args.collectionId)
            }
        }

        binding.errorLayout.errorRetryButton.setOnClickListener {
            viewModel.loadItems(args.collectionId)
        }

        binding.errorLayout.errorDetailsButton.setOnClickListener {
            errorDialog.show(parentFragmentManager, ErrorDialogFragment.TAG)
        }

        return binding.root
    }

    private fun bindUiStateNormal(uiState: CollectionViewModel.UiState.Normal) {
        uiState.apply {
            binding.noFavoritesText.isVisible = collectionSections.isEmpty()

            val adapter = binding.favoritesRecyclerView.adapter as FavoritesListAdapter
            adapter.submitList(collectionSections)
        }
        binding.loadingIndicator.isVisible = false
        binding.favoritesRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun bindUiStateError(uiState: CollectionViewModel.UiState.Error) {
        errorDialog = ErrorDialogFragment.newInstance(uiState.error)
        binding.loadingIndicator.isVisible = false
        binding.favoritesRecyclerView.isVisible = false
        binding.errorLayout.errorPanel.isVisible = true
        checkIfLoginRequired(uiState.error.message)
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    CollectionFragmentDirections.actionCollectionFragmentToMovieFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    CollectionFragmentDirections.actionCollectionFragmentToShowFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidEpisode -> {
                findNavController().navigate(
                    CollectionFragmentDirections.actionCollectionFragmentToEpisodeBottomSheetFragment(
                        item.id,
                    ),
                )
            }
        }
    }
}
