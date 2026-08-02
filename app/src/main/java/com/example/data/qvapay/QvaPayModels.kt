package com.example.data.qvapay

data class QvaPayUserInfo(
    val name: String,
    val username: String,
    val email: String,
    val balance: Double,
    val logo: String? = null,
    val bio: String? = null
)

data class QvaPayCoin(
    val name: String,
    val coin: String,
    val feePercent: Double,
    val min: Double,
    val max: Double,
    val logo: String? = null
)

data class QvaPayTransferResponse(
    val success: Boolean,
    val message: String,
    val transactionId: String? = null,
    val amount: Double = 0.0
)

data class QvaPayTransaction(
    val id: String,
    val type: String, // "Paid", "Received", "Withdraw", "Deposit"
    val amount: Double,
    val description: String,
    val remoteUser: String,
    val status: String, // "Completed", "Pending", "Cancelled"
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = ""
)

data class QvaPayInvoice(
    val invoiceId: String,
    val amount: Double,
    val description: String,
    val url: String,
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)
