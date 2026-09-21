package com.peta.films;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FilmService {

    @Autowired
    private FilmRepository repository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public List<Film> searchFilms(String query) {
        // Search where 'filepath' contains the query text
    	String[] keywords = query.split(" ");
        Criteria criteria = new Criteria("name");
        for (int i = 0; i < keywords.length; i++) {
        	criteria.contains(keywords[i]);
		}
        
        // CriteriaQuery automatically sorts by Score (Relevance) descending by default
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

        SearchHits<Film> searchHits = elasticsearchOperations.search(criteriaQuery, Film.class);

        // Extract the actual Film objects from the hits
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    // Service wrapper for full-text searching the `path` field
    public List<Film> searchByPathFuzzy(String query) {
        try {
            List<Film> films = searchFilms(query);
            List<Film> resultList = searchBySubstrings(query);
            for (Film f : films) {
                if (!resultList.contains(f)) {
                    resultList.add(f);
                }
            }
            return resultList;
                    
        } catch (Exception e) {
        	e.printStackTrace();
            System.err.println("Full-text search failed: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Film> searchBySubstrings(String query) {
        Set<Film> fullFilmNameResults = searchByFullFilmName(query);
        if (fullFilmNameResults.size() == 1) {// FULL NAME MATCH
            return new ArrayList<>(fullFilmNameResults);
        }

        String[] serchedWords = query.toLowerCase().split(" ");

        Iterable<Film> films = repository.findAll();
        List<FilmWithScore> resultList = new ArrayList<>();
        for (Film f : films) {
            int count = 0;
            FilmWithScore fws = new FilmWithScore(f);
            String lowerPath = f.getName().toLowerCase();
            for (String word : serchedWords) {
                if (lowerPath.contains(word)) {
                    count++;
                }
            }
            fws.setScore(count);
            if (count > 0) {
            	resultList.add(fws);
            }
        }

        // sort according a score
        Collections.sort(resultList);

        List<Film> results = new ArrayList<>();
        for (FilmWithScore fws : resultList) {
            results.add(fws.getFilm());
        }

        return results;
    }


    public Set<Film> searchByFullFilmName(String name) {

        Iterable<Film> films = repository.findAll();
        Set<Film> results = new HashSet<>();
        for (Film f : films) {
            String lowerCaseName = f.getName().toLowerCase();
            if (lowerCaseName.equals(name.toLowerCase())) {
                results.add(f);
            }
        }

        return results;
    }


}


class FilmWithScore implements Comparable<FilmWithScore> {
	public FilmWithScore(Film f) {
		super();
		this.film = f;
	}

    private Film film;
	private int score = 0;

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	
	
	public Film getFilm() {
		return film;
	}

	public void setFilm(Film film) {
		this.film = film;
	}

	@Override
	public int compareTo(FilmWithScore secondFilm) {
		if (this.score > secondFilm.score) {
			return -1;
		} else if (this.score < secondFilm.score) {
			return 1;
		}
		return 0;
	}
	
	
	
}