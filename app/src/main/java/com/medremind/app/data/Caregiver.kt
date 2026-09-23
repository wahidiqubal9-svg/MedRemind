package com.medremind.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A person whose medicines are tracked on this device.
 *
 * [id] 0 is reserved for the device owner ("Me"), whose display name comes from
 * the Me profile. Rows with id > 0 are other people (e.g. "Mom") a caregiver has
 * added. This keeps the existing single-user data (profileId 0) untouched.
 */
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val relation: String = "",
    val isSelf: Boolean = false,
    val avatarPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

object CaregiverDirection {
    /** This device is the patient; someone else cares for me. */
    const val INCOMING = "INCOMING"
    /** This device is the caregiver; I help someone else. */
    const val OUTGOING = "OUTGOING"
}

object CaregiverStatus {
    const val PENDING = "PENDING"
    const val ACTIVE = "ACTIVE"
    const val REMOVED = "REMOVED"
}

/**
 * A caregiver <-> patient relationship. Stored locally for now; the same shape
 * maps onto a future cloud record.
 */
@Entity(tableName = "caregiver_links")
data class CaregiverLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** The patient profile this link is about (0 = device owner). */
    val patientProfileId: Long,
    val caregiverName: String,
    val direction: String = CaregiverDirection.INCOMING,
    val status: String = CaregiverStatus.PENDING,
    /** Bitmask of [CaregiverPermission] flags. */
    val permissions: Int = CaregiverPermission.DEFAULT_MEDICATION,
    /** Correlates the two sides of one pairing (0 when created directly). */
    val pairingId: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object PairingStatus {
    const val OPEN = "OPEN"
    const val CLAIMED = "CLAIMED"
    const val APPROVED = "APPROVED"
    const val DECLINED = "DECLINED"
    const val EXPIRED = "EXPIRED"
}

/**
 * A short-lived pairing token. The QR code / 6-digit code carry only this token —
 * never medicine data. In this offline build the token is validated locally.
 */
@Entity(tableName = "pairing_requests")
data class PairingRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val token: String,
    val code: String,
    val direction: String = CaregiverDirection.INCOMING,
    val peerName: String = "",
    val expiresAt: Long,
    val status: String = PairingStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
)

object CaregiverActivityType {
    const val REMIND_SENT = "REMIND_SENT"
    const val MEDICINE_ADDED = "MEDICINE_ADDED"
    const val MEDICINE_EDITED = "MEDICINE_EDITED"
    const val MEDICINE_REMOVED = "MEDICINE_REMOVED"
    const val DOSE_CONFIRMED = "DOSE_CONFIRMED"
    const val CONNECTED = "CONNECTED"
    const val REMOVED = "REMOVED"
}

object CaregiverActor {
    const val CAREGIVER = "CAREGIVER"
    const val PATIENT = "PATIENT"
}

/** A human-readable audit entry shown for transparency. */
@Entity(tableName = "caregiver_activity")
data class CaregiverActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val linkId: Long = 0L,
    val patientProfileId: Long = 0L,
    val actor: String = CaregiverActor.CAREGIVER,
    val type: String,
    val medicineName: String = "",
    val message: String = "",
    val at: Long = System.currentTimeMillis()
)
