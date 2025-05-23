package org.javacs.kt.index

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.name.FqName
import org.javacs.kt.LOG
import org.javacs.kt.database.DatabaseService
import org.javacs.kt.progress.Progress
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import kotlin.sequences.Sequence

private const val MAX_FQNAME_LENGTH = 255
private const val MAX_SHORT_NAME_LENGTH = 80
private const val MAX_URI_LENGTH = 511

private object Symbols : IntIdTable() {
    val fqName = varchar("fqname", length = MAX_FQNAME_LENGTH).index()
    val shortName = varchar("shortname", length = MAX_SHORT_NAME_LENGTH)
    val kind = integer("kind")
    val visibility = integer("visibility")
    val extensionReceiverType = varchar("extensionreceivertype", length = MAX_FQNAME_LENGTH).nullable()
    val location = optReference("location", Locations)

    val byShortName = index("symbol_shortname_index", false, shortName)
}

private object Locations : IntIdTable() {
    val uri = varchar("uri", length = MAX_URI_LENGTH)
    val range = reference("range", Ranges)
}

private object Ranges : IntIdTable() {
    val start = reference("start", Positions)
    val end = reference("end", Positions)
}

private object Positions : IntIdTable() {
    val line = integer("line")
    val character = integer("character")
}

class SymbolEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<SymbolEntity>(Symbols)

    var fqName by Symbols.fqName
    var shortName by Symbols.shortName
    var kind by Symbols.kind
    var visibility by Symbols.visibility
    var extensionReceiverType by Symbols.extensionReceiverType
    var location by LocationEntity optionalReferencedOn Symbols.location
}

class LocationEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LocationEntity>(Locations)

    var uri by Locations.uri
    var range by RangeEntity referencedOn Locations.range
}

class RangeEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RangeEntity>(Ranges)

    var start by PositionEntity referencedOn Ranges.start
    var end by PositionEntity referencedOn Ranges.end
}

class PositionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PositionEntity>(Positions)

    var line by Positions.line
    var character by Positions.character
}

/**
 * A global view of all available symbols across all packages.
 */
class SymbolIndex(
    private val databaseService: DatabaseService
) {
    private val db: Database by lazy {
        databaseService.db ?: Database.connect("jdbc:h2:mem:symbolindex;DB_CLOSE_DELAY=-1", "org.h2.Driver")
    }

    var progressFactory: Progress.Factory = Progress.Factory.None

    init {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(Symbols, Locations, Ranges, Positions)
        }
    }

    /** Rebuilds the entire index. May take a while. */
    fun refresh(descriptors: Sequence<DeclarationDescriptor>) {
        val started = System.currentTimeMillis()
        LOG.info("Updating full bazel symbol index, this is required for most global completions/quick fixes to work...")

        progressFactory.create("Indexing bazel symbols").thenApply { progress ->
            try {
                transaction(db) {
                    // Keeping track of progress
                    var processedSymbols = 0
                    var batchCount = 0

                    // Remove everything first.
                    Symbols.deleteAll()
                    // Add new ones.
                    addDeclarationsInBatch(descriptors, onBatchProcessed = { batchSize ->
                        batchCount++
                        processedSymbols += batchSize
                        progress.update( "Indexed symbols - Batch #$batchCount ($processedSymbols total symbols)", null)
                    })

                    val finished = System.currentTimeMillis()
                    val count = Symbols.slice(Symbols.fqName.count()).selectAll().first()[Symbols.fqName.count()]
                    LOG.info("Updated full bazel symbol index in ${finished - started} ms! (${count} symbol(s))")
                }
            } catch (e: Exception) {
                LOG.error("Error while updating symbol index")
                LOG.printStackTrace(e)
            }

            progress.close()
        }
    }

    private fun addDeclarationsInBatch(declarations: Sequence<DeclarationDescriptor>, batchSize: Int = 1000, onBatchProcessed: (Int) -> Unit) {
        declarations
            .map { declaration ->
                val (descriptorFqn, extensionReceiverFqn) = getFqNames(declaration)
                Triple(descriptorFqn, extensionReceiverFqn, declaration)
            }
            .filter { (descriptorFqn, extensionReceiverFqn, _) ->
                validFqName(descriptorFqn) && (extensionReceiverFqn?.let { validFqName(it) } != false)
            }
            .chunked(batchSize)
            .forEach { batch ->
                Symbols.batchInsert(batch) { (descriptorFqn, extensionReceiverFqn, declaration) ->
                    this[Symbols.fqName] = descriptorFqn.toString()
                    this[Symbols.shortName] = descriptorFqn.shortName().toString()
                    this[Symbols.kind] = declaration.accept(ExtractSymbolKind, Unit).rawValue
                    this[Symbols.visibility] = declaration.accept(ExtractSymbolVisibility, Unit).rawValue
                    this[Symbols.extensionReceiverType] = extensionReceiverFqn?.toString()
                }
                onBatchProcessed(batch.size)
            }
    }

    // Removes a list of indexes and adds another list. Everything is done in the same transaction.
    fun updateIndexes(remove: Sequence<DeclarationDescriptor>, add: Sequence<DeclarationDescriptor>) {
        val started = System.currentTimeMillis()
        LOG.debug("Updating symbol index...")

        try {
            transaction(db) {
                removeDeclarations(remove)
                addDeclarationsInBatch(add, onBatchProcessed = { batchSize -> })

                val finished = System.currentTimeMillis()
                val count = Symbols.slice(Symbols.fqName.count()).selectAll().first()[Symbols.fqName.count()]
                LOG.debug("Updated symbol index in ${finished - started} ms! (${count} symbol(s))")
            }
        } catch (e: Exception) {
            LOG.error("Error while updating symbol index")
            LOG.printStackTrace(e)
        }
    }

    private fun removeDeclarations(declarations: Sequence<DeclarationDescriptor>) =
        declarations.forEach { declaration ->
            val (descriptorFqn, extensionReceiverFqn) = getFqNames(declaration)

            if (validFqName(descriptorFqn) && (extensionReceiverFqn?.let { validFqName(it) } != false)) {
                Symbols.deleteWhere {
                    (Symbols.fqName eq descriptorFqn.toString()) and (Symbols.extensionReceiverType eq extensionReceiverFqn?.toString())
                }
            } else {
                LOG.warn("Excluding symbol {} from index since its name is too long", descriptorFqn.toString())
            }
        }

    private fun getFqNames(declaration: DeclarationDescriptor): Pair<FqName, FqName?> {
        val descriptorFqn = declaration.fqNameSafe
        val extensionReceiverFqn = declaration.accept(ExtractSymbolExtensionReceiverType, Unit)?.takeIf { !it.isRoot }

        return Pair(descriptorFqn, extensionReceiverFqn)
    }

    private fun validFqName(fqName: FqName) =
        fqName.toString().length <= MAX_FQNAME_LENGTH
            && fqName.shortName().toString().length <= MAX_SHORT_NAME_LENGTH

    fun query(prefix: String, receiverType: FqName? = null, limit: Int = 20, suffix: String = "%"): List<Symbol> = transaction(db) {
        // TODO: Extension completion currently only works if the receiver matches exactly,
        //       ideally this should work with subtypes as well
        SymbolEntity.find {
            (Symbols.shortName like "$prefix$suffix") and (Symbols.extensionReceiverType eq receiverType?.toString())
        }.limit(limit)
            .map { Symbol(
                fqName = FqName(it.fqName),
                kind = Symbol.Kind.fromRaw(it.kind),
                visibility = Symbol.Visibility.fromRaw(it.visibility),
                extensionReceiverType = it.extensionReceiverType?.let(::FqName)
            ) }
    }
}
