package com.ecm.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecm.inventory.ui.EcmNavHost
import com.ecm.inventory.ui.EcmViewModel
import com.ecm.inventory.ui.theme.EcmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as EcmApp

        setContent {
            EcmTheme {
                val vm: EcmViewModel = viewModel(factory = EcmViewModel.factory(app))
                EcmNavHost(vm = vm)
            }
        }
    }
}
