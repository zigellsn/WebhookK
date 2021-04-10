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
    implementation 'com.github.zigellsn:webhookk:{latest version}'
}
```

and use it like this:
```Kotlin
import com.github.zigellsn.webhookk.WebhookK
import com.github.zigellsn.webhookk.add
import io.ktor.http.Url
import io.ktor.http.content.TextContent

// ...

var webhooks = WebhookK(HttpClient(CIO) {
    engine {
        // CIO Configuration
    }
})

// ...

// Add urls to a topic
webhooks.topics.add("topic_name", Url("http://localhost:8080/receiver/"))
webhooks.topics.add("topic_name", Url("http://127.0.0.1:8080/receiver/"))

// ...

try {
    webhooks.trigger("topic_name") { url ->
        // Post message to receivers
        webhooks.post(
            url,
            TextContent("Message for receiver", ContentType.Text.Plain)
        ).execute()
    }
} catch (e: ConnectException) {
    // Handle exceptions
}    

webhooks.responses().collect { (topic, response) -> 
    // Handle responses
}
```