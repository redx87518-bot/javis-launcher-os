package com.javis.launcher.agents

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContactInfo(
    val name: String,
    val phone: String,
    val photoUri: String? = null
)

@Singleton
class ContactAgent @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun search(name: String): AgentResult {
        if (name.isBlank()) return AgentResult(false, "Please provide a name to search.")

        val contacts = findContacts(name)
        return if (contacts.isNotEmpty()) {
            AgentResult(true, "Found ${contacts.size} contact(s) named \"$name\".", mapOf("contacts" to contacts))
        } else {
            AgentResult(false, "No contacts found with name \"$name\".")
        }
    }

    fun call(name: String): AgentResult {
        if (name.isBlank()) return AgentResult(false, "Who should I call?")

        val contacts = findContacts(name)
        val contact = contacts.firstOrNull()
            ?: return AgentResult(false, "I couldn't find a contact named \"$name\".")

        return try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phone}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AgentResult(true, "Calling ${contact.name}.")
        } catch (e: Exception) {
            AgentResult(false, "Couldn't place the call: ${e.message}")
        }
    }

    fun findContacts(name: String): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        val cr: ContentResolver = context.contentResolver
        val cursor: Cursor? = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (it.moveToNext()) {
                contacts.add(
                    ContactInfo(
                        name = it.getString(nameIdx) ?: "",
                        phone = it.getString(numIdx) ?: "",
                        photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null
                    )
                )
            }
        }
        return contacts
    }

    fun getAllContacts(): List<ContactInfo> = findContacts("")
}
