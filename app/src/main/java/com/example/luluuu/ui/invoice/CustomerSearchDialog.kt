package com.example.luluuu.ui.invoice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.databinding.DialogCustomerSearchBinding
import com.example.luluuu.model.Customer
import com.example.luluuu.adapter.CustomerAdapter
import com.example.luluuu.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.luluuu.ui.customer.AddCustomerDialog

@AndroidEntryPoint
class CustomerSearchDialog(
    private val onCustomerSelected: (Customer) -> Unit
) : DialogFragment() {
    private var _binding: DialogCustomerSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CustomerViewModel by viewModels()
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomerSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupAddCustomerButton()
        observeCustomers()
    }

    private fun setupRecyclerView() {
        adapter = CustomerAdapter { customer ->
            onCustomerSelected(customer)
            dismiss()
        }
        binding.customersRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CustomerSearchDialog.adapter
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchCustomers(s?.toString() ?: "")
            }
        })
    }

    private fun setupAddCustomerButton() {
        binding.addCustomerFab.setOnClickListener {
            AddCustomerDialog().show(parentFragmentManager, "AddCustomerDialog")
        }
    }

    private fun observeCustomers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collectLatest { customers ->
                adapter.submitList(customers)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
