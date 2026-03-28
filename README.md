# Timeline Game

A ClojureScript implementation of the Timeline card game using [helix](https://github.com/lilactown/helix) and [shadow-cljs](https://shadow-cljs.org/).

## Features

- Multiple card decks (History, Science, Inventions, Space, etc).
- Local multiplayer support.
- Interactive timeline where players place cards in chronological order.
- Real-time feedback on card placements.
- Scoreboard tracking player hands and deck size.

## Prerequisites

- [Node.js](https://nodejs.org/) (v16 or newer)
- [Clojure](https://clojure.org/guides/install_clojure) (CLI tools)
- [Java Development Kit (JDK)](https://adoptium.net/) (v11 or newer)

## Installation

1. Install dependencies:
   ```bash
   npm install
   ```

## Development

To start the development environment:

```bash
npx shadow-cljs watch frontend
```

- The app will be available at [http://localhost:8080](http://localhost:8080).

## Testing

To run the ClojureScript tests using Node.js and jsdom:

```bash
npx shadow-cljs compile node-test
```

To run the ClojureScript tests in the browser:

```bash
npx shadow-cljs watch test
```

- The tests will be available at [http://localhost:8021](http://localhost:8021).

## Releasing

To build the production version of the application:

```bash
npx shadow-cljs release frontend
```

The compiled assets will be in `target/public/js/main.js`.
The build process additionally copies `resources/public` into `target/public`,
and you can serve the contents of `target/public` using any web server.

## Development Process

This was one-shot generated a handful of times
using `gemini-cli` from the SPEC.md.
I'd adjust the spec, delete everything, and fire again.
I'd get minor variations,
sometimes losing or gaining entire design aspects
or requirements.
This seemed to waste a lot of time (and tokens)
without much benefit,
so I decided to keep one,
and go back to iteratively having the `gemini-cli` agent
enhance the application
in a series of smaller requests.
Building upon previous code
got me lots more functionality
more quickly.

## License

Copyright 2026 John Flinchbaugh
