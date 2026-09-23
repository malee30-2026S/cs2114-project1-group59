# cs2114-project1-group59
App that recommends food places based off past experience/ratings

**Hungry Hokie** suggests restaurants in the Blacksburg/Christiansburg area based on a user's survey answers, past ratings, budget, and distance.

## What it does

- **Starting survey** (first launch only): name, email, age, location, dietary restriction, favorite cuisine, and a flavor profile (how much you like sweet, salty, umami, sour, and bitter). Bad input (a blank name, `#*@^`, an email like `jaidev`) shows a red message instead of crashing.
- **Live location**: choosing "Current location" asks permission first. If you allow it, distances use your real location from the Windows location service. If you don't, pick a town instead. Turn it off anytime on My Profile.
- **Home page**: your top 5 recommendations, plus every restaurant near you, closest first, with price and distance.
- **Search by dish**: "What are you craving?" finds places that serve a dish, like tacos (Mexican places) or pad thai (Thai places).
- **Eat here**: marks the visit as pending. The **next time you open the app**, the post-visit survey page comes up first: 1-5 stars, an optional review (checked by the Moderator), and whether you'd go back. Finishing it clears the visit. Good ratings raise that restaurant's cuisines in your recommendations, bad ones lower them.
- **Blacklist**: hides a restaurant everywhere. Take it off your blacklist on My Profile.
- **Search**: filter by name, dish, cuisine, max price, and max distance. Bad input (like `-5` or `abc` miles) shows a red message.
- **My Profile**: your photo (upload a .jpg or .png), name, age, email, location, flavor profile, favorite cuisines, favorite restaurants, blacklisted restaurants, visits waiting for a rating, what the app has learned from your ratings, and your rating history. Edit your profile or sign out from here.

The app remembers who's signed in, so you only see the starting survey once. Everything is saved to `hungryhokie.db` in the folder the app runs from.

Restaurant names, cuisines, and locations come from OpenStreetMap. Prices are estimates.

**Demo tip:** to show the post-visit survey, click "Eat here" on a restaurant, close the app, and run it again. To start over from the survey, use Sign out on My Profile.

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
