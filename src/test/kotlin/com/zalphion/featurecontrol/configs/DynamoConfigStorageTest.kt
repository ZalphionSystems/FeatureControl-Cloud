package com.zalphion.featurecontrol.configs

import com.zalphion.featurecontrol.fakeCoreStorage

class DynamoConfigStorageTest: ConfigStorageContract(::fakeCoreStorage)