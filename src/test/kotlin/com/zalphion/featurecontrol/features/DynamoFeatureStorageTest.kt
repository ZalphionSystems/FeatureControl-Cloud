package com.zalphion.featurecontrol.features

import com.zalphion.featurecontrol.fakeCoreStorage

class DynamoFeatureStorageTest: FeatureStorageContract(::fakeCoreStorage)