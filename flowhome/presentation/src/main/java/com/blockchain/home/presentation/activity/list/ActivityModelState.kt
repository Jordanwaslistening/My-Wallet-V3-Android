package com.blockchain.home.presentation.activity.list

import com.blockchain.commonarch.presentation.mvi_v2.ModelState
import com.blockchain.data.DataResource
import com.blockchain.home.presentation.SectionSize

data class ActivityModelState<ACTIVITY_MODEL>(
    val activityItems: DataResource<Activity<ACTIVITY_MODEL>> = DataResource.Loading,
    val sectionSize: SectionSize = SectionSize.All,
    val filterTerm: String = ""
) : ModelState

data class Activity<ACTIVITY_MODEL>(
    val items: List<ACTIVITY_MODEL>
)
