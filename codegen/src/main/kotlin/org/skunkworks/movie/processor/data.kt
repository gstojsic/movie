package org.skunkworks.movie.processor

import com.squareup.kotlinpoet.TypeName
import kotlinx.metadata.KmValueParameter

data class ParameterData(val kmData: KmValueParameter, val typeName: TypeName)