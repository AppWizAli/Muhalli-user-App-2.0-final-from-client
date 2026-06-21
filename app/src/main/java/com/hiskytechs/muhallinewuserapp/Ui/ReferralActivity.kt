package com.hiskytechs.muhallinewuserapp.Ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.hiskytechs.muhallinewuserapp.Adapters.ReferralClaimAdapter
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivityReferralBinding
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter

class ReferralActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReferralBinding
    private lateinit var claimAdapter: ReferralClaimAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        claimAdapter = ReferralClaimAdapter(emptyList())
        binding.rvReferralClaims.layoutManager = LinearLayoutManager(this)
        binding.rvReferralClaims.adapter = claimAdapter

        binding.btnShareReferral.setOnClickListener {
            shareReferralCode(binding.tvReferralCode.text?.toString().orEmpty())
        }
        binding.btnApplyReferral.setOnClickListener {
            val code = binding.etReferralCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                Toast.makeText(this, R.string.referral_code_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AppData.applyReferralCode(
                referralCode = code,
                onSuccess = {
                    bindSummary(it)
                    Toast.makeText(this, R.string.referral_code_applied, Toast.LENGTH_SHORT).show()
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }

        loadSummary()
    }

    private fun loadSummary() {
        AppData.loadReferralSummary(
            onSuccess = { bindSummary(it) },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindSummary(summary: com.hiskytechs.muhallinewuserapp.Models.ReferralSummary) {
        binding.tvReferralCode.text = summary.referralCode.ifBlank {
            getString(R.string.referral_code_placeholder)
        }
        binding.tvReferralStats.text = getString(
            R.string.referral_stats_format,
            summary.totalClaims,
            CurrencyFormatter.format(summary.earnedAmount),
            CurrencyFormatter.format(summary.rewardAmount),
            CurrencyFormatter.format(summary.refereeRewardAmount)
        )
        binding.tvReferralSubtitle.text = if (summary.enabled) {
            getString(R.string.referral_share_description)
        } else {
            getString(R.string.referral_disabled)
        }
        binding.btnShareReferral.isEnabled = summary.enabled && summary.referralCode.isNotBlank()
        claimAdapter.updateItems(summary.recentClaims)
    }

    private fun shareReferralCode(code: String) {
        val message = getString(R.string.referral_share_message, code)
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                getString(R.string.share_referral_code)
            )
        )
    }
}
