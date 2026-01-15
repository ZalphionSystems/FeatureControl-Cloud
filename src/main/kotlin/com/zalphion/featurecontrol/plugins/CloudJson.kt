package com.zalphion.featurecontrol.plugins

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
private object CloudJsonAdapterFactory : JsonAdapter.Factory by KotshiCloudJsonAdapterFactory