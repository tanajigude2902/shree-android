package com.shree.ai.core.system

enum class ActionRiskLevel { SAFE, MODERATE, HIGH_RISK }

object ActionRiskClassifier {
    private val highRisk = listOf("delete", "remove", "wipe", "rm -rf", "pay", "transfer")
    private val moderateRisk = listOf("send", "post", "update", "modify", "call", "sms")

    fun classify(action: String): ActionRiskLevel {
        val lower = action.lowercase()
        return when {
            highRisk.any { lower.contains(it) } -> ActionRiskLevel.HIGH_RISK
            moderateRisk.any { lower.contains(it) } -> ActionRiskLevel.MODERATE
            else -> ActionRiskLevel.SAFE
        }
    }
}
