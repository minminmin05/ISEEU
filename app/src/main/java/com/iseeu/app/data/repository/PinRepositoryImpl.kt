package com.iseeu.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.iseeu.app.data.remote.FirestorePaths
import com.iseeu.app.data.remote.dto.PinDto
import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.PinType
import com.iseeu.app.domain.model.toFirestoreValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PinRepository {

    override fun observePins(familyCode: String): Flow<List<Pin>> = callbackFlow {
        val listener = FirestorePaths.pinsCollection(firestore, familyCode)
            .addSnapshotListener { snapshot, _ ->
                val pins = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val dto = doc.toObject(PinDto::class.java) ?: return@mapNotNull null
                    Pin(
                        id = doc.id,
                        name = dto.name,
                        lat = dto.lat,
                        lng = dto.lng,
                        radiusMeters = dto.radiusMeters.toFloat(),
                        type = PinType.fromFirestoreValue(dto.type),
                    )
                }
                trySend(pins)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addPin(familyCode: String, uid: String, name: String, lat: Double, lng: Double, type: PinType) {
        FirestorePaths.pinsCollection(firestore, familyCode).add(
            mapOf(
                "name" to name,
                "lat" to lat,
                "lng" to lng,
                "radiusMeters" to DEFAULT_RADIUS_METERS,
                "type" to type.toFirestoreValue(),
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun deletePin(familyCode: String, pinId: String) {
        FirestorePaths.pinDoc(firestore, familyCode, pinId).delete().await()
    }

    companion object {
        private const val DEFAULT_RADIUS_METERS = 150.0
    }
}
