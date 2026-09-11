package com.iseeu.app.di

import com.iseeu.app.data.repository.AuthRepository
import com.iseeu.app.data.repository.AuthRepositoryImpl
import com.iseeu.app.data.repository.FamilyRepository
import com.iseeu.app.data.repository.FamilyRepositoryImpl
import com.iseeu.app.data.repository.MemberRepository
import com.iseeu.app.data.repository.MemberRepositoryImpl
import com.iseeu.app.data.repository.PinRepository
import com.iseeu.app.data.repository.PinRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFamilyRepository(impl: FamilyRepositoryImpl): FamilyRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(impl: MemberRepositoryImpl): MemberRepository

    @Binds
    @Singleton
    abstract fun bindPinRepository(impl: PinRepositoryImpl): PinRepository
}
