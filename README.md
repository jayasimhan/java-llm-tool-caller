# LLM Tool Caller - Java

A simple Java application demonstrating how to use LLM (OpenCodeZen with Kimi 2.5) tool calling capabilities. This example shows how to:

1. **Define tool schemas** - Tell the LLM what tools are available
2. **Handle tool calls** - Parse and route LLM tool requests
3. **Execute tools** - Implement actual API calls in tool methods
4. **Return results** - Send tool results back to LLM for natural responses

## Features

- Weather lookup tool (simulated API)
- Calculator tool (math operations)
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

```
Example: Ask about weather (e.g., 'What's the weather in London?')
Your question: What's the temperature in Paris?

[Executing tool: get_weather]
  Location: Paris
  Unit: celsius

Final response: The current temperature in Paris is 22.5°C with partly cloudy conditions.
```

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
   - Routes to corresponding method (`getWeather`, `calculate`)
   - Returns results as JSON array

3. **`getFinalResponse(...)`** - Complete conversation
   - Adds original message + assistant response + tool results
   - Calls LLM again to generate natural response

4. **Tool Methods** - Your API implementations
   - `getWeather(location, unit)` - Call weather API
   - `calculate(operation, a, b)` - Math operations

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
    getWeatherTool(),
    getCalculatorTool(),
    getMyNewTool()  // Add here
);
```

3. **Implement the tool method**:

```java
private String myToolMethod(String param1) {
    // Your API call here
    System.out.println("[Executing tool: my_tool]");
    return "Result from API call";
}
```

4. **Add to switch statement** in `executeToolCalls`:

```java
switch (toolName) {
    case "get_weather": ...
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

No external HTTP client needed - uses Java 11+ native `HttpClient`.

## Customization Ideas

- **Real weather API** - Integrate OpenWeatherMap or WeatherAPI
- **Database queries** - SQL tool to query databases
- **File operations** - Read/write files based on LLM requests
- **API integrations** - Slack, GitHub, Jira, etc.
- **Web scraping** - Extract data from websites
- **Email sending** - Gmail/SendGrid integration

## API Costs

This makes 1-2 API calls per interaction:
- 1 call: LLM decides if tools needed
- 1 additional call: If tools used, send results back

Monitor your OpenCodeZen usage dashboard for costs.

## Troubleshooting

**Error: "Please set OPENCODEZEN_API_KEY environment variable"**
- Ensure OPENCODEZEN_API_KEY is set in your shell

**Error: "API Error: 401"**
- Invalid API key - check your key is correct

**Error: "Unknown tool: xxx"**
- The LLM called a tool not in your switch statement
- Add handling in `executeToolCalls`

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
- [Jackson Documentation](https://github.com/FasterXML/jackson)
- [Java HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)

---

**Happy coding! 🚀**
