package com.zalphion.featurecontrol.apikeys

import com.zalphion.featurecontrol.crypto.Base64String
import com.zalphion.featurecontrol.encryptedApiKeyAttr
import com.zalphion.featurecontrol.auth.EnginePrincipal
import com.zalphion.featurecontrol.environmentNameAttr
import com.zalphion.featurecontrol.hashedApiKeyAttr
import com.zalphion.featurecontrol.appIdAttr
import dev.forkhandles.result4k.onFailure
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.query

// Uses document-api with projections to minimize transfer of sensitive data
fun ApiKeyStorage.Companion.dynamoDb(
    dynamoDb: DynamoDb,
    tableName: TableName
) = object: ApiKeyStorage {

    override fun get(hashedApiKey: Base64String) = dynamoDb.getItem(
        TableName = tableName,
        Key = Key(hashedApiKeyAttr of hashedApiKey),
        ProjectionExpression = "$appIdAttr, $environmentNameAttr"
    ).onFailure { it.reason.throwIt() }.item?.let {
        EnginePrincipal(
            appId = appIdAttr(it),
            environmentName = environmentNameAttr(it)
        )
    }

    override fun get(enginePrincipal: EnginePrincipal) = dynamoDb.query(
        TableName = tableName,
        IndexName = IndexName.of("lookup"),
        KeyConditionExpression = "$appIdAttr = :$appIdAttr AND $environmentNameAttr = :$environmentNameAttr",
        ExpressionAttributeValues = mapOf(
            ":$appIdAttr" to appIdAttr.asValue(enginePrincipal.appId),
            ":$environmentNameAttr" to environmentNameAttr.asValue(enginePrincipal.environmentName)
        ),
        ProjectionExpression = "$encryptedApiKeyAttr"
    ).onFailure { it.reason.throwIt() }.items
        .firstOrNull()
        ?.let(encryptedApiKeyAttr)

    override fun set(enginePrincipal: EnginePrincipal, pair: ApiKeyPair) {
        dynamoDb.putItem(
            TableName = tableName,
            Item = Item(
                appIdAttr of enginePrincipal.appId,
                environmentNameAttr of enginePrincipal.environmentName,
                encryptedApiKeyAttr of pair.encrypted,
                hashedApiKeyAttr of pair.hashed
            )
        ).onFailure { it.reason.throwIt() }
    }
}