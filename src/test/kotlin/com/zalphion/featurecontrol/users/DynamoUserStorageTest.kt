package com.zalphion.featurecontrol.users

import com.zalphion.featurecontrol.fakeCoreStorage

class DynamoUserStorageTest: UserStorageContract(::fakeCoreStorage)