package com.hiskytechs.muhallinewuserapp.Utill

import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

object ShimmerSkeleton {

    fun start(view: View) {
        view.visibility = View.VISIBLE
        view.startAnimation(newAnimation())
    }

    fun stop(view: View) {
        view.clearAnimation()
        view.visibility = View.GONE
    }

    private fun newAnimation(): Animation {
        return AlphaAnimation(0.45f, 1f).apply {
            duration = 700L
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }
}
