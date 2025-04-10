package org.javacs.kt.index

import org.javacs.kt.LOG
import org.javacs.kt.classpath.PackageSourceMapping
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName

/**
 * This retrieves all the symbols for the packages which we tracked with Bazel for every classpath entries
 * and extract their declaration descriptors from the compiled file
 */
class BazelSymbolView (private val module: ModuleDescriptor, private val packages: Set<PackageSourceMapping>) {

    fun getAllDeclarations(): Set<DeclarationDescriptor> {
        return packages.asSequence()
            .map { it.sourcePackage }
            .map { FqName(it) }
            .map { module.getPackage(it) }
            .filter { !it.isEmpty() }
            .flatMap { it.memberScope.getContributedDescriptors() }
            .flatMap { descriptor ->
                if (descriptor is ClassDescriptor) {
                    sequenceOf(descriptor) + getAllClassMembersSequence(descriptor)
                } else {
                    sequenceOf(descriptor)
                }
            }
            .toSet()
    }

    private fun getAllClassMembersSequence(classDescriptor: ClassDescriptor): Sequence<DeclarationDescriptor> {
        val memberDescriptors = classDescriptor.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
        val constructors = classDescriptor.constructors.asSequence()

        val nestedClasses = classDescriptor.unsubstitutedInnerClassesScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()

        val nestedClassMembers = nestedClasses.asSequence()
            .flatMap { nestedClass ->
                sequenceOf(nestedClass) + getAllClassMembersSequence(nestedClass)
            }

        return memberDescriptors + constructors + nestedClassMembers
    }
}
