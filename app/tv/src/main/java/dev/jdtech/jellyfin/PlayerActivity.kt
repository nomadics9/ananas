package org.askartv.phone

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.ActivityDestination
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.scope.resultRecipient
import dagger.hilt.android.AndroidEntryPoint
import org.askartv.phone.destinations.PlayerActivityDestination
import org.askartv.phone.destinations.PlayerScreenDestination
import org.askartv.phone.models.PlayerItem
import org.askartv.phone.ui.PlayerScreen
import org.askartv.phone.ui.theme.FindroidTheme

data class PlayerActivityNavArgs(
    val items: ArrayList<PlayerItem>,
)

@AndroidEntryPoint
@ActivityDestination(
    navArgsDelegate = PlayerActivityNavArgs::class,
)
class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = PlayerActivityDestination.argsFrom(intent)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            FindroidTheme {
                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    startRoute = PlayerScreenDestination,
                ) {
                    composable(PlayerScreenDestination) {
                        PlayerScreen(
                            navigator = destinationsNavigator,
                            items = args.items,
                            resultRecipient = resultRecipient(),
                        )
                    }
                }
            }
        }
    }
}
