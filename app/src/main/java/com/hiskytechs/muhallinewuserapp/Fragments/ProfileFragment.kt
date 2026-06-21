package com.hiskytechs.muhallinewuserapp.Fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.MainActivity
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.Ui.AboutAppActivity
import com.hiskytechs.muhallinewuserapp.Ui.AccountDetailsActivity
import com.hiskytechs.muhallinewuserapp.Ui.CheckoutAddressActivity
import com.hiskytechs.muhallinewuserapp.Ui.ReferralActivity
import com.hiskytechs.muhallinewuserapp.Utill.ShimmerSkeleton
import com.hiskytechs.muhallinewuserapp.Utill.LogoutManager
import com.hiskytechs.muhallinewuserapp.Utill.ThemeManager
import com.hiskytechs.muhallinewuserapp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var hasLoadedProfile = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swThemeMode.isChecked = ThemeManager.isDarkModeEnabled(requireContext())
        binding.swThemeMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkModeEnabled(requireContext(), isChecked)
        }
        if (!hasLoadedProfile) {
            showLoadingState()
        }
        loadProfile()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        if (!hasLoadedProfile) {
            showLoadingState()
        }
        loadProfile()
    }

    private fun loadProfile() {
        AppData.loadBuyerProfile(
            onSuccess = { profile ->
                if (_binding == null) return@loadBuyerProfile
                hasLoadedProfile = true
                bindProfile(profile)
                hideLoadingState()
            },
            onError = { message ->
                if (_binding == null) return@loadBuyerProfile
                hideLoadingState()
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindProfile(profile: com.hiskytechs.muhallinewuserapp.Models.BuyerProfile) {
        binding.scrollProfileContent.visibility = View.VISIBLE
        binding.tvStoreName.text = profile.buyerName.ifBlank { profile.storeName }
        binding.tvBuyerMeta.text = getString(
            R.string.profile_member_since_format,
            profile.city.ifBlank { profile.buyerName },
            profile.memberSince
        )
        binding.tvPhone.text = profile.phoneNumber
    }

    private fun showLoadingState() {
        if (_binding == null) return
        binding.scrollProfileContent.visibility = View.INVISIBLE
        ShimmerSkeleton.start(binding.layoutProfileSkeleton.root)
    }

    private fun hideLoadingState() {
        if (_binding == null) return
        binding.scrollProfileContent.visibility = View.VISIBLE
        ShimmerSkeleton.stop(binding.layoutProfileSkeleton.root)
    }

    private fun setupClicks() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), AccountDetailsActivity::class.java))
        }
        binding.rowAccountDetails.setOnClickListener {
            startActivity(Intent(requireContext(), AccountDetailsActivity::class.java))
        }
        binding.rowReferrals.setOnClickListener {
            startActivity(Intent(requireContext(), ReferralActivity::class.java))
        }
        binding.rowSavedAddresses.setOnClickListener {
            startActivity(Intent(requireContext(), CheckoutAddressActivity::class.java).apply {
                putExtra(CheckoutAddressActivity.EXTRA_CONTINUE_TO_REVIEW, false)
            })
        }
        binding.rowChats.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_chats)
        }
        binding.rowAboutApp.setOnClickListener {
            startActivity(Intent(requireContext(), AboutAppActivity::class.java))
        }
        binding.rowMyOrders.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_orders)
        }
        binding.btnLogout.setOnClickListener {
            LogoutManager.clearAll(requireContext())
            startActivity(
                Intent().setClassName(
                    requireContext(),
                    "com.hiskytechs.muhallinewuserapp.Ui.LoginActivity"
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            activity?.finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (_binding != null) {
            hideLoadingState()
        }
        _binding = null
    }
}
