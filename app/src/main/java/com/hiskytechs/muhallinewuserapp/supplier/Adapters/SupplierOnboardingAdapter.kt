package com.hiskytechs.muhallinewuserapp.supplier.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.databinding.ItemSupplierOnboardingBinding
import com.hiskytechs.muhallinewuserapp.supplier.Models.SupplierIntroPage

class SupplierOnboardingAdapter(
    private val items: List<SupplierIntroPage>
) : RecyclerView.Adapter<SupplierOnboardingAdapter.SupplierOnboardingViewHolder>() {

    inner class SupplierOnboardingViewHolder(
        private val binding: ItemSupplierOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SupplierIntroPage) {
            binding.ivOnboarding.setImageResource(item.iconRes)
            binding.tvTitle.text = item.title
            binding.tvDescription.text = item.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierOnboardingViewHolder {
        val binding = ItemSupplierOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SupplierOnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SupplierOnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
