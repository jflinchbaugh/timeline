# Timeline Game

A ClojureScript implementation of the Timeline card game using [helix](https://github.com/lilactown/helix) and [shadow-cljs](https://shadow-cljs.org/).

## Features

- Multiple card decks (History, Science, Inventions, Space).
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
npx shadow-cljs watch app
```

- The app will be available at [http://localhost:8080](http://localhost:8080).

## Testing

To run the ClojureScript tests using Node.js and jsdom:

```bash
npx shadow-cljs compile test
```

## Releasing

To build the production version of the application:

```bash
npx shadow-cljs release app
```

The compiled assets will be in `target/public/js/main.js`. You can serve the contents of `resources/public` and `target/public` using any web server.

## License

Copyright © 2026 John Flinchbaugh
