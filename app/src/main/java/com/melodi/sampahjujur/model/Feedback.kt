package com.melodi.sampahjujur.model

import com.google.firebase.firestore.PropertyName

/**
 * Data class representing user feedback submitted through Help & Support
 *
 * @property id Unique identifier for the feedback
 * @property userId ID of the user who submitted the feedback
 * @property name Name of the user submitting feedback
 * @property email Email address for contact
 * @property message Feedback message content
 * @property timestamp Time when feedback was submitted (milliseconds since epoch)
 * @property status Status of the feedback (pending, reviewed, resolved)
 */
data class Feedback(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("userId")
    @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("email")
    @set:PropertyName("email")
    var email: String = "",

    @get:PropertyName("message")
    @set:PropertyName("message")
    var message: String = "",

    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Long = 0L,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_REVIEWED = "reviewed"
        const val STATUS_RESOLVED = "resolved"
    }
}
