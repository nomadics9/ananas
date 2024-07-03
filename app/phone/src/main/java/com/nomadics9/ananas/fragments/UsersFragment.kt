package com.nomadics9.ananas.fragments

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
import com.nomadics9.ananas.AppNavigationDirections
import com.nomadics9.ananas.adapters.UserListAdapter
import com.nomadics9.ananas.databinding.FragmentUsersBinding
import com.nomadics9.ananas.dialogs.DeleteUserDialogFragment
import com.nomadics9.ananas.viewmodels.UsersEvent
import com.nomadics9.ananas.viewmodels.UsersViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class UsersFragment : Fragment() {

    private lateinit var binding: FragmentUsersBinding
    private val viewModel: UsersViewModel by viewModels()
    private val args: UsersFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentUsersBinding.inflate(inflater)

        binding.usersRecyclerView.adapter =
            UserListAdapter(
                { user ->
                    viewModel.loginAsUser(user)
                },
                { user ->
                    DeleteUserDialogFragment(viewModel, user).show(
                        parentFragmentManager,
                        "deleteUser",
                    )
                    true
                },
            )

        binding.buttonAddUser.setOnClickListener {
            navigateToLoginFragment()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsChannelFlow.collect { event ->
                    when (event) {
                        is UsersEvent.NavigateToHome -> navigateToMainActivity()
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
                        is UsersViewModel.UiState.Normal -> bindUiStateNormal(uiState)
                        is UsersViewModel.UiState.Loading -> Unit
                        is UsersViewModel.UiState.Error -> Unit
                    }
                }
            }
        }

        viewModel.loadUsers(args.serverId)
    }

    fun bindUiStateNormal(uiState: UsersViewModel.UiState.Normal) {
        (binding.usersRecyclerView.adapter as UserListAdapter).submitList(uiState.users)
    }

    private fun navigateToLoginFragment() {
        findNavController().navigate(
            AppNavigationDirections.actionGlobalLoginFragment(),
        )
    }

    private fun navigateToMainActivity() {
        findNavController().navigate(UsersFragmentDirections.actionUsersFragmentToHomeFragment())
    }
}
