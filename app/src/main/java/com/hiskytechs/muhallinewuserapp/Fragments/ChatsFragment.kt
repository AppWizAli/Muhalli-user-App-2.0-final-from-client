package com.hiskytechs.muhallinewuserapp.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.ChatThreadAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.databinding.FragmentChatsBinding

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatThreadAdapter: ChatThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvChats.layoutManager = LinearLayoutManager(requireContext())
        chatThreadAdapter = ChatThreadAdapter(emptyList())
        binding.rvChats.adapter = chatThreadAdapter
        loadChats()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadChats()
        }
    }

    private fun loadChats() {
        AppData.loadChats(
            onSuccess = { threads ->
                if (_binding == null) return@loadChats
                chatThreadAdapter.updateItems(threads)
            },
            onError = { message ->
                if (_binding == null) return@loadChats
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
