package com.hiskytechs.muhallinewuserapp.Utill

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.hiskytechs.muhallinewuserapp.Data.AppData
import com.hiskytechs.muhallinewuserapp.network.AppSession
import com.hiskytechs.muhallinewuserapp.notifications.AppNotificationHelper
import com.hiskytechs.muhallinewuserapp.supplier.Data.SupplierData
import java.io.File

object LogoutManager {

    fun clearAll(context: Context) {
        CartManager.clearCart()
        AppData.clearCachedState()
        SupplierData.clearCachedState()
        AppSession.clearAll()
        AppNotificationHelper.clearAll(context)
        clearImageCache(context)
        clearAppCache(context)
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun clearImageCache(context: Context) {
        runCatching { context.imageLoader.memoryCache?.clear() }
        runCatching { context.imageLoader.diskCache?.clear() }
    }

    private fun clearAppCache(context: Context) {
        clearDirectoryContents(context.cacheDir)
        context.externalCacheDir?.let(::clearDirectoryContents)
    }

    private fun clearDirectoryContents(directory: File) {
        val files = directory.listFiles().orEmpty()
        files.forEach { file ->
            runCatching {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    }
}
