package com.javis.launcher.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline AI Engine — handles basic intent recognition and responses
 * without internet access. Uses pattern matching + template responses.
 */
@Singleton
class OfflineAiEngine @Inject constructor() {

    private val greetings = listOf(
        "Hello! I'm running in offline mode, but I can still help with basic tasks.",
        "Hi there! I'm offline right now, but I'm ready to assist with what I can.",
        "Hey! Working offline — I can still launch apps, set alarms, and search contacts."
    )

    private val statusResponses = listOf(
        "Running in offline mode. I can still handle app launching, alarms, and contacts.",
        "All core features are available offline. Only AI conversation requires internet.",
        "Offline and operational. Ready for your commands."
    )

    fun chat(messages: List<AiMessage>): Result<String> {
        val lastUserMessage = messages.lastOrNull { it.role == "user" }?.content?.lowercase() ?: ""

        val response = when {
            isGreeting(lastUserMessage) -> greetings.random()
            isStatusCheck(lastUserMessage) -> statusResponses.random()
            isHelpRequest(lastUserMessage) -> buildHelpResponse()
            isJoke(lastUserMessage) -> offlineJokes.random()
            isThanks(lastUserMessage) -> thankResponses.random()
            else -> buildOfflineFallback(lastUserMessage)
        }

        return Result.success(response)
    }

    private fun isGreeting(msg: String) =
        msg.contains(Regex("\\b(hi|hello|hey|good morning|good afternoon|good evening|howdy)\\b"))

    private fun isStatusCheck(msg: String) =
        msg.contains(Regex("\\b(how are you|status|online|offline|working)\\b"))

    private fun isHelpRequest(msg: String) =
        msg.contains(Regex("\\b(help|what can you do|commands|features)\\b"))

    private fun isJoke(msg: String) =
        msg.contains(Regex("\\b(joke|funny|laugh|humor)\\b"))

    private fun isThanks(msg: String) =
        msg.contains(Regex("\\b(thanks|thank you|thx|cheers|appreciate)\\b"))

    private fun buildHelpResponse() = """
        In offline mode, I can help you with:
        • Launch any app — just say "Open [app name]"
        • Call contacts — "Call [name]"
        • Set alarms — "Set alarm for 7 AM"
        • Create reminders — "Remind me to [task] at [time]"
        • Search contacts — "Find [name]"
        • Read notifications — "What did I miss?"
        
        For full AI conversation, please connect to the internet.
    """.trimIndent()

    private fun buildOfflineFallback(msg: String) =
        "I'm currently offline. I understood your request but need an internet connection for full AI responses. " +
        "I can still launch apps, make calls, and set alarms — just ask!"

    private val offlineJokes = listOf(
        "Why don't scientists trust atoms? Because they make up everything.",
        "I told my phone to remind me to exercise. It laughed. Now we're both broken.",
        "Why did the computer go to the doctor? It had a virus. Classic.",
        "I'm an AI making an offline joke. The irony is not lost on me."
    )

    private val thankResponses = listOf(
        "Always at your service.",
        "Happy to help.",
        "Anytime.",
        "That's what I'm here for."
    )
}
