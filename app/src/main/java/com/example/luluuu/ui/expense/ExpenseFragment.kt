package com.example.luluuu.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.luluuu.R
import com.example.luluuu.databinding.FragmentExpenseBinding
import com.example.luluuu.databinding.DialogExpenseEditBinding
import com.example.luluuu.model.Expense
import com.example.luluuu.model.ExpenseCategory
import com.example.luluuu.viewmodel.ExpenseViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Date

class ExpenseFragment : Fragment() {
    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupFab()
    }

    private fun setupViewPager() {
        val pagerAdapter = ExpensePagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.expenses)
                1 -> getString(R.string.summary)
                else -> null
            }
        }.attach()
    }

    private fun setupFab() {
        binding.addExpenseFab.setOnClickListener {
            showExpenseDialog()
        }
    }

    fun showExpenseDialog(expense: Expense? = null) {
        val binding = DialogExpenseEditBinding.inflate(layoutInflater)
        
        // Pre-fill dialog if editing existing expense
        expense?.let {
            binding.descriptionEditText.setText(it.description)
            binding.amountEditText.setText(it.amount.toString())
            binding.categorySpinner.setText(it.category.name)
        }

        // Setup category spinner
        val categories = ExpenseCategory.values().map { it.name }
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.categorySpinner.setAdapter(arrayAdapter)
        
        // If no category is selected, default to first category
        if (binding.categorySpinner.text.isEmpty()) {
            binding.categorySpinner.setText(categories[0], false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (expense == null) R.string.add_expense else R.string.edit_expense)
            .setView(binding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val description = binding.descriptionEditText.text.toString()
                val amount = binding.amountEditText.text.toString().toDoubleOrNull() ?: 0.0
                val categoryStr = binding.categorySpinner.text.toString()
                
                try {
                    val category = ExpenseCategory.valueOf(categoryStr)
                    val newExpense = expense?.copy(
                        description = description,
                        amount = amount,
                        category = category,
                        date = expense.date,  // Preserve the original date
                        firebaseId = expense.firebaseId,  // Preserve the Firebase ID
                        id = expense.id  // Preserve the local database ID
                    ) ?: Expense(
                        description = description,
                        amount = amount,
                        category = category,
                        date = Date(),
                        firebaseId = "",  // New expenses start with empty Firebase ID
                        id = 0  // Let Room auto-generate the ID
                    )
                    
                    if (expense == null) {
                        viewModel.insert(newExpense)
                    } else {
                        viewModel.update(newExpense)
                    }
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(context, "Invalid category selected", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}