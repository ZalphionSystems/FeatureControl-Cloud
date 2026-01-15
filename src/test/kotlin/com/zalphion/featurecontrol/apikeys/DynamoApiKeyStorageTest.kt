package com.zalphion.featurecontrol.apikeys

import com.zalphion.featurecontrol.fakeCoreStorage

class DynamoApiKeyStorageTest: ApiKeyStorageContract(::fakeCoreStorage)