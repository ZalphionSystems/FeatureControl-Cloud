package com.zalphion.featurecontrol.users

import com.zalphion.featurecontrol.emailAddressAttr
import com.zalphion.featurecontrol.userIdAttr
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.plusAssign
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.autoDynamoLens
import se.ansman.kotshi.JsonSerializable
import java.net.URI

fun UserStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    tableName: TableName,
    json: ConfigurableMoshi
): UserStorage = object : UserStorage {

    private val table = dynamoDb.tableMapper(tableName, DynamoUser.primaryIndex(json))
    private val emailsIndex = DynamoUser.emailsIndex(json)

    override fun get(userId: UserId) = table[userId]?.toModel()

    override fun get(userIds: Collection<UserId>): Collection<User> {
        return table.batchGet(userIds.map { it to null })
            .map { it.toModel() }
            .toList()
    }

    override fun get(emailAddress: EmailAddress): User? {
        return table.index(emailsIndex).query(emailAddress)
            .firstOrNull()
            ?.toModel()
    }

    override fun plusAssign(user: User) {
        table += DynamoUser(
            userId = user.userId,
            emailAddress = user.emailAddress,
            userName = user.userName,
            photoUrl = user.photoUrl?.toString()
        )
    }
}

@JsonSerializable
data class DynamoUser(
    val userId: UserId,
    val emailAddress: EmailAddress,
    val userName: String?,
    val photoUrl: String?
) {
    companion object {
        fun primaryIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.Primary<DynamoUser, UserId, Unit>(
            hashKeyAttribute = userIdAttr,
            sortKeyAttribute = null,
            lens = json.autoDynamoLens()
        )
        fun emailsIndex(json: ConfigurableMoshi) = DynamoDbTableMapperSchema.GlobalSecondary<DynamoUser, EmailAddress, UserId>(
            indexName = IndexName.of("emails"),
            hashKeyAttribute = emailAddressAttr,
            sortKeyAttribute = userIdAttr,
            lens = json.autoDynamoLens()
        )
    }
}

private fun DynamoUser.toModel() = User(
    userId = userId,
    emailAddress = emailAddress,
    userName = userName,
    photoUrl = photoUrl?.let(URI::create)
)