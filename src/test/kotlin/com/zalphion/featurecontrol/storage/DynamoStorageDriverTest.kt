package com.zalphion.featurecontrol.storage

import dev.forkhandles.result4k.onFailure
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.model.AttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.KeyType
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.ProjectionType
import org.http4k.connect.amazon.dynamodb.model.TableName

private fun createStorage(pageSize: PageSize): StorageDriver {
    val client = FakeDynamoDb().client()

    client.createTable(
        TableName = TableName.parse("test"),
        KeySchema = listOf(
            KeySchema(AttributeName.parse("groupId"), KeyType.HASH),
            KeySchema(AttributeName.parse("itemId"), KeyType.RANGE)
        ),
        AttributeDefinitions = listOf(
            AttributeDefinition(AttributeName.parse("groupId"), DynamoDataType.S),
            AttributeDefinition(AttributeName.parse("itemId"), DynamoDataType.S)
        ),
        GlobalSecondaryIndexes = listOf(
            GlobalSecondaryIndex(
                IndexName = IndexName.parse("inverse"),
                KeySchema = listOf(
                    KeySchema(AttributeName.parse("itemId"), KeyType.HASH),
                    KeySchema(AttributeName.parse("groupId"), KeyType.RANGE),
                ),
                Projection = Projection(ProjectionType = ProjectionType.ALL)
            )
        )
    ).onFailure { it.reason.throwIt() }
    return StorageDriver.dynamoDb(client, pageSize)
}

class DynamoStorageDriverTest: StorageDriverContract({ pageSize -> createStorage(pageSize)})