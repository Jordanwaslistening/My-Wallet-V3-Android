package com.blockchain.nabu.metadata

import com.blockchain.featureflag.FeatureFlag
import com.blockchain.metadata.MetadataEntry
import com.blockchain.metadata.MetadataRepository
import com.blockchain.metadata.load
import com.blockchain.metadata.save
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.tokenresponse.mapToBlockchainCredentialsMetadata
import com.blockchain.nabu.models.responses.tokenresponse.mapToLegacyCredentials
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.utils.extensions.thenMaybe
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import timber.log.Timber

class AccountCredentialsMetadata(
    private val metadataRepository: MetadataRepository,
    private val accountMetadataMigrationFF: FeatureFlag
) {
    private var loadMetadataMaybe: Maybe<CredentialMetadata>? = null

    fun load(): Maybe<CredentialMetadata> {
        loadMetadataMaybe?.let {
            Timber.d("Metadata loading already")
            return it
        } ?: kotlin.run {
            return loadCached().doFinally {
                loadMetadataMaybe = null
            }.also {
                loadMetadataMaybe = it
            }
        }
    }

    private fun loadCached(): Maybe<CredentialMetadata> =
        metadataRepository.load<BlockchainAccountCredentialsMetadata>(
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        ).switchIfEmpty(
            // Not found - haven't been created yet.
            Maybe.just(BlockchainAccountCredentialsMetadata.invalid())
        ).flatMap { blockchainMetadata ->
            if (blockchainMetadata.isValid()) {
                Maybe.just(blockchainMetadata)
            } else {
                metadataRepository.load<NabuLegacyCredentialsMetadata>(
                    MetadataEntry.NABU_LEGACY_CREDENTIALS
                ).flatMap { legacyMetadata ->
                    migrate(legacyMetadata, blockchainMetadata).thenMaybe {
                        Maybe.just(legacyMetadata)
                    }
                }
            }
        }.cache()

    fun saveAndReturn(tokenResponse: NabuOfflineTokenResponse): Single<CredentialMetadata> {
        return accountMetadataMigrationFF.enabled.flatMap { enabled ->
            if (enabled) {
                saveAndReturnMetadata(tokenResponse)
            } else {
                saveAndReturnLegacyMetadata(tokenResponse)
            }
        }
    }

    private fun saveAndReturnMetadata(tokenResponse: NabuOfflineTokenResponse): Single<CredentialMetadata> {
        val metadata = tokenResponse.mapToBlockchainCredentialsMetadata()
        return metadataRepository.save(
            metadata,
            MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
        ).thenSingle {
            Single.just(metadata)
        }
    }

    private fun saveAndReturnLegacyMetadata(tokenResponse: NabuOfflineTokenResponse): Single<CredentialMetadata> {
        val metadata = tokenResponse.mapToLegacyCredentials()
        return metadataRepository.save(
            metadata,
            MetadataEntry.NABU_LEGACY_CREDENTIALS
        ).thenSingle {
            Single.just(metadata)
        }
    }

    private var metadataMigrationCompletable: Completable? = null

    private fun migrate(
        legacyMetadata: NabuLegacyCredentialsMetadata,
        blockchainMetadata: BlockchainAccountCredentialsMetadata
    ): Completable {
        return if (metadataMigrationCompletable == null) {
            metadataMigrationCompletable = metadataRepository.save(
                BlockchainAccountCredentialsMetadata(
                    userId = legacyMetadata.userId,
                    exchangeLifetimeToken = blockchainMetadata.exchangeLifetimeToken?.takeIf { it.isNotEmpty() },
                    lifetimeToken = legacyMetadata.lifetimeToken,
                    exchangeUserId = blockchainMetadata.exchangeUserId?.takeIf { it.isNotEmpty() }
                ),
                MetadataEntry.BLOCKCHAIN_UNIFIED_CREDENTIALS
            ).doFinally {
                metadataMigrationCompletable = null
            }.cache()
            metadataMigrationCompletable!!
        } else {
            metadataMigrationCompletable!!
        }
    }
}
