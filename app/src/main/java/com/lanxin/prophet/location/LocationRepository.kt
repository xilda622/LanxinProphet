package com.lanxin.prophet.location

import com.lanxin.prophet.model.LocationSnapshot

interface LocationRepository {
    suspend fun getCurrentLocationOrNull(): LocationSnapshot?
}
