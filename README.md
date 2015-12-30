### KREST
A simple REST client for Kotlin

Example:

```
data class PersonInfo(val name: String, val address: String)

val result = URI("https://someservice.com").get().response<PersonInfo>().body
println(result.name) // result is a PersonInfo object
```

The idea is to consider how one thinks about connecting to a RESTful service, and then model the API after that.

This is how I think about it:

1. Let's start with the URI that we're connecting to:

```
val uri = URI("https://someservice.com")
```

2. Next, the request method (a GET in our example):

```
val request = uri.get()
```

3. You can modify the request at this point:

```
request.header("X-Client", "krest")
```

or modify it as part of an initialization function when you create the request:

```
val request = uri.get {
    header("X-Client", "krest")
}
```

4. Now, fetch the response. When you do so, you'll let us know what type of data you're expecting in the response.:

```
val response = request.response<Person>()
```

5. But, of course, REST responses included information in headers, etc, so this response isn't just that type. You
get the actual entity object by calling body:

```
val body = response.body
```

or, fetch other info like headers:

```
val server = response.header("X-Server")
```

For convenience, all of this can be put together in a single thought:

```
val result = URI("https://someservice.com").get {
  header("X-Client", "krest")
}.response<Person>().body
```
