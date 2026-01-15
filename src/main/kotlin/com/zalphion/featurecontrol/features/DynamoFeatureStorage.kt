package com.zalphion.featurecontrol.features

import com.zalphion.featurecontrol.applications.AppId
import com.zalphion.featurecontrol.appIdAttr
import com.zalphion.featurecontrol.featureKeyAttr
import dev.andrewohara.utils.pagination.Page
import dev.andrewohara.utils.pagination.Paginator
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.plusAssign
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.autoDynamoLens
import se.ansman.kotshi.JsonSerializable

fun FeatureStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    tableName: TableName,
    json: ConfigurableMoshi
) = object : FeatureStorage {

    private val table = dynamoDb.tableMapper(tableName, DynamoFeature.primaryIndex(json))

    override fun list(
        appId: AppId,
        pageSize: Int
    ) = Paginator<Feature, FeatureKey> { cursor ->
        val page = table.primaryIndex().queryPage(
            HashKey = appId,
            Limit = pageSize,
            ExclusiveStartKey = cursor?.let {
                Key(appIdAttr of appId, featureKeyAttr of cursor)
            }
        )

        Page(
            items = page.items.map { it.toModel() },
            next = page.lastEvaluatedKey?.let(featureKeyAttr)
        )
    }

    override fun get(appId: AppId, featureKey: FeatureKey) =
        table[appId, featureKey]?.toModel()

    override fun plusAssign(feature: Feature) =
        table.plusAssign(feature.toDynamo())

    override fun minusAssign(feature: Feature) =
        table.delete(feature.appId, feature.key)
}

@JsonSerializable
internal data class DynamoFeature(
    val appId: AppId,
    val featureKey: FeatureKey,
    val variants: List<DynamoVariant>,
    val defaultVariant: Variant,
    val description: String,
    val extensions: Map<String, String>,
    val environments: Map<EnvironmentName, DynamoEnvironment>
) {
    companion object {
        fun primaryIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary(
            hashKeyAttribute = appIdAttr,
            sortKeyAttribute = featureKeyAttr,
            lens = json.autoDynamoLens<DynamoFeature>()
        )
    }
}

@JsonSerializable
internal data class DynamoEnvironment(
    val overrides: Map<SubjectId, Variant>,
    val variants: Map<Variant, Weight>,
    val extensions: Map<String, String>
)

@JsonSerializable
internal data class DynamoVariant(
    val name: Variant,
    val description: String
)

private fun DynamoFeature.toModel() = Feature(
    appId = appId,
    key = featureKey,
    variants = variants.associate { it.name to it.description },
    defaultVariant = defaultVariant,
    description = description,
    extensions = extensions,
    environments = environments.mapValues { (_, env) ->
        FeatureEnvironment(env.variants, env.overrides, env.extensions)
    }
)

private fun Feature.toDynamo() = DynamoFeature(
    appId = appId,
    featureKey = key,
    variants = variants.map { DynamoVariant(it.key, it.value) },
    defaultVariant = defaultVariant,
    description = description,
    extensions = extensions,
    environments = environments.mapValues { (_, env) ->
        DynamoEnvironment(env.overrides, env.weights, env.extensions)
    }
)