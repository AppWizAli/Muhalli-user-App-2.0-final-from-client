package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierMessagesBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierConversationAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData

class SupplierMessagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierMessagesBinding
    private lateinit var conversationAdapter: SupplierConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        conversationAdapter = SupplierConversationAdapter(emptyList()) { conversation ->
            startActivity(
                Intent(this, SupplierChatConversationActivity::class.java)
                    .putExtra(SupplierChatConversationActivity.EXTRA_CONVERSATION_ID, conversation.id)
            )
        }
        binding.rvConversations.layoutManager = LinearLayoutManager(this)
        binding.rvConversations.adapter = conversationAdapter
        binding.etSearchConversations.addTextChangedListener { loadConversations() }
        binding.ivBack.setOnClickListener { finish() }
        refreshMessages()
    }

    override fun onResume() {
        super.onResume()
        refreshMessages()
    }

    private fun loadConversations() {
        conversationAdapter.updateItems(
            SupplierData.getConversations(binding.etSearchConversations.text?.toString().orEmpty())
        )
    }

    private fun refreshMessages() {
        SupplierData.refreshMessages(
            onSuccess = {
                loadConversations()
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
