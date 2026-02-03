package com.example.llmtools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple LLM Tool Call Example
 * 
 * This class demonstrates how to:
 * 1. Call an LLM with tool definitions
 * 2. Handle tool calls when the LLM returns them
 * 3. Execute the actual API calls for each tool
 * 
 * Note: You need to add these dependencies:
 * - Jackson (for JSON handling)
 * 
 * Using OpenCodeZen API with Kimi 2.5 model
 */
public class LLMToolCaller {
    
    private static final String OPENCODEZEN_API_KEY = System.getenv("OPENCODEZEN_API_KEY");
    private static final String API_URL = "https://opencode.ai/zen/v1/chat/completions";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public LLMToolCaller() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Main entry point - example usage
     */
    public static void main(String[] args) throws Exception {
        // Check for API key
        if (OPENCODEZEN_API_KEY == null || OPENCODEZEN_API_KEY.isEmpty()) {
            System.err.println("Please set OPENCODEZEN_API_KEY environment variable");
            System.exit(1);
        }
        
        LLMToolCaller caller = new LLMToolCaller();
        
        // Example: User asks about Star Wars
        System.out.println("Example: Ask about Star Wars (e.g., 'Tell me about Luke Skywalker')");
        System.out.print("Your question: ");
        String userInput = new Scanner(System.in).nextLine();
        
        // Call LLM with tools
        String response = caller.chatWithTools(userInput);
        System.out.println("\nFinal response: " + response);
    }
    
    /**
     * Main method to chat with LLM that has tool calling capabilities
     */
    public String chatWithTools(String userMessage) throws Exception {
        // Define available tools
        List<Map<String, Object>> tools = List.of(
            getStarWarsTool(),
            getCalculatorTool()
        );
        
        // Build the initial request
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "kimi-k2.5");
        
        ArrayNode messages = requestBody.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        
        ArrayNode toolsArray = requestBody.putArray("tools");
        for (Map<String, Object> tool : tools) {
            toolsArray.add(objectMapper.valueToTree(tool));
        }
        
        requestBody.put("tool_choice", "auto");
        
        // Call the LLM
        String llmResponse = callOpenCodeZen(requestBody);
        JsonNode responseJson = objectMapper.readTree(llmResponse);
        
        // Check if the LLM wants to call tools
        JsonNode choices = responseJson.get("choices");
        if (choices == null || choices.size() == 0) {
            return "No response from LLM";
        }
        
        JsonNode message = choices.get(0).get("message");
        
        // Check for tool calls
        JsonNode toolCalls = message.get("tool_calls");
        if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
            // Execute tools and get results
            ArrayNode toolResults = executeToolCalls(toolCalls);
            
            // Add the tool results to the conversation and get final response
            return getFinalResponse(requestBody, message, toolResults);
        } else {
            // No tools needed, just return the content
            return message.get("content").asText();
        }
    }
    
    /**
     * Execute the tool calls returned by the LLM
     */
    private ArrayNode executeToolCalls(JsonNode toolCalls) throws Exception {
        ArrayNode results = objectMapper.createArrayNode();
        
        for (JsonNode toolCall : toolCalls) {
            String toolName = toolCall.get("function").get("name").asText();
            String arguments = toolCall.get("function").get("arguments").asText();
            String toolCallId = toolCall.get("id").asText();
            
            // Parse the arguments
            JsonNode args = objectMapper.readTree(arguments);
            
            // Execute the corresponding method
            String result;
            try {
                switch (toolName) {
                    case "search_starwars_character":
                        result = searchStarWarsCharacter(
                            args.get("name").asText()
                        );
                        break;
                    case "calculate":
                        result = calculate(
                            args.get("operation").asText(),
                            args.get("a").asDouble(),
                            args.get("b").asDouble()
                        );
                        break;
                    default:
                        result = "Error: Unknown tool: " + toolName;
                }
            } catch (Exception e) {
                result = "Error executing tool: " + e.getMessage();
            }
            
            // Add result to array
            ObjectNode toolMessage = results.addObject();
            toolMessage.put("role", "tool");
            toolMessage.put("tool_call_id", toolCallId);
            toolMessage.put("content", result);
        }
        
        return results;
    }
    
    /**
     * Get final response after executing tools
     */
    private String getFinalResponse(
        ObjectNode originalRequest,
        JsonNode assistantMessage,
        ArrayNode toolResults
    ) throws Exception {
        
        // Build new request with all messages
        ObjectNode newRequest = originalRequest.deepCopy();
        ArrayNode messages = newRequest.putArray("messages");
        
        // Add original user message
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", originalRequest.get("messages").get(0).get("content").asText());
        
        // Add assistant's tool call message
        ObjectNode assistantMsg = messages.addObject();
        assistantMsg.put("role", "assistant");
        assistantMsg.set("content", assistantMessage.get("content"));
        assistantMsg.set("tool_calls", assistantMessage.get("tool_calls"));
        
        // Add tool results
        for (JsonNode toolResult : toolResults) {
            messages.add(toolResult);
        }
        
        // Remove tools from second call (we've already used them)
        newRequest.remove("tools");
        newRequest.remove("tool_choice");
        
        // Call LLM again with results
        String llmResponse = callOpenCodeZen(newRequest);
        JsonNode responseJson = objectMapper.readTree(llmResponse);
        
        return responseJson.get("choices").get(0).get("message").get("content").asText();
    }
    
    /**
     * ACTUAL TOOL IMPLEMENTATIONS
     * These are the methods that implement the API calls for each tool
     */
    
    private static final String SWAPI_URL = "https://swapi.dev/api/people/?search=";
    
    /**
     * Search for a Star Wars character using the SWAPI (Star Wars API)
     * This calls a real public API - no API key required!
     */
    private String searchStarWarsCharacter(String name) throws Exception {
        System.out.println("\n[Executing tool: search_starwars_character]");
        System.out.println("  Searching for: " + name);
        
        // Build the API URL
        String searchUrl = SWAPI_URL + name.replace(" ", "%20");
        
        // Make the HTTP request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .header("Accept", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            return "Error: API returned status " + response.statusCode();
        }
        
        // Parse the response
        JsonNode swapiResponse = objectMapper.readTree(response.body());
        JsonNode results = swapiResponse.get("results");
        
        if (results == null || results.size() == 0) {
            return "No character found with name: " + name;
        }
        
        // Get the first result
        JsonNode character = results.get(0);
        String charName = character.get("name").asText();
        String height = character.get("height").asText();
        String mass = character.get("mass").asText();
        String hairColor = character.get("hair_color").asText();
        String eyeColor = character.get("eye_color").asText();
        String birthYear = character.get("birth_year").asText();
        String gender = character.get("gender").asText();
        
        // Format the result
        return String.format(
            "Character: %s\n" +
            "Height: %s cm\n" +
            "Mass: %s kg\n" +
            "Hair Color: %s\n" +
            "Eye Color: %s\n" +
            "Birth Year: %s\n" +
            "Gender: %s",
            charName, height, mass, hairColor, eyeColor, birthYear, gender
        );
    }
    
    private String calculate(String operation, double a, double b) {
        System.out.println("\n[Executing tool: calculate]");
        System.out.println("  Operation: " + operation);
        System.out.println("  A: " + a + ", B: " + b);
        
        double result;
        switch (operation) {
            case "add":
                result = a + b;
                break;
            case "subtract":
                result = a - b;
                break;
            case "multiply":
                result = a * b;
                break;
            case "divide":
                if (b == 0) return "Error: Division by zero";
                result = a / b;
                break;
            default:
                return "Error: Unknown operation: " + operation;
        }
        
        return String.format("%.2f %s %.2f = %.2f", a, operation, b, result);
    }
    
    /**
     * TOOL DEFINITIONS
     * These define the schema for each tool that the LLM can call
     */
    
    private Map<String, Object> getStarWarsTool() {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", "search_starwars_character",
                "description", "Search for a Star Wars character using the SWAPI (Star Wars API). Returns character details like height, mass, hair color, eye color, birth year, and gender.",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "name", Map.of(
                            "type", "string",
                            "description", "The name of the Star Wars character to search for (e.g., 'Luke Skywalker', 'Darth Vader', 'Leia')"
                        )
                    ),
                    "required", List.of("name")
                )
            )
        );
    }
    
    private Map<String, Object> getCalculatorTool() {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", "calculate",
                "description", "Perform a mathematical calculation",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "operation", Map.of(
                            "type", "string",
                            "description", "The mathematical operation to perform",
                            "enum", List.of("add", "subtract", "multiply", "divide")
                        ),
                        "a", Map.of(
                            "type", "number",
                            "description", "The first number"
                        ),
                        "b", Map.of(
                            "type", "number",
                            "description", "The second number"
                        )
                    ),
                    "required", List.of("operation", "a", "b")
                )
            )
        );
    }
    
    /**
     * HTTP Client to call OpenCodeZen API
     */
    private String callOpenCodeZen(ObjectNode requestBody) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + OPENCODEZEN_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("API Error: " + response.statusCode() + " - " + response.body());
        }
        
        return response.body();
    }
}
