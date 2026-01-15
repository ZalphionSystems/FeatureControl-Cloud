package com.zalphion.featurecontrol.configs

import com.zalphion.featurecontrol.environmentNameAttr
import com.zalphion.featurecontrol.features.EnvironmentName
import com.zalphion.featurecontrol.appIdAttr
import com.zalphion.featurecontrol.applications.AppId
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.minusAssign
import org.http4k.connect.amazon.dynamodb.mapper.plusAssign
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.autoDynamoLens
import se.ansman.kotshi.JsonSerializable

fun ConfigStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    propertiesTableName: TableName,
    valuesTableName: TableName,
    json: ConfigurableMoshi
) = object: ConfigStorage {
    private val properties = dynamoDb.tableMapper(propertiesTableName, DynamoProperties.primarySchema(json))
    private val values = dynamoDb.tableMapper(valuesTableName, DynamoValues.primarySchema(json))

    override fun get(appId: AppId): ConfigSpec? {
        val item = properties[appId] ?: return null

        return ConfigSpec(
            appId = item.appId,
            properties = item.properties
                .mapKeys { (key, _) -> PropertyKey.parse(key) }
                .mapValues { (_, value) ->
                    Property(
                        description = value.description,
                        group = value.group,
                        type = value.type.toModel()
                    )
                }
        )
    }

    override fun get(appId: AppId, environmentName: EnvironmentName): ConfigEnvironment? {
        val item = values[appId, environmentName] ?: return null

        return ConfigEnvironment(
            appId = item.appId,
            environmentName = item.environmentName,
            values = item.values
                .mapKeys { PropertyKey.parse(it.key) }
                .mapValues { PropertyValue(it.value.type.toModel(), it.value.value) }
        )
    }

    override fun plusAssign(config: ConfigSpec) {
        properties += DynamoProperties(
            appId = config.appId,
            properties = config.properties
                .mapKeys { (key, _) -> key.value }
                .mapValues { (_, value) ->
                    DynamoProperty(
                        description = value.description,
                        group = value.group,
                        type = value.type.toDynamo()
                    )
                }
        )
    }

    override fun plusAssign(environment: ConfigEnvironment) {
        this.values += DynamoValues(
            appId = environment.appId,
            environmentName = environment.environmentName,
            values = environment.values
                .mapKeys { it.key.value }
                .mapValues {  DynamoPropertyValue(it.value.value, it.value.type.toDynamo()) }
        )
    }

    override fun minusAssign(appId: AppId) {
        values.primaryIndex()
            .query(appId)
            .forEach(values::minusAssign)

        properties.delete(appId)
    }

    override fun delete(appId: AppId, environmentName: EnvironmentName) {
        values.delete(appId, environmentName)
    }
}

@JsonSerializable
data class DynamoProperties(
    val appId: AppId,
    val properties: Map<String, DynamoProperty>
) {
    companion object {
        fun primarySchema(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary<DynamoProperties, AppId, Unit>(
            hashKeyAttribute = appIdAttr,
            sortKeyAttribute = null,
            lens = json.autoDynamoLens()
        )
    }
}

@JsonSerializable
data class DynamoProperty(
    val description: String,
    val group: String?,
    val type: DynamoPropertyType
)

@JsonSerializable
enum class DynamoPropertyType { Boolean, Decimal, String, Secret }

private fun DynamoPropertyType.toModel() = when(this) {
    DynamoPropertyType.Boolean -> PropertyType.Boolean
    DynamoPropertyType.Decimal -> PropertyType.Number
    DynamoPropertyType.String -> PropertyType.String
    DynamoPropertyType.Secret -> PropertyType.Secret
}

private fun PropertyType.toDynamo() = when(this) {
    PropertyType.String -> DynamoPropertyType.String
    PropertyType.Boolean -> DynamoPropertyType.Boolean
    PropertyType.Number -> DynamoPropertyType.Decimal
    PropertyType.Secret -> DynamoPropertyType.Secret
}

@JsonSerializable
data class DynamoValues(
    val appId: AppId,
    val environmentName: EnvironmentName,
    val values: Map<String, DynamoPropertyValue>
) {
    companion object {
        fun primarySchema(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary(
            hashKeyAttribute = appIdAttr,
            sortKeyAttribute = environmentNameAttr,
            lens = json.autoDynamoLens<DynamoValues>()
        )
    }
}

@JsonSerializable
data class DynamoPropertyValue(val value: String, val type: DynamoPropertyType)