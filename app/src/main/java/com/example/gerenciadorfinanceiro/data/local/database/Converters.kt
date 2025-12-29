package com.example.gerenciadorfinanceiro.data.local.database

import androidx.room.TypeConverter
import com.example.gerenciadorfinanceiro.domain.model.*

class Converters {

    // Bank
    @TypeConverter
    fun fromBank(value: Bank): String = value.name

    @TypeConverter
    fun toBank(value: String): Bank = Bank.fromName(value)

    // Category
    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.fromName(value)

    // TransactionType
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    // TransactionStatus
    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus): String = value.name

    @TypeConverter
    fun toTransactionStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)

    // BillStatus
    @TypeConverter
    fun fromBillStatus(value: BillStatus): String = value.name

    @TypeConverter
    fun toBillStatus(value: String): BillStatus = BillStatus.valueOf(value)

    // Frequency
    @TypeConverter
    fun fromFrequency(value: Frequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): Frequency = Frequency.valueOf(value)

    // PaymentMethod
    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}