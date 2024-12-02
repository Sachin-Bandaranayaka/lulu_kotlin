package com.example.luluuu.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.luluuu.databinding.DialogAddCustomerBinding
import com.example.luluuu.viewmodel.CustomerViewModel

class AddCustomerDialog : DialogFragment() {
    private var _binding: DialogAddCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[CustomerViewModel::class.java]

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val address = binding.addressEditText.text.toString().trim()

            if (validateInput(name, phone)) {
                viewModel.addCustomer(name, phone, address)
                Toast.makeText(context, "Customer added successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun validateInput(name: String, phone: String): Boolean {
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return false
        }
        binding.nameInputLayout.error = null

        if (phone.isEmpty()) {
            binding.phoneInputLayout.error = "Phone number is required"
            return false
        }
        binding.phoneInputLayout.error = null

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCustomerDialog"
    }
}
