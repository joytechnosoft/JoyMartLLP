package com.joymart.llp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.joymart.llp.app.JoyMartApp
import com.joymart.llp.data.ApiFactory
import com.joymart.llp.data.TokenStore
import com.joymart.llp.ui.theme.JoyMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val tokens = TokenStore(this)
        val api = ApiFactory.create(this, tokens)
        setContent { JoyMartTheme { JoyMartApp(api, tokens) } }
    }
}
