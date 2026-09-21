package com.peta.films;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class FileWebController {

    private final FilmService filmService;

    @Autowired
    public FileWebController(FilmService filmService) {
        this.filmService = filmService;
    }

    // This handles the root URL "localhost:9000/"
    @GetMapping("/")
    public String showSearchPage(
            @RequestParam(name = "q", required = false) String query,
            Model model) {

        // 1. Always pass the query back to the model so we can keep it inside the input box
        // (This makes the UI feel "sticky")
        model.addAttribute("lastQuery", query);

        // 2. If the user actually searched for something...
        if (query != null && !query.trim().isEmpty()) {
            // Perform the search (using your fuzzy search method)
            List<Film> results = filmService.searchByPathFuzzy(query);
            model.addAttribute("files", results);
        }

        // 3. Return the single HTML template
        return "index";
    }

    @GetMapping("/autocomplete")
    @ResponseBody
    public List<String> autocomplete(@RequestParam(name = "q", required = false) String query, Model model) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        List<Film> results = filmService.searchByPathFuzzy(query);
        model.addAttribute("files", results);
        return results.stream()
                .map(Film::getName)
                .collect(Collectors.toList());
    }
}
