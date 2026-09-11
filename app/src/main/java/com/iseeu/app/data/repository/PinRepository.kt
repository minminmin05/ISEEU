package com.iseeu.app.data.repository

import com.iseeu.app.domain.model.Pin
import com.iseeu.app.domain.model.PinType
import kotlinx.coroutines.flow.Flow

interface PinRepository {
    /** Live list of saved places (e.g. "Home", "School") for the family. */
    fun observePins(familyCode: String): Flow<List<Pin>>

    suspend fun addPin(familyCode: String, uid: String, name: String, lat: Double, lng: Double, type: PinType)

    suspend fun deletePin(familyCode: String, pinId: String)
}
