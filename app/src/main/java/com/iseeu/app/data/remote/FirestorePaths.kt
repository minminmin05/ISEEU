package com.iseeu.app.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Single source of truth for Firestore collection/document paths, so the schema
 * (families/{code}/members/{uid}/location/current) is only spelled out once.
 */
object FirestorePaths {
    private const val FAMILIES = "families"
    private const val MEMBERS = "members"
    private const val LOCATION = "location"
    private const val LOCATION_DOC = "current"

    fun familyDoc(db: FirebaseFirestore, familyCode: String): DocumentReference =
        db.collection(FAMILIES).document(familyCode)

    fun membersCollection(db: FirebaseFirestore, familyCode: String): CollectionReference =
        familyDoc(db, familyCode).collection(MEMBERS)

    fun memberDoc(db: FirebaseFirestore, familyCode: String, uid: String): DocumentReference =
        membersCollection(db, familyCode).document(uid)

    fun memberLocationDoc(db: FirebaseFirestore, familyCode: String, uid: String): DocumentReference =
        memberDoc(db, familyCode, uid).collection(LOCATION).document(LOCATION_DOC)
}
