package com.example.vinyls_application.network

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectorParserTest {

    private val singleCollectorJson = """
        [
          {
            "id": 1,
            "name": "Jaime Andrés Monsalve",
            "telephone": "3102178976",
            "email": "j.monsalve@gmail.com",
            "comments": [
              {
                "id": 1,
                "description": "Excelente álbum",
                "rating": 5
              }
            ],
            "favoritePerformers": [
              {
                "id": 1,
                "name": "Rubén Blades",
                "image": "https://example.com/ruben.jpg",
                "description": "Cantante y compositor panameño..."
              }
            ],
            "collectorAlbums": [
              {
                "id": 1,
                "price": 25,
                "status": "Active"
              }
            ]
          }
        ]
    """.trimIndent()

    private val multipleCollectorsJson = """
        [
          {
            "id": 1,
            "name": "Jaime Andrés Monsalve",
            "telephone": "3102178976",
            "email": "j.monsalve@gmail.com"
          },
          {
            "id": 2,
            "name": "Manolo Bellon",
            "telephone": "3503102456",
            "email": "bellon@manolo.com"
          }
        ]
    """.trimIndent()

    @Test
    fun test_parse_collector_json() {
        val collectors = NetworkServiceAdapter.parseCollectors(singleCollectorJson)

        assertEquals(1, collectors.size)
        val collector = collectors[0]
        assertEquals(1, collector.collectorId)
        assertEquals("Jaime Andrés Monsalve", collector.name)
        assertEquals("3102178976", collector.telephone)
        assertEquals("j.monsalve@gmail.com", collector.email)
    }

    @Test
    fun test_parse_multiple_collectors() {
        val collectors = NetworkServiceAdapter.parseCollectors(multipleCollectorsJson)

        assertNotNull(collectors)
        assertEquals(2, collectors.size)
        assertEquals(1, collectors[0].collectorId)
        assertEquals("Jaime Andrés Monsalve", collectors[0].name)
        assertEquals("3102178976", collectors[0].telephone)
        assertEquals("j.monsalve@gmail.com", collectors[0].email)

        assertEquals(2, collectors[1].collectorId)
        assertEquals("Manolo Bellon", collectors[1].name)
        assertEquals("3503102456", collectors[1].telephone)
        assertEquals("bellon@manolo.com", collectors[1].email)
    }

    @Test
    fun test_parse_empty_collectors_list() {
        val collectors = NetworkServiceAdapter.parseCollectors("[]")
        assertNotNull(collectors)
        assertTrue(collectors.isEmpty())
    }

    @Test(expected = JSONException::class)
    fun test_parse_invalid_json_throws_exception() {
        NetworkServiceAdapter.parseCollectors("not a json")
    }
}
