package com.hiskytechs.muhallinewuserapp.supplier.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.FragmentSupplierEarningsBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierTransactionAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierEarningsPeriod
import com.hiskytechs.muhallinewuserapp.supplier.Utill.formatPkr

class SupplierEarningsFragment : Fragment() {

    private var _binding: FragmentSupplierEarningsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionAdapter: SupplierTransactionAdapter
    private var currentPeriod = SupplierEarningsPeriod.ALL
    private var didInitialRefresh = false
    private var skippedInitialResume = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupplierEarningsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionAdapter = SupplierTransactionAdapter(emptyList())
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.setHasFixedSize(true)
        binding.rvTransactions.adapter = transactionAdapter

        val chips = listOf(binding.chipEarningsAll, binding.chipEarningsThisMonth, binding.chipEarningsLastMonth)
        binding.chipEarningsAll.setOnClickListener {
            updateChipState(chips, binding.chipEarningsAll)
            currentPeriod = SupplierEarningsPeriod.ALL
            renderTransactions(currentPeriod)
        }
        binding.chipEarningsThisMonth.setOnClickListener {
            updateChipState(chips, binding.chipEarningsThisMonth)
            currentPeriod = SupplierEarningsPeriod.THIS_MONTH
            renderTransactions(currentPeriod)
        }
        binding.chipEarningsLastMonth.setOnClickListener {
            updateChipState(chips, binding.chipEarningsLastMonth)
            currentPeriod = SupplierEarningsPeriod.LAST_MONTH
            renderTransactions(currentPeriod)
        }

        updateChipState(chips, binding.chipEarningsAll)
        if (SupplierData.restoreCachedEarnings()) {
            didInitialRefresh = true
            bindEarnings()
        }
        refreshEarnings()
    }

    override fun onResume() {
        super.onResume()
        if (!skippedInitialResume) {
            skippedInitialResume = true
            return
        }
        if (_binding != null && didInitialRefresh) {
            refreshEarnings()
        }
    }

    private fun refreshEarnings() {
        SupplierData.refreshEarnings(
            onSuccess = {
                if (_binding == null) return@refreshEarnings
                didInitialRefresh = true
                bindEarnings()
            },
            onError = { message ->
                if (_binding == null) return@refreshEarnings
                if (!didInitialRefresh) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun bindEarnings() {
        val allTransactions = SupplierData.getTransactions(SupplierEarningsPeriod.ALL)
        val thisMonthTransactions = SupplierData.getTransactions(SupplierEarningsPeriod.THIS_MONTH)
        val lastMonthTransactions = SupplierData.getTransactions(SupplierEarningsPeriod.LAST_MONTH)

        binding.tvTotalEarnings.text = formatPkr(allTransactions.sumOf { it.amountPkr })
        binding.tvThisMonthEarnings.text = formatPkr(thisMonthTransactions.sumOf { it.amountPkr })
        binding.tvLastMonthEarnings.text = formatPkr(lastMonthTransactions.sumOf { it.amountPkr })
        renderTransactions(currentPeriod)
    }

    private fun renderTransactions(period: SupplierEarningsPeriod) {
        val transactions = SupplierData.getTransactions(period)
        transactionAdapter.updateItems(transactions)
        binding.tvNoTransactions.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateChipState(chips: List<TextView>, selectedChip: TextView) {
        chips.forEach { chip ->
            val isSelected = chip == selectedChip
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_supplier_pill_selected else R.drawable.bg_supplier_pill_default
            )
            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isSelected) R.color.white else R.color.supplier_text_secondary
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
