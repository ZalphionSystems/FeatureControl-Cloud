package com.zalphion.featurecontrol.applications

import com.zalphion.featurecontrol.features.EnvironmentName
import com.zalphion.featurecontrol.lib.Colour
import com.zalphion.featurecontrol.appIdAttr
import com.zalphion.featurecontrol.teamIdAttr
import com.zalphion.featurecontrol.teams.TeamId
import dev.andrewohara.utils.pagination.Page
import dev.andrewohara.utils.pagination.Paginator
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.autoDynamoLens
import se.ansman.kotshi.JsonSerializable

fun ApplicationStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    applicationsTableName: TableName,
    json: ConfigurableMoshi
) = object : ApplicationStorage {

    private val applications = dynamoDb.tableMapper(applicationsTableName, DynamoApplication.primaryIndex(json))
    private val lookup = applications.index(DynamoApplication.lookupIndex(json))

    override fun list(teamId: TeamId, pageSize: Int) = Paginator<Application, AppId> { cursor ->
        val page = applications.primaryIndex().scanPage(
            ExclusiveStartKey = cursor?.let { Key(teamIdAttr of teamId, appIdAttr of cursor) },
            Limit = pageSize
        )

        Page(
            items = page.items.map { it.toModel() },
            next = page.lastEvaluatedKey?.let(appIdAttr)
        )
    }

    override fun get(appId: AppId) = lookup.query(appId)
        .firstOrNull()
        ?.toModel()

    override fun plusAssign(application: Application) {
        applications.save(application.toDynamo())
    }

    override fun minusAssign(application: Application) {
        applications.delete(application.teamId, application.appId)
    }
}

@JsonSerializable
internal data class DynamoApplication(
    val teamId: TeamId,
    val appId: AppId,
    val appName: AppName,
    val environments: List<DynamoEnvironment>,
    val extensions: Map<String, String>
) {
    companion object {
        fun primaryIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary<DynamoApplication, TeamId, AppId>(
            hashKeyAttribute = teamIdAttr,
            sortKeyAttribute = appIdAttr,
            lens = json.autoDynamoLens()
        )
        fun lookupIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.GlobalSecondary<DynamoApplication, AppId, Unit>(
            indexName = IndexName.of("lookup"),
            hashKeyAttribute = appIdAttr,
            sortKeyAttribute = null,
            lens = json.autoDynamoLens()
        )
    }
}

private fun DynamoApplication.toModel() = Application(
    teamId = teamId,
    appId = appId,
    appName = appName,
    environments = environments.map { it.toModel() },
    extensions = extensions
)

private fun Application.toDynamo() = DynamoApplication(
    teamId = teamId,
    appId = appId,
    appName = appName,
    environments = environments.map { it.toDynamo() },
    extensions = extensions
)

@JsonSerializable
data class DynamoEnvironment(
    val name: EnvironmentName,
    val description: String,
    val colour: Colour,
    val extensions: Map<String, String>
)

private fun Environment.toDynamo() = DynamoEnvironment(
    name = name,
    description = description,
    colour = colour,
    extensions = extensions
)

private fun DynamoEnvironment.toModel() = Environment(
    name = name,
    description = description,
    colour = colour,
    extensions = extensions
)