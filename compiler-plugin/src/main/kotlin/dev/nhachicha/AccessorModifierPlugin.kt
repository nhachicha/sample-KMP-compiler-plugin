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
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.FileWriter
import java.time.Instant

@AutoService(ComponentRegistrar::class)
class AccessorModifierComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        registerExtensions(project)
    }

    companion object {
        fun registerExtensions(project: MockProject) {
            IrGenerationExtension.registerExtension(
                    project,
                    AccessorModifierIrGenerationExtension()
            )
        }
    }
}

private class AccessorModifierIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val realmObjectClassLowering = RealmObjectClassLowering(pluginContext)
        for (file in moduleFragment.files) {
            realmObjectClassLowering.runOnFileInOrder(file)
        }
    }
}

private class RealmObjectClassLowering(val context: IrPluginContext) :
        ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        generate(irClass, context)
    }

    companion object {
        fun generate(
                irClass: IrClass,
                context: IrPluginContext
        ) {
            if (irClass.isRealmObject) {
                irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        logger("modifying expression: ${expression.dump()}")
                        return IrBlockBuilder(context, currentScope?.scope!!, expression.startOffset, expression.endOffset).irBlock {
                            val irConcat = irConcat()
                            irConcat.addArgument(irString("Hello "))
                            irConcat.addArgument(expression.value)
                            +irReturn(irConcat)
                        }
                    }
                })
            }
        }
    }
}

fun ClassLoweringPass.runOnFileInOrder(irFile: IrFile) {
    irFile.acceptVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            lower(declaration)
            declaration.acceptChildrenVoid(this)
        }
    })
}


// Annotation
private val IrClass.isRealmObject: Boolean get() = kind == ClassKind.CLASS && hasRealmObjectAnnotationWithoutArgs()

private val realmObjectAnnotationFqName = FqName("io.realm.kmmapplication.shared.RealmObject")

private fun IrClass.hasRealmObjectAnnotationWithoutArgs(): Boolean {
    val annotation = getAnnotation(realmObjectAnnotationFqName) ?: return false
    for (i in 0 until annotation.valueArgumentsCount) {
        if (annotation.getValueArgument(i) != null) return false // TODO consider raising a compiler error with proper line number and message
    }
    return true
}

private fun IrAnnotationContainer.getAnnotation(name: FqName): IrConstructorCall? =
        annotations.find {
            it.symbol.owner.parentAsClass.descriptor.fqNameSafe == name
        }


// Logging to a temp file and to console/IDE (Build Output)
lateinit var messageCollector: MessageCollector
fun logger(message: String, severity: CompilerMessageSeverity = CompilerMessageSeverity.WARNING) {
    val formattedMessage =  "[Kotlin Compiler] ${Instant.now()} $message\n"
    messageCollector.report(severity, formattedMessage)
    FileWriter("/tmp/kmp.log").use {
        it.append(formattedMessage)
    }
}
