package com.example.luluuu.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.luluuu.databinding.FragmentExpenseListBinding
import com.example.luluuu.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch

class ExpenseListFragment : Fragment() {
    private var _binding: FragmentExpenseListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExpenseViewModel by activityViewModels()
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeExpenses()
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onEditClick = { expense ->
                (parentFragment as? ExpenseFragment)?.showExpenseDialog(expense)
            },
            onDeleteClick = { expense ->
                viewModel.delete(expense)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expenseAdapter
        }
    }

    private fun observeExpenses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allExpenses.collect { expenses ->
                expenseAdapter.submitList(expenses)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 