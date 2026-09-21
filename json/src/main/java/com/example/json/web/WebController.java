package com.example.json.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

	@GetMapping("/chat")
	public String chat() {
		// forwards to /static/chat.html
		return "forward:/chat.html";
	}
}
