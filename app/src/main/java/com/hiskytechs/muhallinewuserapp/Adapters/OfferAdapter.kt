package com.hiskytechs.muhallinewuserapp.Adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.MarketplaceOffer
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.loadMarketplaceImage
import com.hiskytechs.muhallinewuserapp.databinding.ItemOfferBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class OfferAdapter(
    private var offers: List<MarketplaceOffer>,
    private val onClick: (MarketplaceOffer) -> Unit
) : RecyclerView.Adapter<OfferAdapter.OfferViewHolder>() {

    class OfferViewHolder(val binding: ItemOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        return OfferViewHolder(
            ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        val offer = offers[position]
        holder.binding.apply {
            tvOfferBadge.text = offer.badgeLabel.ifBlank {
                offer.discountLabel.ifBlank { root.context.getString(R.string.limited_offer) }
            }
            tvOfferTitle.text = offer.title
            tvOfferDescription.text = offer.description.ifBlank { offer.discountLabel }
            tvOfferMeta.text = listOf(offer.supplierName, offer.productName, offer.city)
                .filter { it.isNotBlank() }
                .joinToString(" - ")
            val hasOfferPrice = offer.offerPrice > 0.0 && offer.originalPrice > offer.offerPrice
            offerPriceRow.visibility = if (hasOfferPrice) View.VISIBLE else View.GONE
            if (hasOfferPrice) {
                tvOfferPrice.text = formatCurrency(offer.offerPrice)
                tvOriginalPrice.text = formatCurrency(offer.originalPrice)
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            ivOffer.loadMarketplaceImage(offer.imageUrl)
            root.setOnClickListener { onClick(offer) }
        }
    }

    override fun getItemCount(): Int = offers.size

    fun updateItems(updated: List<MarketplaceOffer>) {
        val previous = offers
        offers = updated
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = previous.size
            override fun getNewListSize(): Int = updated.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition].id == updated[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return previous[oldItemPosition] == updated[newItemPosition]
            }
        }).dispatchUpdatesTo(this)
    }

    private fun ItemOfferBinding.formatCurrency(amount: Double): String {
        return CurrencyFormatter.format(amount)
    }
}
