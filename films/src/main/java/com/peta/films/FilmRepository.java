package com.peta.films;

import java.time.Instant;
import java.util.List;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface FilmRepository extends ElasticsearchRepository<Film, String> {
    // Custom query methods for querying the films index
    List<Film> findByEventType(String eventType);

    List<Film> findByPathContaining(String pathFragment);

    List<Film> findBySizeGreaterThan(long size);

    List<Film> findByTimestampAfter(Instant timestamp);

    // Full-text search over the `name` field using Elasticsearch `match` query
    @Query("{\"match\": {\"name\": {\"query\": \"?0\"}}}")
    List<Film> searchByNameFullText(String text);


    // ?0 represents the first parameter (String searchPath)
    @Query(
    """
    {
      "match": {
        "name": {
          "query": "?0",
          "fuzziness": "AUTO"
        }
      }
    }
    """)
    List<Film> searchByNameFuzzy(String searchName);

}