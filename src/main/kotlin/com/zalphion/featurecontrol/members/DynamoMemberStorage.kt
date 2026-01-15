package com.zalphion.featurecontrol.members

import com.zalphion.featurecontrol.teamIdAttr
import com.zalphion.featurecontrol.teams.TeamId
import com.zalphion.featurecontrol.userIdAttr
import com.zalphion.featurecontrol.users.UserId
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
import java.time.Instant

fun MemberStorage.Companion.dynamoDb(dynamoDb: DynamoDb, tableName: TableName, json: ConfigurableMoshi) = object : MemberStorage {

    private val members = dynamoDb.tableMapper(tableName, DynamoMember.primaryIndex(json))
    private val teams = members.index(DynamoMember.teamIndex(json))

    override fun plusAssign(member: Member) = members.save(DynamoMember(
        teamId = member.teamId,
        userId = member.userId,
        invitedBy = member.invitedBy,
        role = member.role,
        invitationExpiresOn = member.invitationExpiresOn?.epochSecond
    ))

    override fun list(
        userId: UserId, pageSize: Int
    ) = Paginator<Member, TeamId> { cursor ->
        val page = teams.queryPage(
            HashKey = userId,
            ExclusiveStartKey = cursor?.let { Key(userIdAttr of userId, teamIdAttr of it) },
            Limit = pageSize
        )

        Page(
            items = page.items.map { it.toModel() },
            next = page.lastEvaluatedKey?.let(teamIdAttr)
        )
    }

    override fun list(
        teamId: TeamId, pageSize: Int
    ) = Paginator<Member, UserId> { cursor ->
        val page = members.primaryIndex().queryPage(
            HashKey = teamId,
            ExclusiveStartKey = cursor?.let { Key(teamIdAttr of teamId, userIdAttr of it) },
            Limit = pageSize
        )

        Page(
            items = page.items.map { it.toModel() },
            next = page.lastEvaluatedKey?.let(userIdAttr)
        )
    }

    override fun get(teamId: TeamId, userId: UserId) = members[teamId, userId]?.toModel()

    override fun minusAssign(member: Member) = members.delete(member.teamId, member.userId)
}

@JsonSerializable
data class DynamoMember(
    val teamId: TeamId,
    val userId: UserId,
    val invitedBy: UserId?,
    val role: UserRole,
    val invitationExpiresOn: Long?
) {
    companion object {
        fun primaryIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary<DynamoMember, TeamId, UserId>(
            hashKeyAttribute = teamIdAttr,
            sortKeyAttribute = userIdAttr,
            lens = json.autoDynamoLens()
        )
        fun teamIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.GlobalSecondary(
            indexName = IndexName.of("teams"),
            hashKeyAttribute = userIdAttr,
            sortKeyAttribute = teamIdAttr,
            lens = json.autoDynamoLens<DynamoMember>()
        )
    }
}

private fun DynamoMember.toModel() = Member(
    teamId = teamId,
    userId = userId,
    role = role,
    invitedBy = invitedBy,
    invitationExpiresOn = invitationExpiresOn?.let(Instant::ofEpochSecond)
)