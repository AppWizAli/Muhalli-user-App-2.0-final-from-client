package com.hiskytechs.muhallinewuserapp.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hiskytechs.muhallinewuserapp.Models.BuyerNotificationItem
import com.hiskytechs.muhallinewuserapp.databinding.ItemNotificationBinding

class NotificationAdapter(
    private var items: List<BuyerNotificationItem>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvNotificationTitle.text = item.title
            tvNotificationMessage.text = item.message
            tvNotificationTime.text = item.createdAtLabel
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(updated: List<BuyerNotificationItem>) {
        items = updated
        notifyDataSetChanged()
    }
}
