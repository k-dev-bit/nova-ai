package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Ambiguous(val message: String, val options: List<String>) : ActionResult()
    data class RequiresInput(val prompt: String, val contactName: String) : ActionResult()
    data class Failure(val reason: String) : ActionResult()
}

class AndroidActionEngine(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private var isTorchOn = false

    /**
     * Resolves contact in:
     * 1. Saved local memory
     * 2. Android device contacts
     * 3. Asks user for phone number if not found
     */
    suspend fun resolveAndCallContact(targetNameOrQuery: String): ActionResult = withContext(Dispatchers.IO) {
        val query = targetNameOrQuery.trim()
        val normalizedQuery = query.lowercase()

        // 1. Search local memory
        val localContacts = db.memoryDao().getEnabledContextMemories().filter {
            it.category == "Important Contact" || it.title.isNotBlank()
        }

        val directMatch = localContacts.firstOrNull {
            it.title.equals(query, ignoreCase = true) ||
            (normalizedQuery.contains("papa") && it.title.equals("Papa", ignoreCase = true)) ||
            (normalizedQuery.contains("bhaiya") && it.title.equals("Bhaiya", ignoreCase = true)) ||
            (normalizedQuery.contains("mummy") && it.title.equals("Mummy", ignoreCase = true)) ||
            (normalizedQuery.contains("father") && it.title.equals("Papa", ignoreCase = true)) ||
            (normalizedQuery.contains("brother") && it.title.equals("Bhaiya", ignoreCase = true)) ||
            (normalizedQuery.contains("mother") && it.title.equals("Mummy", ignoreCase = true))
        }

        if (directMatch != null && !directMatch.extraData.isNullOrBlank()) {
            val phone = directMatch.extraData
            launchDialer(phone)
            return@withContext ActionResult.Success("Opening dialer for ${directMatch.title} ($phone)...")
        }

        // 2. Search Android Contacts if permission is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            val deviceContact = searchDeviceContacts(query)
            if (deviceContact != null) {
                launchDialer(deviceContact.second)
                return@withContext ActionResult.Success("Found ${deviceContact.first} in contacts ($deviceContact.second). Opening dialer...")
            }
        }

        // 3. Not found, request number
        return@withContext ActionResult.RequiresInput(
            "$query ka phone number kya hai? Enter the phone number to save and dial:",
            query
        )
    }

    private fun searchDeviceContacts(query: String): Pair<String, String>? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    return Pair(name, number)
                }
            }
            null
        } catch (e: Exception) {
            Log.e("ActionEngine", "Error querying contacts: ${e.message}")
            null
        }
    }

    fun launchDialer(phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ActionEngine", "Dialer error", e)
        }
    }

    fun toggleFlashlight(enable: Boolean): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                return ActionResult.Failure("Device camera flash is not available.")
            }

            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            ActionResult.Success(if (enable) "Flashlight turned ON 💡" else "Flashlight turned OFF 🌑")
        } catch (e: CameraAccessException) {
            ActionResult.Failure("Flashlight access error: ${e.message}")
        } catch (e: Exception) {
            ActionResult.Failure("Flashlight error: ${e.message}")
        }
    }

    fun openYouTube(searchQuery: String? = null): ActionResult {
        return try {
            val url = if (!searchQuery.isNullOrBlank()) {
                val encoded = URLEncoder.encode(searchQuery, "UTF-8")
                "https://www.youtube.com/results?search_query=$encoded"
            } else {
                "https://www.youtube.com"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                // Try YouTube package if present
                `package` = "com.google.android.youtube"
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to browser
                intent.`package` = null
                context.startActivity(intent)
            }
            ActionResult.Success(if (searchQuery.isNullOrBlank()) "Opening YouTube..." else "Searching YouTube for '$searchQuery'...")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open YouTube: ${e.message}")
        }
    }

    fun openChrome(queryOrUrl: String? = null): ActionResult {
        return try {
            val intent = if (queryOrUrl.isNullOrBlank()) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            } else if (queryOrUrl.startsWith("http://") || queryOrUrl.startsWith("https://")) {
                Intent(Intent.ACTION_VIEW, Uri.parse(queryOrUrl)).apply {
                    `package` = "com.android.chrome"
                }
            } else {
                val encoded = URLEncoder.encode(queryOrUrl, "UTF-8")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                    `package` = "com.android.chrome"
                }
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                intent.`package` = null
                context.startActivity(intent)
            }
            ActionResult.Success(if (queryOrUrl.isNullOrBlank()) "Opening Chrome..." else "Searching '$queryOrUrl'...")
        } catch (e: Exception) {
            ActionResult.Failure("Failed to open Chrome: ${e.message}")
        }
    }

    fun openInstagram(reels: Boolean = false): ActionResult {
        return try {
            val url = if (reels) "https://www.instagram.com/reels/" else "https://www.instagram.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                `package` = "com.instagram.android"
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                intent.`package` = null
                context.startActivity(intent)
            }
            ActionResult.Success(if (reels) "Opening Instagram Reels..." else "Opening Instagram...")
        } catch (e: Exception) {
            ActionResult.Failure("Could not open Instagram: ${e.message}")
        }
    }

    fun openCamera(): ActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening Camera 📷...")
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open camera: ${e.message}")
        }
    }

    fun openWifiSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening Wi-Fi settings...")
        } catch (e: Exception) {
            ActionResult.Failure("Could not open Wi-Fi settings: ${e.message}")
        }
    }

    fun openDataSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening Mobile Data settings...")
        } catch (e: Exception) {
            ActionResult.Failure("Could not open Data settings: ${e.message}")
        }
    }

    fun openSecuritySettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionResult.Success("Opening Screen Lock & Security settings...")
        } catch (e: Exception) {
            ActionResult.Failure("Could not open Security settings: ${e.message}")
        }
    }
}
