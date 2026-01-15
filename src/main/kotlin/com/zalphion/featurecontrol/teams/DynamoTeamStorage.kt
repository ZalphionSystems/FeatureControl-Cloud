package com.zalphion.featurecontrol.teams

import com.zalphion.featurecontrol.teamIdAttr
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.plusAssign
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.autoDynamoLens
import se.ansman.kotshi.JsonSerializable

fun TeamStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    tableName: TableName,
    json: ConfigurableMoshi
) = object : TeamStorage {
    private val table = dynamoDb.tableMapper(tableName, DynamoTeam.primaryIndex(json))

    override fun get(teamId: TeamId) = table[teamId]?.toModel()

    override fun batchGet(ids: Collection<TeamId>) =
        table.batchGet(ids.map { it to null }).map { it.toModel() }.toList()

    override fun plusAssign(team: Team) {
        table += DynamoTeam(
            teamId = team.teamId,
            teamName = team.teamName
        )
    }

    override fun minusAssign(team: Team) = table.delete(team.teamId)
}

@JsonSerializable
data class DynamoTeam(
    val teamId: TeamId,
    val teamName: TeamName
) {
    companion object {
        fun primaryIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary<DynamoTeam, TeamId, Unit>(
            hashKeyAttribute = teamIdAttr,
            sortKeyAttribute = null,
            lens = json.autoDynamoLens()
        )
    }
}

private fun DynamoTeam.toModel() = Team(
    teamId = teamId,
    teamName = teamName
)