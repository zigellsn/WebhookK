# WebhookK

![Java CI](https://github.com/zigellsn/WebhookK/workflows/Java%20CI/badge.svg) 
[![Release](https://jitpack.io/v/zigellsn/WebhookK.svg)](https://jitpack.io/#zigellsn/WebhookK)

A Kotlin webhook provider

## Example

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:
```gradle
dependencies {
    implementation "com.github.zigellsn:webhookk:$latestWebhookKVersion"
}
```

and use it like this:
```Kotlin
import com.github.zigellsn.webhookk.WebhookK
import com.github.zigellsn.webhookk.add
import io.ktor.http.Url
import io.ktor.content.TextContent

// ...

val webhooks = WebhookK(HttpClient(CIO) {
    engine {
        // CIO Configuration
    }
})

// ...

// Add urls to a topic
webhooks.topics.add("topic_name", Url("http://localhost:8080/receiver/"))
webhooks.topics.add("topic_name", Url("https://127.0.0.1:8080/receiver/"))

// ...

try {
    webhooks.trigger("topic_name") { url ->
        // Post message to receivers
        post(
            url,
            TextContent("Message for receiver", ContentType.Text.Plain)
        )
    }
} catch (e: ConnectException) {
    // Handle exceptions
}    

webhooks.responses().collect { (topic, response) -> 
    // Handle responses
}
```

To save the urls into a JSON file, instantiate the WebhookK class like this:

```Kotlin
// ...
import java.io.File 
import java.net.URI
// ...

// ...
val webhooks = WebhookK(HttpClient(CIO) {
    engine {
        // CIO Configuration
    }
}, FileDataAccess(Path.of(URI("file:///C:/my_webhook_file.json"))))
// ...
```

To implement an alternative means of data persistence, create a class that implements the interface `DataAccess`.  
