# LLM Tool Caller - Java

A simple Java application demonstrating how to use LLM (OpenCodeZen with Kimi 2.5) tool calling capabilities with a **real API**. This example shows how to:

1. **Define tool schemas** - Tell the LLM what tools are available
2. **Handle tool calls** - Parse and route LLM tool requests
3. **Execute tools** - Implement actual API calls in tool methods
4. **Return results** - Send tool results back to LLM for natural responses

## Features

- **Star Wars character search** (real SWAPI - no API key required!)
- **Calculator tool** (math operations)
- Proper JSON handling with Jackson
- HTTP client using Java's native HttpClient
- Full conversation flow with tool results
- Uses OpenCodeZen API with Kimi 2.5 model

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- OpenCodeZen API key

## Quick Start

### 1. Clone and Navigate

```bash
git clone <repo-url>
cd llm-tool-caller
```

### 2. Set API Key

```bash
export OPENCODEZEN_API_KEY="your-api-key-here"
```

Or create a `.env` file:
```
OPENCODEZEN_API_KEY=your-api-key-here
```

### 3. Build and Run

```bash
mvn clean package
java -cp target/llm-tool-caller-1.0-SNAPSHOT.jar com.example.llmtools.LLMToolCaller
```

Or use Maven to run:
```bash
mvn exec:java -Dexec.mainClass="com.example.llmtools.LLMToolCaller"
```

## Example Usage

### Star Wars Character Search (Real API)

```
Example: Ask about Star Wars (e.g., 'Tell me about Luke Skywalker')
Your question: Tell me about Darth Vader

[Executing tool: search_starwars_character]
  Searching for: Darth Vader

Final response: Darth Vader is one of the most iconic villains in cinematic history...
[Full character description with height, mass, birth year, etc.]
```

**Try these:**
- "Who is Luke Skywalker?"
- "Tell me about Princess Leia"
- "What can you tell me about Yoda?"
- "Give me info on Han Solo"

### Calculator Tool

```
Your question: Calculate 150 divided by 5

[Executing tool: calculate]
  Operation: divide
  A: 150.0, B: 5.0

Final response: 150.0 divided by 5.0 equals 30.0.
```

## How It Works

```
User Input → OpenCodeZen API (with tool definitions)
                      ↓
          Kimi 2.5 decides to call tool(s)
                      ↓
           Parse tool calls & execute
                      ↓
    Real API call to SWAPI (Star Wars API)
                      ↓
          Send results back to LLM
                      ↓
          Kimi 2.5 generates natural response
                      ↓
          Return to user
```

### Key Code Flow

1. **`chatWithTools(userMessage)`** - Main entry point
   - Sends user message + available tools to LLM
   - Returns tool calls or direct response

2. **`executeToolCalls(toolCalls)`** - Parse and execute
   - Extracts tool name and arguments from LLM response
   - Routes to corresponding method (`searchStarWarsCharacter`, `calculate`)
   - Returns results as JSON array

3. **`getFinalResponse(...)`** - Complete conversation
   - Adds original message + assistant response + tool results
   - Calls LLM again to generate natural response

4. **Tool Methods** - Your API implementations
   - `searchStarWarsCharacter(name)` - Calls real SWAPI at https://swapi.dev
   - `calculate(operation, a, b)` - Math operations

## The Star Wars API (SWAPI)

The **Star Wars API** (https://swapi.dev) is a completely free, open REST API that provides Star Wars data. No API key required!

**What it returns:**
- Character name, height, mass
- Hair color, eye color, skin color
- Birth year, gender
- Homeworld, films, species, vehicles, starships

**Example API call:**
```
GET https://swapi.dev/api/people/?search=luke
```

This demonstrates how to:
- Make HTTP GET requests to external APIs
- Parse JSON responses
- Handle errors gracefully
- Transform API data into natural language

## Adding Your Own Tools

To add a new tool:

1. **Define the tool schema** in `LLMToolCaller.java`:

```java
private Map<String, Object> getMyNewTool() {
    return Map.of(
        "type", "function",
        "function", Map.of(
            "name", "my_tool",
            "description", "Description for the LLM",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "param1", Map.of(
                        "type", "string",
                        "description", "What this parameter does"
                    )
                ),
                "required", List.of("param1")
            )
        )
    );
}
```

2. **Add to available tools**:

```java
List<Map<String, Object>> tools = List.of(
    getStarWarsTool(),
    getCalculatorTool(),
    getMyNewTool()  // Add here
);
```

3. **Implement the tool method**:

```java
private String myToolMethod(String param1) {
    // Your API call here
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/" + param1))
        .GET()
        .build();
    
    HttpResponse<String> response = httpClient.send(request, 
        HttpResponse.BodyHandlers.ofString());
    
    return response.body();
}
```

4. **Add to switch statement** in `executeToolCalls`:

```java
switch (toolName) {
    case "search_starwars_character": ...
    case "calculate": ...
    case "my_tool":  // Add here
        result = myToolMethod(args.get("param1").asText());
        break;
}
```

## Project Structure

```
llm-tool-caller/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── llmtools/
│                       └── LLMToolCaller.java
├── target/
│   └── llm-tool-caller-1.0-SNAPSHOT.jar
├── .gitignore
├── .env.example
├── pom.xml
└── README.md
```

## Dependencies

- **Jackson** (2.15.2) - JSON parsing
- **OpenCodeZen API** - Uses Chat Completions endpoint with Kimi 2.5
- **SWAPI** - Free Star Wars REST API

No external HTTP client needed - uses Java 11+ native `HttpClient`.

## Customization Ideas

- **Other free APIs** - Pokemon API, Rick and Morty API, JSONPlaceholder
- **Weather API** - OpenWeatherMap (requires API key)
- **Database queries** - SQL tool to query databases
- **File operations** - Read/write files based on LLM requests
- **API integrations** - Slack, GitHub, Jira, etc.
- **Web scraping** - Extract data from websites
- **Email sending** - Gmail/SendGrid integration

## API Costs

This makes 1-2 API calls to OpenCodeZen per interaction:
- 1 call: LLM decides if tools needed
- 1 additional call: If tools used, send results back

The Star Wars API (SWAPI) is completely free and requires no API key!

Monitor your OpenCodeZen usage dashboard for costs.

## Troubleshooting

**Error: "Please set OPENCODEZEN_API_KEY environment variable"**
- Ensure OPENCODEZEN_API_KEY is set in your shell

**Error: "API Error: 401"**
- Invalid API key - check your key is correct

**Error: "Unknown tool: xxx"**
- The LLM called a tool not in your switch statement
- Add handling in `executeToolCalls`

**Error: "No character found"**
- Try different spellings or partial names (e.g., "Luke" instead of "Luke Skywalker")

## License

MIT License - Feel free to use and modify!

## Contributing

This is a simple starter project. Feel free to:
- Add more example tools
- Improve error handling
- Add tests
- Create GUI version
- Add streaming support

## Resources

- [OpenCodeZen Documentation](https://opencodezen.ai/docs)
- [Star Wars API (SWAPI)](https://swapi.dev)
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [Java HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)

---

**Happy coding! 🚀**
