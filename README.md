# cs2114-project1-group59
App that recommends food places based off past experience/ratings

**Hungry Hokie** suggests restaurants in the Blacksburg/Christiansburg area based on a user's survey answers, past ratings, budget, and distance.

## What it does

- **Starting survey**: name, email, age, location, dietary restriction, and favorite cuisine. Bad input (a blank name, `#*@^`, an email like `jaidev`) shows a red message instead of crashing.
- **Home page**: your top 5 recommendations, plus every restaurant near you, closest first, with price and distance.
- **Eat here**: logs the visit. The **next time you open the app**, a post-visit survey pops up asking for 1-5 stars, an optional review (checked by the Moderator), and whether you'd go back. Good ratings raise that restaurant's cuisines in your recommendations, bad ones lower them.
- **Hide** (blacklist): hides a restaurant everywhere. Unhide it from My Profile.
- **Search**: filter by name, cuisine, max price, and max distance. Bad input (like `-5` or `abc` miles) shows a red message.
- **My Profile**: everything saved about you: your survey answers, what the app has learned from your ratings, your rating history, places you'd go back to, hidden restaurants, and visits waiting for a rating. Edit your profile or sign out from here.

The app remembers who's signed in, so it opens straight to your home page next time. Everything is saved to `hungryhokie.db` in the folder the app runs from.

Restaurant names, cuisines, and locations come from OpenStreetMap. Prices are estimates.

**Demo tip:** to show the post-visit survey, click "Eat here" on a restaurant, close the app, and run it again.

## Compile and run

From this folder, with Java 11 or newer:

```
javac -d bin -cp "lib/*" src/*.java test/*.java
java -cp "bin;lib/*" Window
```

On Mac/Linux, use `bin:lib/*` (colon instead of semicolon).

## Run the tests

After compiling:

```
java -jar lib/junit-platform-console-standalone-1.14.4.jar execute -cp bin -cp lib/sqlite-jdbc-3.53.4.0.jar --scan-classpath bin
```

## Eclipse

**File → Import → General → Existing Projects into Workspace**, then pick this folder. Run `Window.java` as a Java Application, or right-click the `test` folder → **Run As → JUnit Test**.

## System diagram

![System diagram](docs/system-diagram.png)

## Layout

| Folder | What's in it |
|---|---|
| `src/` | The program's classes. `Window` has `main`. |
| `test/` | JUnit 5 tests: a normal case and a bad-input case for each key method |
| `lib/` | `sqlite-jdbc` (database driver) and the JUnit 5 runner |
| `docs/` | System diagram |
