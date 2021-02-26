package org.javacs.kt.index

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.name.FqName
import org.javacs.kt.LOG
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert

private object Symbols : Table() {
    val fqName = varchar("fqname", length = 255).autoIncrement().primaryKey()
    val kind = integer("kind")
}

/**
 * A global view of all available symbols across all packages.
 */
class SymbolIndex {
    private val db = Database.connect("jdbc:h2:mem:symbolindex", "org.h2.Driver")

    init {
        transaction(db) {
            SchemaUtils.create(Symbols)
        }
    }

    fun update(module: ModuleDescriptor) {
        val started = System.currentTimeMillis()
        LOG.info("Updating symbol index...")

        try {
            transaction(db) {
                for (descriptor in allDescriptors(module)) {
                    Symbols.insert {
                        it[fqName] = descriptor.fqNameSafe.toString()
                        it[kind] = descriptor.accept(ExtractSymbolKind, Unit).rawValue
                    }
                }

                val finished = System.currentTimeMillis()
                val count = Symbols.slice(Symbols.fqName.count()).selectAll().first()[Symbols.fqName.count()]
                LOG.info("Updated symbol index in ${finished - started} ms! (${count} symbol(s))")
            }
        } catch (e: Exception) {
            LOG.error("Error while updating symbol index")
            LOG.printStackTrace(e)
        }
    }

    private fun allDescriptors(module: ModuleDescriptor): Collection<DeclarationDescriptor> = allPackages(module)
        .map(module::getPackage)
        .flatMap {
            try {
                it.memberScope.getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER)
            } catch (e: IllegalStateException) {
                LOG.warn("Could not query descriptors in package $it")
                emptyList()
            }
        }

    private fun allPackages(module: ModuleDescriptor, pkgName: FqName = FqName.ROOT): Collection<FqName> = module
        .getSubPackagesOf(pkgName) { it.toString() != "META-INF" }
        .flatMap { setOf(it) + allPackages(module, it) }
}
