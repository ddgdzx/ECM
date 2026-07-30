package com.ecm.inventory

import android.app.Application
import com.ecm.inventory.data.EcmDatabase
import com.ecm.inventory.data.EcmRepository

class EcmApp : Application() {

    val repository: EcmRepository by lazy { EcmRepository(EcmDatabase.get(this)) }
}
