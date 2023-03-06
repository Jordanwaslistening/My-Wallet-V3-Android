package com.blockchain.home.announcements

import com.blockchain.data.DataResource
import com.blockchain.data.FreshnessStrategy
import com.blockchain.data.RefreshStrategy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

interface AnnouncementsService {
    fun announcements(
        freshnessStrategy: FreshnessStrategy = FreshnessStrategy.Cached(
            RefreshStrategy.RefreshIfOlderThan(15, TimeUnit.MINUTES)
        )
    ): Flow<DataResource<List<Announcement>>>
}
