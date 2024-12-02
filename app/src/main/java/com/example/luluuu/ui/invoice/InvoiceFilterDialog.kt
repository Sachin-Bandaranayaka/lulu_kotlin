package com.example.luluuu.ui.invoice

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.luluuu.databinding.DialogInvoiceFilterBinding
import com.example.luluuu.model.Customer
import java.text.SimpleDateFormat
import java.util.*

class InvoiceFilterDialog : DialogFragment() {
    private var _binding: DialogInvoiceFilterBinding? = null
    private val binding get() = _binding!!
    private var selectedCustomer: Customer? = null
    private var fromDate: Calendar? = null
    private var toDate: Calendar? = null
    private var onFilterApplied: ((Customer?, Date?, Date?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogInvoiceFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePickers()
        setupCustomerSelection()
        setupButtons()
    }

    private fun setupDatePickers() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        binding.dateFromInput.setOnClickListener {
            showDatePicker { calendar ->
                fromDate = calendar
                binding.dateFromInput.setText(dateFormat.format(calendar.time))
            }
        }

        binding.dateToInput.setOnClickListener {
            showDatePicker { calendar ->
                toDate = calendar
                binding.dateToInput.setText(dateFormat.format(calendar.time))
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupCustomerSelection() {
        binding.customerInput.setOnClickListener {
            val dialog = CustomerSearchDialog { customer ->
                selectedCustomer = customer
                binding.customerInput.setText(customer.name)
            }
            dialog.show(parentFragmentManager, "CustomerSearchDialog")
        }
    }

    private fun setupButtons() {
        binding.clearButton.setOnClickListener {
            binding.dateFromInput.text?.clear()
            binding.dateToInput.text?.clear()
            binding.customerInput.text?.clear()
            selectedCustomer = null
            fromDate = null
            toDate = null
        }

        binding.applyButton.setOnClickListener {
            onFilterApplied?.invoke(
                selectedCustomer,
                fromDate?.time,
                toDate?.time
            )
            dismiss()
        }
    }

    fun setOnFilterAppliedListener(listener: (Customer?, Date?, Date?) -> Unit) {
        onFilterApplied = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = InvoiceFilterDialog()
    }
}
