package org.github.keepasscompose.core.database

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KdbxInnerHeaderTest {

    // -- Reader tests --

    @Test
    fun readInnerHeader_streamIdAndKey() {
        val buf = Buffer()

        // Inner stream ID = 3 (ChaCha20)
        buf.writeByte(0x01) // field ID
        buf.writeIntLe(4)   // length
        buf.writeIntLe(3)   // ChaCha20

        // Inner stream key (32 bytes)
        val key = ByteArray(32) { (it + 1).toByte() }
        buf.writeByte(0x02) // field ID
        buf.writeIntLe(32)  // length
        buf.write(key)

        // End of inner header
        buf.writeByte(0x00)
        buf.writeIntLe(0)

        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(3, result.innerRandomStreamId)
        assertTrue(key.contentEquals(result.innerRandomStreamKey))
        assertTrue(result.binaries.isEmpty())
    }

    @Test
    fun readInnerHeader_withBinaries() {
        val buf = Buffer()

        // Inner stream ID
        buf.writeByte(0x01)
        buf.writeIntLe(4)
        buf.writeIntLe(3)

        // Inner stream key
        buf.writeByte(0x02)
        buf.writeIntLe(4)
        buf.write(ByteArray(4))

        // Binary 0: flags(1 byte) + data "hello"
        val binary0Data = "hello".encodeToByteArray()
        buf.writeByte(0x03)
        buf.writeIntLe(1 + binary0Data.size) // flags + data
        buf.writeByte(0x01) // flags: memory protection
        buf.write(binary0Data)

        // Binary 1: flags(1 byte) + data "world"
        val binary1Data = "world".encodeToByteArray()
        buf.writeByte(0x03)
        buf.writeIntLe(1 + binary1Data.size)
        buf.writeByte(0x00) // flags: none
        buf.write(binary1Data)

        // End of inner header
        buf.writeByte(0x00)
        buf.writeIntLe(0)

        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(2, result.binaries.size)
        assertTrue(binary0Data.contentEquals(result.binaries[0]!!))
        assertTrue(binary1Data.contentEquals(result.binaries[1]!!))
    }

    @Test
    fun readInnerHeader_skipsUnknownFields() {
        val buf = Buffer()

        // Unknown field ID 0x05
        buf.writeByte(0x05)
        buf.writeIntLe(3)
        buf.write("abc".encodeToByteArray())

        // Inner stream ID
        buf.writeByte(0x01)
        buf.writeIntLe(4)
        buf.writeIntLe(2) // Salsa20

        // Inner stream key
        buf.writeByte(0x02)
        buf.writeIntLe(4)
        buf.write(ByteArray(4))

        // End
        buf.writeByte(0x00)
        buf.writeIntLe(0)

        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(2, result.innerRandomStreamId)
    }

    @Test
    fun readInnerHeader_endWithData() {
        val buf = Buffer()

        // Inner stream ID
        buf.writeByte(0x01)
        buf.writeIntLe(4)
        buf.writeIntLe(3)

        // Inner stream key
        buf.writeByte(0x02)
        buf.writeIntLe(4)
        buf.write(ByteArray(4))

        // End of inner header with extra data (should be skipped)
        buf.writeByte(0x00)
        buf.writeIntLe(4)
        buf.write("junk".encodeToByteArray())

        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(3, result.innerRandomStreamId)
    }

    // -- Writer tests --

    @Test
    fun writeInnerHeader_streamIdAndKey() {
        val buf = Buffer()
        val key = ByteArray(32) { (it + 1).toByte() }

        KdbxInnerHeaderWriter().writeInnerHeader(
            sink = buf,
            innerRandomStreamId = 3,
            innerRandomStreamKey = key,
            binaries = KdbxBinaryPool(),
        )

        // Read it back
        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(3, result.innerRandomStreamId)
        assertTrue(key.contentEquals(result.innerRandomStreamKey))
        assertTrue(result.binaries.isEmpty())
    }

    @Test
    fun writeAndReadInnerHeader_withBinaries() {
        val binary0 = "attachment1".encodeToByteArray()
        val binary1 = "attachment2".encodeToByteArray()
        val key = ByteArray(32) { 0xAA.toByte() }

        val pool = KdbxBinaryPool()
        pool.put(0, binary0)
        pool.put(1, binary1)

        val buf = Buffer()
        KdbxInnerHeaderWriter().writeInnerHeader(
            sink = buf,
            innerRandomStreamId = 3,
            innerRandomStreamKey = key,
            binaries = pool,
        )

        // Read it back
        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        assertEquals(3, result.innerRandomStreamId)
        assertTrue(key.contentEquals(result.innerRandomStreamKey))
        assertEquals(2, result.binaries.size)
        assertTrue(binary0.contentEquals(result.binaries[0]!!))
        assertTrue(binary1.contentEquals(result.binaries[1]!!))
    }

    @Test
    fun writeAndRead_roundTrip_preservesBinaryOrder() {
        val pool = KdbxBinaryPool()
        // Add in non-sequential order
        pool.put(2, "c".encodeToByteArray())
        pool.put(0, "a".encodeToByteArray())
        pool.put(1, "b".encodeToByteArray())

        val buf = Buffer()
        KdbxInnerHeaderWriter().writeInnerHeader(
            sink = buf,
            innerRandomStreamId = 2,
            innerRandomStreamKey = ByteArray(16),
            binaries = pool,
        )

        val result = KdbxInnerHeaderReader().readInnerHeader(buf)

        // Writer sorts by ID, reader assigns sequential IDs 0, 1, 2
        assertEquals(3, result.binaries.size)
        assertEquals("a", result.binaries[0]!!.decodeToString())
        assertEquals("b", result.binaries[1]!!.decodeToString())
        assertEquals("c", result.binaries[2]!!.decodeToString())
    }

    // -- XmlReader external pool integration --

    @Test
    fun xmlReader_usesExternalPool() {
        val binaryData = "external binary data".encodeToByteArray()
        val pool = KdbxBinaryPool()
        pool.put(0, binaryData)

        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <KeePassFile>
                <Meta></Meta>
                <Root>
                    <Group>
                        <UUID>cm9vdA==</UUID>
                        <Name>Root</Name>
                        <Entry>
                            <UUID>ZW50cnk=</UUID>
                            <Binary>
                                <Key>document.pdf</Key>
                                <Value Ref="0" />
                            </Binary>
                        </Entry>
                    </Group>
                </Root>
            </KeePassFile>
        """.trimIndent()

        val result = KdbxXmlReader(
            isV4 = true,
            externalBinaryPool = pool,
        ).readXml(xml)

        val attachment = result.rootGroup.entries.first().attachments.first()
        assertEquals("document.pdf", attachment.name)
        assertEquals(0, attachment.id)
        assertTrue(binaryData.contentEquals(attachment.data))

        // Pool should be the same one we provided
        assertEquals(1, result.binaryPool.size)
        assertTrue(binaryData.contentEquals(result.binaryPool[0]!!))
    }

    @Test
    fun xmlReader_externalPoolIgnoresXmlBinaries() {
        val externalData = "from inner header".encodeToByteArray()
        val pool = KdbxBinaryPool()
        pool.put(0, externalData)

        // XML also has a Binaries section — but since external pool is provided,
        // the XML binaries should be ADDED to the same pool
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <KeePassFile>
                <Meta>
                    <Binaries>
                        <Binary ID="1">${kotlin.io.encoding.Base64.encode("from xml".encodeToByteArray())}</Binary>
                    </Binaries>
                </Meta>
                <Root>
                    <Group>
                        <UUID>cm9vdA==</UUID>
                        <Name>Root</Name>
                        <Entry>
                            <UUID>ZW50cnk=</UUID>
                            <Binary>
                                <Key>file.txt</Key>
                                <Value Ref="0" />
                            </Binary>
                        </Entry>
                    </Group>
                </Root>
            </KeePassFile>
        """.trimIndent()

        val result = KdbxXmlReader(
            isV4 = true,
            externalBinaryPool = pool,
        ).readXml(xml)

        // Entry references pool ID 0 which has the external data
        val attachment = result.rootGroup.entries.first().attachments.first()
        assertTrue(externalData.contentEquals(attachment.data))

        // Pool should have both entries (external + XML)
        assertEquals(2, result.binaryPool.size)
    }
}
