package com.example.gerenciadorfinanceiro.di

import com.example.gerenciadorfinanceiro.domain.notification.GoogleWalletNotificationParser
import com.example.gerenciadorfinanceiro.domain.notification.ItauNotificationParser
import com.example.gerenciadorfinanceiro.domain.notification.NotificationParser
import com.example.gerenciadorfinanceiro.domain.notification.NubankNotificationParser
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ParserModule {

    @Binds
    @IntoSet
    abstract fun bindItauParser(parser: ItauNotificationParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindNubankParser(parser: NubankNotificationParser): NotificationParser

    @Binds
    @IntoSet
    abstract fun bindGoogleWalletParser(parser: GoogleWalletNotificationParser): NotificationParser
}
