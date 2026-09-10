package com.iseeu.app.data.repository

sealed interface CreateFamilyResult {
    data class Success(val code: String) : CreateFamilyResult
    data object Offline : CreateFamilyResult
    data object Failed : CreateFamilyResult
}

sealed interface JoinFamilyResult {
    data object Success : JoinFamilyResult
    data object NotFound : JoinFamilyResult
    data object Offline : JoinFamilyResult
    data object Failed : JoinFamilyResult
}

interface FamilyRepository {
    suspend fun createFamily(displayName: String): CreateFamilyResult
    suspend fun joinFamily(code: String, displayName: String): JoinFamilyResult
}
