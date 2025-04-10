package org.javacs.kt.index

import org.javacs.kt.classpath.PackageSourceMapping
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

/**
 * This retrieves all the symbols for the packages which we tracked with Bazel for every classpath entries
 * and extract their declaration descriptors from the compiled file
 */
class BazelSymbolView (private val module: ModuleDescriptor, private val packages: Set<PackageSourceMapping>) {

    /**
     * Gets only top-level classes and functions from all packages
     * Used for initial lightweight indexing
     */
    fun getAllTopLevelDeclarations(): Sequence<DeclarationDescriptor> {
        val kindFilter = DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.FUNCTIONS_MASK)
        return packages.asSequence()
            .map { it.sourcePackage }
            .flatMap { packageName ->
                val packageFqName = FqName(packageName)
                val packageView = module.getPackage(packageFqName)

                if (packageView.isEmpty()) {
                    emptySequence()
                } else {
                    // Only get classes and functions, not properties or other types
                    packageView.memberScope.getContributedDescriptors(
                        kindFilter = kindFilter
                    ).asSequence()
                }
            }
    }

    /**
     * Gets detailed declarations for any declaration type
     * Used for deep indexing of symbols in open files
     */
    fun getDetailedDeclarations(descriptor: DeclarationDescriptor): Sequence<DeclarationDescriptor> {
        return when (descriptor) {
            is ClassDescriptor -> getDetailedClassDeclarations(descriptor)
            is FunctionDescriptor -> getDetailedFunctionDeclarations(descriptor)
            is PropertyDescriptor -> getDetailedPropertyDeclarations(descriptor)
            else -> sequenceOf(descriptor) // Default for other descriptor types
        }
    }

    private fun getDetailedClassDeclarations(classDescriptor: ClassDescriptor): Sequence<DeclarationDescriptor> {
        // Implementation similar to your previous method
        val members = classDescriptor.unsubstitutedMemberScope.getContributedDescriptors(
            kindFilter = DescriptorKindFilter.ALL
        ).asSequence()

        val constructors = classDescriptor.constructors.asSequence()

        // Process nested classes
        val nestedClasses = classDescriptor.unsubstitutedInnerClassesScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()

        val nestedClassesWithMembers = nestedClasses.asSequence().flatMap { nestedClass ->
            sequenceOf(nestedClass) + getDetailedClassDeclarations(nestedClass)
        }

        return members + constructors + nestedClassesWithMembers
    }

    private fun getDetailedFunctionDeclarations(functionDescriptor: FunctionDescriptor): Sequence<DeclarationDescriptor> {
        val typeParameters = functionDescriptor.typeParameters.asSequence()
        val parameters = functionDescriptor.valueParameters.asSequence()

        return sequenceOf(functionDescriptor) + typeParameters + parameters
    }

    private fun getDetailedPropertyDeclarations(propertyDescriptor: PropertyDescriptor): Sequence<DeclarationDescriptor> {
        val accessors = sequenceOf(
            propertyDescriptor.getter,
            propertyDescriptor.setter
        ).filterNotNull()

        return sequenceOf(propertyDescriptor) + accessors
    }
}
