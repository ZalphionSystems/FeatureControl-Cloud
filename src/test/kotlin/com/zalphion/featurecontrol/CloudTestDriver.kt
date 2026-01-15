package com.zalphion.featurecontrol

import com.zalphion.featurecontrol.configs.DynamoProperties
import com.zalphion.featurecontrol.configs.DynamoValues
import com.zalphion.featurecontrol.features.DynamoFeature
import com.zalphion.featurecontrol.members.DynamoMember
import com.zalphion.featurecontrol.applications.DynamoApplication
import com.zalphion.featurecontrol.teams.DynamoTeam
import com.zalphion.featurecontrol.users.DynamoUser
import dev.forkhandles.result4k.kotest.shouldBeSuccess
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.KeyType
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.ProjectionType
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.asAttributeDefinition
import org.http4k.format.ConfigurableMoshi

fun fakeCoreStorage(json: ConfigurableMoshi): CoreStorage {
    val dynamo = FakeDynamoDb().client()
    return CoreStorage.dynamoDb(
        json = json,
        dynamoDb = dynamo,
        applicationsTableName = dynamo
            .tableMapper(TableName.of("applications"), DynamoApplication.primaryIndex(json))
            .createTable(DynamoApplication.lookupIndex(json)).shouldBeSuccess()
            .TableDescription.TableName!!,
        featuresTableName = dynamo
            .tableMapper(TableName.of("features"), DynamoFeature.primaryIndex(json))
            .createTable().shouldBeSuccess()
            .TableDescription.TableName!!,
        apiKeysTableName = dynamo.createTable(
            TableName = TableName.parse("apiKeys"),
            KeySchema = listOf(
                KeySchema(hashedApiKeyAttr.name, KeyType.HASH)
            ),
            AttributeDefinitions = listOf(
                appIdAttr.asAttributeDefinition(),
                environmentNameAttr.asAttributeDefinition(),
                hashedApiKeyAttr.asAttributeDefinition()
            ),
            GlobalSecondaryIndexes = listOf(
                GlobalSecondaryIndex(
                    IndexName = IndexName.parse("lookup"),
                    KeySchema = listOf(
                        KeySchema(appIdAttr.name, KeyType.HASH),
                        KeySchema(environmentNameAttr.name, KeyType.RANGE)
                    ),
                    Projection = Projection(
                        ProjectionType = ProjectionType.INCLUDE,
                        NonKeyAttributes = listOf(encryptedApiKeyAttr.name)
                    )
                )
            )
        )
            .shouldBeSuccess()
            .TableDescription.TableName!!,
        usersTableName = dynamo
            .tableMapper(TableName.of("users"), DynamoUser.primaryIndex(json))
            .createTable(DynamoUser.emailsIndex(json))
            .shouldBeSuccess()
            .TableDescription.TableName!!,
        membersTableName = dynamo
            .tableMapper(TableName.of("members"), DynamoMember.primaryIndex(json))
            .createTable(DynamoMember.teamIndex(json))
            .shouldBeSuccess()
            .TableDescription.TableName!!,
        teamsTableName = dynamo
            .tableMapper(TableName.of("teams"), DynamoTeam.primaryIndex(json))
            .createTable().shouldBeSuccess()
            .TableDescription.TableName!!,
        configPropertiesTableName = dynamo
            .tableMapper(TableName.of("properties"), DynamoProperties.primarySchema(json))
            .createTable().shouldBeSuccess()
            .TableDescription.TableName!!,
        configValuesTableName = dynamo
            .tableMapper(TableName.of("values"), DynamoValues.primarySchema(json))
            .createTable().shouldBeSuccess()
            .TableDescription.TableName!!
    )
}