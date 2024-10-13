package com.paytondeveloper.checkintest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class OBUser(
    val id: String,
    val username: String,
    val email: String? = null,
    val familyIDs: List<String>,
    val apnsToken: String? = null
)

@Serializable
data class CIFamily(
    val id: String,
    val name: String,
    val users: List<OBUser>,
    val currentSession: CISession? = null
)

@Serializable
data class CISession(
    var id: String,
    val host: OBUser,
    val latitude: Float,
    val longitude: Float,
    val destinationLat: Float,
    val destinationLong: Float,
    @Serializable(with = InstantSerializer::class)
    val lastUpdate: Instant,
    val batteryLevel: Double,
    @Serializable(with = InstantSerializer::class)
    var started: Instant
)

@Serializable
data class NewSession(
    val token: String,
    val user: OBUser
)

@Serializable
data class UserSignup(
    val email: String,
    val username: String,
    val password: String
)



object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeDouble(value.toEpochMilliseconds().toDouble())

    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}