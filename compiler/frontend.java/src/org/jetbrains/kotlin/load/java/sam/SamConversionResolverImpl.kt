/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.java.sam

import org.jetbrains.kotlin.load.java.components.SamConversionResolver
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.SamAdapterDescriptor
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.SimpleType

class SamConversionResolverImpl(val storageManager: StorageManager, val samWithReceiverResolver: SamWithReceiverResolver): SamConversionResolver {
    private val samConstructorForConstructor =
            storageManager.createMemoizedFunction<JavaClassConstructorDescriptor, SamAdapterDescriptor<JavaClassConstructorDescriptor>> { constructor ->
                SingleAbstractMethodUtils.createSamAdapterConstructor(constructor)
            }

    override fun createSamAdapterConstructor(constructor: JavaClassConstructorDescriptor): SamAdapterDescriptor<JavaClassConstructorDescriptor>? {
        if (!SingleAbstractMethodUtils.isSamAdapterNecessary(constructor)) return null
        return samConstructorForConstructor(constructor)
    }

    private val functionTypesForSamInterfaces = storageManager.createCacheWithNullableValues<JavaClassDescriptor, SimpleType>()

    override fun resolveFunctionTypeIfSamInterface(classDescriptor: JavaClassDescriptor): SimpleType? {
        return functionTypesForSamInterfaces.computeIfAbsent(classDescriptor) {
            val abstractMethod = SingleAbstractMethodUtils.getSingleAbstractMethodOrNull(classDescriptor) ?: return@computeIfAbsent null
            val shouldConvertFirstParameterToDescriptor = samWithReceiverResolver.shouldConvertFirstSamParameterToReceiver(abstractMethod)
            SingleAbstractMethodUtils.getFunctionTypeForAbstractMethod(abstractMethod, shouldConvertFirstParameterToDescriptor)
        }
    }
}
