package com.hassn.app.ui.challenges

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hassn.app.data.ChallengeType
import com.hassn.app.data.Difficulty
import com.hassn.app.service.ChallengeResult
import com.hassn.app.ui.theme.HassnTheme

/**
 * Challenge host for the disable-protection flow (launched from the app UI,
 * which is foreground so no background-activity-start restriction applies).
 * The distraction-response flow uses the same ChallengeContent as an overlay.
 */
class ChallengeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = runCatching {
            ChallengeType.valueOf(intent?.getStringExtra(EXTRA_TYPE) ?: "")
        }.getOrNull()
        if (type == null) {
            finish()
            return
        }
        val difficulty = runCatching {
            Difficulty.valueOf(intent?.getStringExtra(EXTRA_DIFFICULTY) ?: "MEDIUM")
        }.getOrDefault(Difficulty.MEDIUM)

        setContent {
            HassnTheme {
                ChallengeContent(
                    type = type,
                    difficulty = difficulty,
                    onResult = { result ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_SUCCESS, result == ChallengeResult.SUCCESS)
                        )
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TYPE = "challenge_type"
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_SUCCESS = "success"
    }
}
