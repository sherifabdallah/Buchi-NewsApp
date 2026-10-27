# Headlines

A small news reader for the BÜCHI Android task. It shows the current US top headlines from
[NewsAPI](https://newsapi.org), lets you search them as you type, and opens a details screen for any
article. Whatever has been loaded is cached, so the app keeps working without a connection.

<p>
  <img src="docs/screenshots/headlines.png" width="230" alt="Headlines" />
  <img src="docs/screenshots/search.png" width="230" alt="Search" />
  <img src="docs/screenshots/details.png" width="230" alt="Article details" />
  <img src="docs/screenshots/offline.png" width="230" alt="Offline" />
</p>

## Running it

Open the project in Android Studio and run the `app` configuration, or from the command line:

```
./gradlew installDebug
```

It's on AGP 9.3 and Gradle 9.5, so it needs an Android Studio version that supports AGP 9.3 and JDK 17+.

The NewsAPI key isn't committed. Before building, add the key from the task email to
`~/.gradle/gradle.properties` (create the file if it doesn't exist):

```
newsApiKey=<key from the task email>
```

Passing `-PnewsApiKey=...` or setting a `NEWS_API_KEY` environment variable works too. Without a key
the app still builds, but every request comes back as "Access denied".

Tests:

```
./gradlew testDebugUnitTest                  # ViewModels, repository, mappers, Retrofit against MockWebServer
./gradlew createDebugUnitTestCoverageReport  # the same, plus a JaCoCo report in app/build/reports/coverage
./gradlew connectedDebugAndroidTest          # Room, Compose UI, accessibility and end-to-end tests (needs a device)
```

The end-to-end tests start the real app through Hilt; only the API and the database file are swapped
for fakes. Everything passes on an Android 8.0 emulator (the minimum supported version) and on a
Pixel 9 Pro emulator running Android 17.

GitHub Actions runs the unit tests and builds the debug APK on every push.

## How it's put together

One module, split by layer:

- `domain`: models, the repository and network monitor interfaces, use cases. Plain Kotlin.
- `data`: Retrofit, Room, the mappers and `OfflineFirstArticlesRepository`.
- `presentation`: Compose screens, their ViewModels, navigation and theme.
- `di`: Hilt modules.

Both screens follow MVI. The ViewModel exposes one immutable state as a `StateFlow`, the UI sends
intents through a single `onIntent()`, and one-off events (the "couldn't refresh" snackbar) go out as
effects through a `Channel`. Each screen has a stateful `...Screen` composable that talks to the
ViewModel and a stateless `...Content` one that only takes state and callbacks; the previews and UI
tests use the stateless one.

### Headlines and caching

Room is the source of truth. On launch the list shows whatever is cached straight away while pages 1
and 2 are requested in parallel. When both come back they're merged in page order, de-duplicated by
url and saved in one transaction. If either request fails the cache stays as it was, rather than
ending up half old and half new.

The feed order lives in its own `top_headlines` table, separate from `articles`, so search results can
be stored in `articles` too without showing up in the feed.

### Search

Typing triggers `top-headlines?q=` after a 500 ms pause. A newer query cancels the request that's still
running, and clearing the field brings the headlines back immediately. The query is kept in
`SavedStateHandle`, so it survives process death.

Results are saved to the database, so the details screen can always load an article by its url. If the
request fails, the repository falls back to a `LIKE` search over the saved articles and the list says
that's what it's showing.

### Details

Only the url goes through navigation; the screen reads the article back from Room. That keeps the
nav arguments small and works after process death. The ViewModel gets the url through Hilt assisted
injection.

### Errors

Exceptions are mapped to a small `DataError` type in the data layer (no connection, timeout, rejected
key, rate limited, server error, unknown) and the UI turns those into messages. What you see depends
on what's on screen:

- nothing cached: a full screen error with a retry button
- cached articles showing: they stay, and a snackbar offers a retry
- device offline: a banner, and the app refreshes by itself when the connection comes back

### NewsAPI quirks

Things in the real responses the app deals with:

- `author`, `description`, `urlToImage` and `content` are often null. Some authors are a Facebook URL.
- Taken-down articles come back as `"[Removed]"` placeholders; they're filtered out.
- Titles end with " - Publisher". That suffix is removed when it matches the source shown next to it.
- `content` is a ~200 character teaser ending in `[+2306 chars]`, and for paywalled sources it's a
  subscription pitch. The marker is stripped and the text is only used when there's no description;
  "Read full article" opens the original.
- The key is on the free developer plan (100 requests a day, shared by everyone using it), so `429`
  gets its own message. The search debounce helps keep the request count down.

## Trade-offs

- The use cases are thin, most of them forward to the repository. I kept them so the ViewModels only
  depend on the domain layer, but in an app this size they're borderline.
- The API key is compiled into `BuildConfig`. That's fine here, but anything in an APK can be
  extracted, so a real app would call its own backend instead.
- No Paging: the task asks for exactly two pages. With an open-ended feed I'd use Paging 3 with a
  `RemoteMediator` on top of the same Room tables.
- Images are cached by Coil, so the ones you've already seen also show up offline. Images you haven't
  opened yet won't.
- AGP 9.3 rather than 9.4: 9.4 needs Gradle 9.6 and an Android Studio release from September 2026, and
  I didn't want anyone to have to update Studio just to open the project.

## With more time

- Shared element transition for the image between the list and the details screen.
- Prefetch images for the cached articles so the offline experience is complete.
- Screenshot tests for the main states.
