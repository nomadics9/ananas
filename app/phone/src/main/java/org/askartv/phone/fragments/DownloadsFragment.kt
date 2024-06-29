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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.askartv.phone.AppPreferences
import org.askartv.phone.adapters.FavoritesListAdapter
import org.askartv.phone.databinding.FragmentDownloadsBinding
import org.askartv.phone.models.FindroidItem
import org.askartv.phone.models.FindroidMovie
import org.askartv.phone.models.FindroidShow
import org.askartv.phone.utils.restart
import org.askartv.phone.viewmodels.DownloadsEvent
import org.askartv.phone.viewmodels.DownloadsViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import org.askartv.phone.core.R as CoreR

@AndroidEntryPoint
class DownloadsFragment : Fragment() {
    private lateinit var binding: FragmentDownloadsBinding
    private val viewModel: DownloadsViewModel by viewModels()

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false)

        binding.downloadsRecyclerView.adapter = FavoritesListAdapter { item ->
            navigateToMediaItem(item)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is DownloadsEvent.ConnectionError -> {
                                Snackbar.make(binding.root, CoreR.string.no_server_connection, Snackbar.LENGTH_INDEFINITE)
                                    .setTextMaxLines(2)
                                    .setAction(CoreR.string.offline_mode) {
                                        appPreferences.offlineMode = true
                                        activity?.restart()
                                    }
                                    .show()
                            }
                        }
                    }
                }
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        when (uiState) {
                            is DownloadsViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                            is DownloadsViewModel.UiState.Loading -> bindUiStateLoading()
                            is DownloadsViewModel.UiState.Error -> Unit
                        }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        viewModel.loadData()
    }

    private fun bindUiStateNormal(uiState: DownloadsViewModel.UiState.Normal) {
        binding.loadingIndicator.isVisible = false
        binding.downloadsRecyclerView.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
        binding.noDownloadsText.isVisible = uiState.sections.isEmpty()
        val adapter = binding.downloadsRecyclerView.adapter as FavoritesListAdapter
        adapter.submitList(uiState.sections)
    }

    private fun bindUiStateLoading() {
        binding.loadingIndicator.isVisible = true
        binding.errorLayout.errorPanel.isVisible = false
    }

    private fun navigateToMediaItem(item: FindroidItem) {
        when (item) {
            is FindroidMovie -> {
                findNavController().navigate(
                    DownloadsFragmentDirections.actionDownloadsFragmentToMovieFragment(
                        item.id,
                        item.name,
                    ),
                )
            }
            is FindroidShow -> {
                findNavController().navigate(
                    DownloadsFragmentDirections.actionDownloadsFragmentToShowFragment(
                        item.id,
                        item.name,
                        true,
                    ),
                )
            }
        }
    }
}
