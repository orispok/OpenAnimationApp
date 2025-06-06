package com.osg.openanimation.repo

import com.osg.openanimation.core.data.animation.AnimationMetadata
import com.osg.openanimation.core.data.stats.AnimationStats
import com.osg.openanimation.core.ui.di.AnimationMetadataRepository
import com.osg.openanimation.core.ui.di.data.FilterQueryType
import com.osg.openanimation.core.ui.di.data.GuestQueryType
import com.osg.openanimation.core.ui.di.data.SelectedQueryType
import com.osg.openanimation.core.ui.home.model.filterSortByText
import com.osg.openanimation.core.utils.extractSortedTags
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AnimationMetadataRepositoryFake(
    private val networkSimulateDelay: Duration = 1.seconds,
) : AnimationMetadataRepository {
    private fun fetchTradingAnimationIds(): Set<String> {
        val statsMap = FakeRepositoryState.statsState.value
        return statsMap.entries.sortedByDescending {it.value.likeCount + it.value.downloadCount }
            .map { it.key }
            .toSet()
    }

    override suspend fun fetchMetaByHash(hash: String): AnimationMetadata {
        return AnimationDataCollection.entries.first {
            it.metadata.hash == hash
        }.metadata
    }

    override suspend fun fetchRelatedAnimations(
        animationMetadata: AnimationMetadata,
        count: Int
    ): List<AnimationMetadata> {
        delay(networkSimulateDelay)
        return AnimationDataCollection.metadataList
            .sortedByDescending {
                if (it.hash == animationMetadata.hash) {
                    0
                } else {
                    it.tags.intersect(animationMetadata.tags).size
                }
            }.take(count)
    }

    override fun animationStatsFlow(hash: String): Flow<AnimationStats> = FakeRepositoryState.statsState.map {
        it[hash] ?: AnimationStats()
    }

    override suspend fun fetchBy(
        queryType: GuestQueryType,
        limit: Int
    ): List<AnimationMetadata> {
        delay(networkSimulateDelay)
        return when(queryType){
            is FilterQueryType -> {
                AnimationDataCollection.entries
                    .map { it.metadata }
                    .filterSortByText(queryType)
            }
            is SelectedQueryType.ExploreCategory.Explore ->{
                AnimationDataCollection.metadataList
            }

            SelectedQueryType.ExploreCategory.Trending -> {
                val trendingIds = fetchTradingAnimationIds()
                return trendingIds.map {
                    AnimationDataCollection.findByHash(it).metadata
                }
            }
        }

    }

    override suspend fun fetchTags(): List<String> {
        return AnimationDataCollection.entries.map { it.metadata }.extractSortedTags()
    }
}

