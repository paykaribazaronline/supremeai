# 📄 ফাইল: apps/mobile/lib/dataconnect_generated/.guides/usage.md

**প্রকার:** .md  
**সাইজ:** 1,180 বাইট  
**আপডেট:** 2026-07-03T15:03:57.602812

---

## কোড

```md
# Basic Usage

```dart
ExampleConnector.instance.ListMovies().execute();
ExampleConnector.instance.ListUsers().execute();
ExampleConnector.instance.ListUserReviews().execute();
ExampleConnector.instance.GetMovieById(getMovieByIdVariables).execute();
ExampleConnector.instance.SearchMovie(searchMovieVariables).execute();
ExampleConnector.instance.CreateMovie(createMovieVariables).execute();
ExampleConnector.instance.UpsertUser(upsertUserVariables).execute();
ExampleConnector.instance.AddReview(addReviewVariables).execute();
ExampleConnector.instance.DeleteReview(deleteReviewVariables).execute();

```

## Optional Fields

Some operations may have optional fields. In these cases, the Flutter SDK exposes a builder method, and will have to be set separately.

Optional fields can be discovered based on classes that have `Optional` object types.

This is an example of a mutation with an optional field:

```dart
await ExampleConnector.instance.SearchMovie({ ... })
.titleInput(...)
.execute();
```

Note: the above example is a mutation, but the same logic applies to query operations as well. Additionally, `createMovie` is an example, and may not be available to the user.

```