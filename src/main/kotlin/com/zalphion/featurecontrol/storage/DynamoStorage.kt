package com.zalphion.featurecontrol.storage

import dev.andrewohara.utils.pagination.Page
import dev.andrewohara.utils.pagination.Paginator
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.batchGetItem
import org.http4k.connect.amazon.dynamodb.deleteItem
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.ReqGetItem
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.amazon.dynamodb.query
import org.http4k.lens.BiDiMapping

fun StorageDriver.Companion.dynamoDb(dynamoDb: DynamoDb, pageSize: PageSize? = null) = object: StorageDriver {

    override fun <Doc : Any, GroupId: Any, ItemId: Any> create(
        name: String,
        groupIdMapper: BiDiMapping<String, GroupId>,
        itemIdMapper: BiDiMapping<String, ItemId>,
        documentMapper: BiDiMapping<String, Doc>
    ) = dynamoRepository(
        dynamoDb = dynamoDb,
        tableName = TableName.parse(name),
        documentMapper = documentMapper,
        groupIdMapper = groupIdMapper,
        itemIdMapper = itemIdMapper,
        pageSize = pageSize?.value
    )
}

private val inverseIndexName = IndexName.parse("inverse")

 private fun <Doc: Any, GroupId: Any, ItemId: Any> dynamoRepository(
    dynamoDb: DynamoDb,
    tableName: TableName,
    documentMapper: BiDiMapping<String, Doc>,
    groupIdMapper: BiDiMapping<String, GroupId>,
    itemIdMapper: BiDiMapping<String, ItemId>,
    pageSize: Int?
) = object: Repository<Doc, GroupId, ItemId> {

    private val groupIdAttr = Attribute.string().map(groupIdMapper).required("groupId")
    private val itemIdAttr = Attribute.string().map(itemIdMapper).required("itemId")
    private val docAttr = Attribute.string().map(documentMapper).required("document")

     override fun save(groupId: GroupId, itemId: ItemId, doc: Doc) = dynamoDb
        .putItem(tableName, Item().with(
            groupIdAttr of groupId,
            itemIdAttr of itemId,
            docAttr of doc)
        )
        .map {}
        .onFailure { it.reason.throwIt() }

     override fun delete(groupId: GroupId, itemId: ItemId) = dynamoDb
        .deleteItem(tableName, Key(groupIdAttr of groupId, itemIdAttr of itemId))
        .map { }
        .onFailure { it.reason.throwIt() }

     override fun get(groupId: GroupId, itemId: ItemId) = dynamoDb
        .getItem(tableName, Key(groupIdAttr of groupId, itemIdAttr of itemId))
        .onFailure { it.reason.throwIt() }
        .item?.let(docAttr)

    override fun list(group: GroupId) = Paginator<Doc, ItemId> { cursor ->
        val page = dynamoDb.query(
            TableName = tableName,
            KeyConditionExpression = "#group = :group",
            ExpressionAttributeNames = mapOf("#group" to groupIdAttr.name),
            ExpressionAttributeValues = mapOf(":group" to groupIdAttr.asValue(group)),
            Limit = pageSize,
            ExclusiveStartKey = cursor?.let { Key(groupIdAttr of group, itemIdAttr of it) }
        ).onFailure { it.reason.throwIt() }

        Page(
            items = page.items.map(docAttr),
            next = page.LastEvaluatedKey?.let(itemIdAttr)
        )
    }

    override fun listInverse(itemId: ItemId) = Paginator<Doc, GroupId> { cursor ->
        val page = dynamoDb.query(
            TableName = tableName,
            IndexName = inverseIndexName,
            KeyConditionExpression = "#itemId = :itemId",
            ExpressionAttributeNames = mapOf("#itemId" to itemIdAttr.name),
            ExpressionAttributeValues = mapOf(":itemId" to itemIdAttr.asValue(itemId)),
            Limit = pageSize,
            ExclusiveStartKey = cursor?.let { Key(groupIdAttr of cursor, itemIdAttr of itemId) }
        ).onFailure { it.reason.throwIt() }

        Page(
            items = page.items.map(docAttr),
            next = page.LastEvaluatedKey?.let(groupIdAttr)
        )
    }

     override fun get(ids: Collection<Pair<GroupId, ItemId>>) = dynamoDb
        .batchGetItem(mapOf(
            tableName to ReqGetItem.Get(
                Keys = ids.map { Key(groupIdAttr of it.first, itemIdAttr of it.second) }
            )
        ))
        .onFailure { it.reason.throwIt() }
        .Responses?.get(tableName.value).orEmpty()
        .map(docAttr)
}