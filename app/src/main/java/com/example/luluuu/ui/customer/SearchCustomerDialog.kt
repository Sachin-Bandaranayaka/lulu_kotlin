package com.example.luluuu.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.adapter.CustomerAdapter
import com.example.luluuu.databinding.DialogSearchCustomerBinding
import com.example.luluuu.model.Customer
import com.example.luluuu.viewmodel.CustomerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SearchCustomerDialog(
    private val onCustomerSelected: (Customer) -> Unit
) : DialogFragment() {
    private var _binding: DialogSearchCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CustomerViewModel
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSearchCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CustomerViewModel::class.java]

        setupRecyclerView()
        setupSearchInput()
        setupAddCustomerButton()
        observeSearchResults()
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter { customer ->
            onCustomerSelected(customer)
            dismiss()
        }
        
        binding.customersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchCustomerDialog.adapter
        }
    }

    private fun setupSearchInput() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.searchCustomers(text?.toString() ?: "")
        }
    }

    private fun setupAddCustomerButton() {
        binding.addCustomerFab.setOnClickListener {
            AddCustomerDialog().show(parentFragmentManager, AddCustomerDialog.TAG)
        }
    }

    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collectLatest { customers ->
                adapter.submitList(customers)
                updateEmptyState(customers.isEmpty())
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            noResultsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
            customersRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SearchCustomerDialog"
    }
}
