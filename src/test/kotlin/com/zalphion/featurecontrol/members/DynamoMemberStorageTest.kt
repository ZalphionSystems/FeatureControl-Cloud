package com.zalphion.featurecontrol.members

import com.zalphion.featurecontrol.fakeCoreStorage

class DynamoMemberStorageTest: MemberStorageContract(::fakeCoreStorage)