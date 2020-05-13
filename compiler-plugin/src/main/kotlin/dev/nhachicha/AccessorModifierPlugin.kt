/*
 * Copyright 2020 Nabil Hachicha.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nhachicha

import com.google.auto.service.AutoService
import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.isPropertyAccessor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext

@AutoService(ComponentRegistrar::class)
class AccessorModifierComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerExtensions(project, configuration)
    }

    companion object {
        fun registerExtensions(project: Project, configuration: CompilerConfiguration) {
            IrGenerationExtension.registerExtension(
                    project,
                    AccessorModifierIrGenerationExtension()
            )
        }
    }
}

class AccessorModifierIrGenerationExtension : IrGenerationExtension {

    override fun generate(
            f: IrFile,
            backendContext: BackendContext,
            bindingContext: BindingContext
    ) {
        AccessorCallTransformer(backendContext).runOnFileInOrder(f)
    }

    class AccessorCallTransformer(
            val context: BackendContext
    ) : IrElementTransformerVoid(), FileLoweringPass {

        private val nameToString = Name.identifier("toString")
        private val nameAppend = Name.identifier("append")
        private val stringBuilder = context.ir.symbols.stringBuilder.owner
        private val prefix = "Hello "

        private val constructor = stringBuilder.constructors.single {
            it.valueParameters.size == 0
        }

        private val toStringFunction = stringBuilder.functions.single {
            it.valueParameters.size == 0 && it.name == nameToString
        }

        private val appendFunction = stringBuilder.functions.single {
            it.name == nameAppend &&
                    it.valueParameters.size == 1 &&
                    it.valueParameters.single().type.isNullableAny()
        }

        override fun lower(irFile: IrFile) {
            irFile.transformChildrenVoid()
        }

        override fun visitFunction(declaration: IrFunction): IrStatement {

            // Modify only property accessor getter for type String or nullable String
            return if (declaration.isPropertyAccessor
                    && declaration.isGetter
                    && (declaration.returnType.isString() || declaration.returnType.isNullableString())
            ) {
                declaration.transformChildrenVoid(this)
                for (statement in declaration.body?.statements!!) {
                    statement.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitGetField(expression: IrGetField): IrExpression {
                            return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                                val stringBuilderImpl = createTmpVariable(irCall(constructor))

                                +irCall(appendFunction).apply {
                                    dispatchReceiver = irGet(stringBuilderImpl)
                                    putValueArgument(0, irString(prefix)) // appending PROPERTY_BACKING_FIELD
                                }

                                +irCall(appendFunction).apply {
                                    dispatchReceiver = irGet(stringBuilderImpl)
                                    putValueArgument(0, expression) // appending PROPERTY_BACKING_FIELD
                                }
                                // create an irCall and add it to this IrBuilder block
                                +irCall(toStringFunction).apply {
                                    dispatchReceiver /* <-- to String called on --> */ = irGet(stringBuilderImpl)
                                }
                            }
                        }
                    })
                }

                super.visitFunction(declaration)
            } else {
                super.visitFunction(declaration)
            }
        }

    }
}

fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile) {
            lower(declaration)
            declaration.acceptChildrenVoid(this)
        }
    })
}

