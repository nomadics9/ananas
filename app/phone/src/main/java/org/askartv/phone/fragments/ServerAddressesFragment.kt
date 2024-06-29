package org.askartv.phone.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.askartv.phone.adapters.ServerAddressAdapter
import org.askartv.phone.databinding.FragmentServerAddressesBinding
import org.askartv.phone.dialogs.AddServerAddressDialog
import org.askartv.phone.dialogs.DeleteServerAddressDialog
import org.askartv.phone.viewmodels.ServerAddressesEvent
import org.askartv.phone.viewmodels.ServerAddressesViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ServerAddressesFragment : Fragment() {

    private lateinit var binding: FragmentServerAddressesBinding
    private val viewModel: ServerAddressesViewModel by viewModels()
    private val args: UsersFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentServerAddressesBinding.inflate(inflater)

        binding.addressesRecyclerView.adapter =
            ServerAddressAdapter(
                { address ->
                    viewModel.switchToAddress(address)
                },
                { address ->
                    DeleteServerAddressDialog(viewModel, address).show(
                        parentFragmentManager,
                        "deleteServerAddress",
                    )
                    true
                },
            )

        binding.buttonAddAddress.setOnClickListener {
            AddServerAddressDialog(viewModel).show(
                parentFragmentManager,
                "addServerAddress",
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsChannelFlow.collect { event ->
                    when (event) {
                        is ServerAddressesEvent.NavigateToHome -> navigateToMainActivity()
                    }
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    Timber.d("$uiState")
                    when (uiState) {
                        is ServerAddressesViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is ServerAddressesViewModel.UiState.Loading -> Unit
                        is ServerAddressesViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewModel.loadAddresses(args.serverId)
    }

    fun bindUiStateNormal(uiState: ServerAddressesViewModel.UiState.Normal) {
        (binding.addressesRecyclerView.adapter as ServerAddressAdapter).submitList(uiState.addresses)
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToHomeFragment())
    }
}
