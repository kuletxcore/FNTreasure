package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.domain.AppInfo

data class FortniteStatus(
    val isInstalled: Boolean,
    val versionName: String?,
)

object FortniteLauncher {
    const val ANDROID_DEMOS_HINT =
        "Android/data/${AppInfo.FORTNITE_PACKAGE}/files/UE4Game/FortniteGame/FortniteGame/Saved/Demos"

    fun status(context: Context): FortniteStatus {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    AppInfo.FORTNITE_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(AppInfo.FORTNITE_PACKAGE, 0)
            }
            FortniteStatus(isInstalled = true, versionName = info.versionName)
        } catch (_: Exception) {
            FortniteStatus(isInstalled = false, versionName = null)
        }
    }

    fun openExperienceBr(context: Context): Boolean {
        val islandUri = Uri.parse(AppInfo.EXPERIENCE_BR_URL)
        val installed = status(context).isInstalled
        if (installed) {
            // Try deep-linking play.fn.gg/island/experience_br directly into com.epicgames.fortnite first
            val targeted = Intent(Intent.ACTION_VIEW, islandUri).apply {
                setPackage(AppInfo.FORTNITE_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (targeted.resolveActivity(context.packageManager) != null) {
                context.startActivity(targeted)
                return true
            }
            // Fallback to launching Fortnite directly
            val launch = context.packageManager.getLaunchIntentForPackage(AppInfo.FORTNITE_PACKAGE)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launch != null) {
                context.startActivity(launch)
                return true
            }
        }
        // Fallback: open play.fn.gg/island/experience_br in browser
        return openUrl(context, AppInfo.EXPERIENCE_BR_URL)
    }

    fun openUrl(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
