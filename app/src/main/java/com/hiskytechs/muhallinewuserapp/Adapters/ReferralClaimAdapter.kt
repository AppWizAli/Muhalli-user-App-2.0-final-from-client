package com.hiskytechs.muhallinewuserapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.ReferralClaim
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ItemReferralClaimBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class ReferralClaimAdapter(
    private var items: List<ReferralClaim>
) : RecyclerView.Adapter<ReferralClaimAdapter.ReferralClaimViewHolder>() {

    class ReferralClaimViewHolder(val binding: ItemReferralClaimBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReferralClaimViewHolder {
        return ReferralClaimViewHolder(
            ItemReferralClaimBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ReferralClaimViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvClaimStore.text = item.referredStoreName
            tvClaimMeta.text = root.context.getString(
                R.string.referral_claim_meta_format,
                item.referredCity,
                item.createdAtLabel
            )
            tvClaimReward.text = CurrencyFormatter.format(item.rewardAmount)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(updated: List<ReferralClaim>) {
        items = updated
        notifyDataSetChanged()
    }
}
