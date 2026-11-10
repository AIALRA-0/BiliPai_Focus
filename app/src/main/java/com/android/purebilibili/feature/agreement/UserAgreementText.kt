package com.android.purebilibili.feature.agreement

import androidx.annotation.StringRes
import com.android.purebilibili.R

/** Resource-backed agreement copy so the mandatory first-launch gate follows the selected app language. */
data class UserAgreementSection(
    @StringRes val title: Int,
    @StringRes val bodyLines: List<Int>,
)

const val USER_AGREEMENT_VERSION = 1

object UserAgreementText {
    @StringRes
    val TITLE: Int = R.string.user_agreement_title

    val SECTIONS: List<UserAgreementSection> = listOf(
        UserAgreementSection(
            title = R.string.user_agreement_section_open_source,
            bodyLines = listOf(R.string.user_agreement_open_source_1, R.string.user_agreement_open_source_2),
        ),
        UserAgreementSection(
            title = R.string.user_agreement_section_privacy,
            bodyLines = listOf(
                R.string.user_agreement_privacy_1,
                R.string.user_agreement_privacy_2,
                R.string.user_agreement_privacy_3,
                R.string.user_agreement_privacy_4,
                R.string.user_agreement_privacy_5,
                R.string.user_agreement_privacy_6,
            ),
        ),
        UserAgreementSection(
            title = R.string.user_agreement_section_account_risk,
            bodyLines = listOf(
                R.string.user_agreement_account_risk_1,
                R.string.user_agreement_account_risk_2,
                R.string.user_agreement_account_risk_3,
            ),
        ),
        UserAgreementSection(
            title = R.string.user_agreement_section_user_responsibility,
            bodyLines = listOf(
                R.string.user_agreement_user_responsibility_1,
                R.string.user_agreement_user_responsibility_2,
                R.string.user_agreement_user_responsibility_3,
            ),
        ),
        UserAgreementSection(
            title = R.string.user_agreement_section_changes,
            bodyLines = listOf(R.string.user_agreement_changes_1),
        ),
    )
}
