package com.iseeu.app.data.repository

import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.iseeu.app.data.remote.FirestorePaths
import com.iseeu.app.util.AvatarColorPalette
import com.iseeu.app.util.FamilyCodeGenerator
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private class CodeCollisionException : Exception()

@Singleton
class FamilyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : FamilyRepository {

    override suspend fun createFamily(displayName: String): CreateFamilyResult {
        val uid = try {
            authRepository.ensureSignedIn()
        } catch (e: IOException) {
            return CreateFamilyResult.Offline
        } catch (e: FirebaseException) {
            return CreateFamilyResult.Failed
        }

        repeat(MAX_ATTEMPTS) {
            val code = FamilyCodeGenerator.next()
            try {
                firestore.runTransaction { txn ->
                    val familyRef = FirestorePaths.familyDoc(firestore, code)
                    // Plain get-then-set has a race: two devices could roll the same fresh code and
                    // silently merge two families. The transaction makes the check-and-claim atomic.
                    if (txn.get(familyRef).exists()) throw CodeCollisionException()

                    txn.set(
                        familyRef,
                        mapOf(
                            "createdAt" to FieldValue.serverTimestamp(),
                            "createdBy" to uid,
                        ),
                    )
                    txn.set(
                        FirestorePaths.memberDoc(firestore, code, uid),
                        mapOf(
                            "displayName" to displayName,
                            "avatarColor" to AvatarColorPalette.defaultFor(uid),
                            "isVisible" to true,
                            "profileUpdatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }.await()
                return CreateFamilyResult.Success(code)
            } catch (e: CodeCollisionException) {
                // retry with a freshly rolled code
            } catch (e: FirebaseFirestoreException) {
                return if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    CreateFamilyResult.Offline
                } else {
                    CreateFamilyResult.Failed
                }
            } catch (e: IOException) {
                return CreateFamilyResult.Offline
            } catch (e: FirebaseException) {
                return CreateFamilyResult.Failed
            }
        }
        return CreateFamilyResult.Failed
    }

    override suspend fun joinFamily(code: String, displayName: String): JoinFamilyResult {
        val normalizedCode = FamilyCodeGenerator.normalize(code)

        return try {
            val uid = authRepository.ensureSignedIn()
            val familyRef = FirestorePaths.familyDoc(firestore, normalizedCode)
            val snapshot = familyRef.get().await()
            if (!snapshot.exists()) return JoinFamilyResult.NotFound

            // Own-uid member doc can't collide with anyone else's, so a plain set (no transaction) is safe here.
            FirestorePaths.memberDoc(firestore, normalizedCode, uid).set(
                mapOf(
                    "displayName" to displayName,
                    "avatarColor" to AvatarColorPalette.defaultFor(uid),
                    "isVisible" to true,
                    "profileUpdatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            JoinFamilyResult.Success
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) JoinFamilyResult.Offline else JoinFamilyResult.Failed
        } catch (e: IOException) {
            JoinFamilyResult.Offline
        } catch (e: FirebaseException) {
            JoinFamilyResult.Failed
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
    }
}
