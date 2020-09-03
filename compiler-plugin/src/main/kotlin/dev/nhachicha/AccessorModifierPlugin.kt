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
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.isPropertyAccessor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.isNullableString
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.visitors.*

@AutoService(ComponentRegistrar::class)
class AccessorModifierComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        registerExtensions(project, configuration)
    }

    companion object {
        fun registerExtensions(project: MockProject, configuration: CompilerConfiguration) {
            IrGenerationExtension.registerExtension(
                    project,
                    AccessorModifierIrGenerationExtension()
            )
        }
    }
}

class AccessorModifierIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            AccessorCallTransformer(pluginContext).runOnFileInOrder(file)
        }
    }

    class AccessorCallTransformer(
            val context: IrPluginContext
    ) : IrElementTransformerVoidWithContext(), FileLoweringPass {

        override fun lower(irFile: IrFile) {
            irFile.transformChildrenVoid()
        }

        override fun visitFunctionNew(declaration: IrFunction): IrStatement {
            return if (declaration.isPropertyAccessor
                    && declaration.isGetter
                    && (declaration.returnType.isString() || declaration.returnType.isNullableString())
            ) {
                declaration.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return IrBlockBuilder(context, currentScope?.scope!!, expression.startOffset, expression.endOffset).irBlock {
                            val irConcat = irConcat()
                            irConcat.addArgument(irString("Hello "))
                            irConcat.addArgument(expression.value)
                            +irReturn(irConcat)
                        }
                    }
                })
                super.visitFunctionNew(declaration)
            } else {
                super.visitFunctionNew(declaration)
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
