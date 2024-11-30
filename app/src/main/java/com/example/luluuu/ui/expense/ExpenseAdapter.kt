package com.example.luluuu.ui.expense

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.luluuu.databinding.ItemExpenseBinding
import com.example.luluuu.model.Expense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseAdapter(
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(expense: Expense) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "LK"))

            binding.apply {
                descriptionTextView.text = expense.description
                categoryTextView.text = expense.category.name
                dateTextView.text = dateFormat.format(expense.date)
                amountTextView.text = currencyFormat.format(expense.amount)

                editButton.setOnClickListener { onEditClick(expense) }
                deleteButton.setOnClickListener { onDeleteClick(expense) }
            }
        }
    }

    private class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
} 