package brex.lsp_fixtures

import brex.proto.spend.accounting.record.v1.enums.AccountingPrepartionType
import brex.proto.spend.accounting.record.v1.services.AccountingRecordServiceGrpcKt.AccountingRecordServiceCoroutineStub
import brex.proto.spend.accounting.record.v1.services.listAccountingRecordsRequest
import brex.proto.spend.accounting.record.v1.enums.AccountingRecordTypeOuterClass.AccountingRecordType
import brex.lsp_fixtures.extensions.toBase64

class Foo {

    fun hello() {
        val foo = "foo"
        println(foo.toBase64())
    }
}
