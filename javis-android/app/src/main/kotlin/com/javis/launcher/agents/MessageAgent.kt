package com.javis.launcher.agents

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactAgent: ContactAgent
) {
    fun draftWhatsApp(recipient: String, message: String): AgentResult {
        val contacts = if (recipient.isNotBlank()) contactAgent.findContacts(recipient) else emptyList()
        val contact = contacts.firstOrNull()

        return try {
            if (contact != null) {
                val phone = contact.phone.replace(Regex("[^0-9+]"), "")
                val waPhone = if (phone.startsWith("+")) phone.removePrefix("+") else phone
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$waPhone?text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    AgentResult(true, "Opened WhatsApp with draft message to ${contact.name}. Tap Send when ready.", mapOf("recipient" to contact.name, "message" to message))
                } catch (_: Exception) {
                    // WhatsApp not installed — try generic
                    openGenericShare(message)
                }
            } else if (recipient.matches(Regex("\\+?[0-9\\s\\-]{7,}"))) {
                val phone = recipient.replace(Regex("[^0-9+]"), "")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AgentResult(true, "Opened WhatsApp draft to $recipient. Tap Send when ready.")
            } else {
                openGenericShare(message)
            }
        } catch (e: Exception) {
            AgentResult(false, "Could not open WhatsApp: ${e.message}")
        }
    }

    fun draftSms(recipient: String, message: String): AgentResult {
        val contacts = if (recipient.isNotBlank()) contactAgent.findContacts(recipient) else emptyList()
        val contact = contacts.firstOrNull()
        val phone = contact?.phone?.replace(Regex("[^0-9+]"), "") ?: recipient

        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val name = contact?.name ?: recipient
            AgentResult(true, "Opened SMS draft to $name. Tap Send when ready.", mapOf("recipient" to name, "message" to message))
        } catch (e: Exception) {
            AgentResult(false, "Could not open SMS: ${e.message}")
        }
    }

    private fun openGenericShare(message: String): AgentResult {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Send message via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            AgentResult(true, "Select a messaging app to send: \"$message\"")
        } catch (e: Exception) {
            AgentResult(false, "Could not open any messaging app.")
        }
    }
}
