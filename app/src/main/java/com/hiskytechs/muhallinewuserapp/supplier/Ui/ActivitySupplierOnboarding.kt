package com.hiskytechs.muhallinewuserapp.supplier.Ui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.hiskytechs.muhallinewuserapp.R
import com.hiskytechs.muhallinewuserapp.databinding.ActivitySupplierOnboardingBinding
import com.hiskytechs.muhallinewuserapp.supplier.Adapters.SupplierOnboardingAdapter
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData

class ActivitySupplierOnboarding : AppCompatActivity() {

    private lateinit var binding: ActivitySupplierOnboardingBinding
    private lateinit var onboardingAdapter: SupplierOnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupplierOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onboardingAdapter = SupplierOnboardingAdapter(SupplierData.getIntroPages())
        binding.viewPager.adapter = onboardingAdapter
        setupIndicators()
        setCurrentIndicator(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentIndicator(position)
                val isLastPage = position == onboardingAdapter.itemCount - 1
                binding.btnNext.text = if (isLastPage) getString(R.string.get_started) else getString(R.string.next)
                binding.tvSkip.visibility = if (isLastPage) android.view.View.GONE else android.view.View.VISIBLE
            }
        })

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem + 1 < onboardingAdapter.itemCount) {
                binding.viewPager.currentItem += 1
            } else {
                openLogin()
            }
        }

        binding.tvSkip.setOnClickListener { openLogin() }
    }

    private fun setupIndicators() {
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(8, 0, 8, 0) }

        repeat(onboardingAdapter.itemCount) {
            val indicator = ImageView(this).apply {
                layoutParams = params
                setImageDrawable(ContextCompat.getDrawable(this@ActivitySupplierOnboarding, R.drawable.indicator_inactive))
            }
            binding.layoutIndicators.addView(indicator)
        }
    }

    private fun setCurrentIndicator(position: Int) {
        for (index in 0 until binding.layoutIndicators.childCount) {
            val imageView = binding.layoutIndicators.getChildAt(index) as ImageView
            imageView.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (index == position) R.drawable.indicator_active else R.drawable.indicator_inactive
                )
            )
        }
    }

    private fun openLogin() {
        startActivity(Intent(this, SupplierLoginActivity::class.java))
        finish()
    }
}
